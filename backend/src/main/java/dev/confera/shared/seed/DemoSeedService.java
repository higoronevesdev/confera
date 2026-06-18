package dev.confera.shared.seed;

import dev.confera.identity.entity.Role;
import dev.confera.identity.entity.Tenant;
import dev.confera.identity.entity.User;
import dev.confera.identity.repository.RefreshTokenRepository;
import dev.confera.identity.repository.TenantRepository;
import dev.confera.identity.repository.UserRepository;
import dev.confera.ingestion.entity.*;
import dev.confera.ingestion.repository.BankStatementRepository;
import dev.confera.ingestion.repository.FileImportRepository;
import dev.confera.ingestion.repository.ReceivableRepository;
import dev.confera.ledger.entity.*;
import dev.confera.ledger.repository.AccountRepository;
import dev.confera.ledger.repository.EntryRepository;
import dev.confera.ledger.repository.TransactionRepository;
import dev.confera.reconciliation.entity.*;
import dev.confera.reconciliation.repository.ReconciliationDiscrepancyRepository;
import dev.confera.reconciliation.repository.ReconciliationMatchRepository;
import dev.confera.shared.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Profile("demo")
@RequiredArgsConstructor
@Slf4j
public class DemoSeedService {

    public static final String DEMO_SLUG = "confera-dev-demo";
    private static final long RNG_SEED = 42L;
    private static final ZoneOffset BRT = ZoneOffset.of("-03:00");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMuuuu", new Locale("pt", "BR"));

    private final TenantRepository tenantRepo;
    private final UserRepository userRepo;
    private final RefreshTokenRepository refreshTokenRepo;
    private final AccountRepository accountRepo;
    private final TransactionRepository transactionRepo;
    private final EntryRepository entryRepo;
    private final FileImportRepository fileImportRepo;
    private final ReceivableRepository receivableRepo;
    private final BankStatementRepository bankStatementRepo;
    private final ReconciliationMatchRepository matchRepo;
    private final ReconciliationDiscrepancyRepository discrepancyRepo;
    private final OutboxEventRepository outboxEventRepo;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public boolean isDemoDataPresent() {
        return tenantRepo.findBySlug(DEMO_SLUG).isPresent();
    }

    public void seed(boolean forceReset) {
        Optional<Tenant> existing = tenantRepo.findBySlug(DEMO_SLUG);
        if (existing.isPresent()) {
            if (!forceReset) {
                log.info("[DemoSeeder] Demo data já presente (tenant: {}). Pulando.", DEMO_SLUG);
                return;
            }
            log.info("[DemoSeeder] Force-reset solicitado — removendo dados antigos...");
            teardown(existing.get().getId());
        }

        log.info("[DemoSeeder] Populando dados demo para '{}'...", DEMO_SLUG);
        long t0 = System.currentTimeMillis();

        Random rng = new Random(RNG_SEED);

        Tenant tenant = createTenant();
        User user = createUser(tenant);
        Map<String, Account> accounts = createAccounts(tenant.getId());

        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(89);

        Map<String, UUID> acquirerImports = createAcquirerFileImports(tenant.getId(), user.getId(), startDate, endDate);
        Map<String, UUID> bankImports = createBankFileImports(tenant.getId(), user.getId(), startDate, endDate);

        generateBulkData(rng, tenant.getId(), accounts, acquirerImports, bankImports, startDate, endDate);
        createCuratedCases(tenant.getId(), user.getId(), accounts, acquirerImports, bankImports, endDate);

        log.info("[DemoSeeder] Concluído em {}ms. Login: demo@confera.dev / demo1234",
                System.currentTimeMillis() - t0);
    }

    @Transactional
    public void teardown(UUID tenantId) {
        log.info("[DemoSeeder] Removendo dados do tenant {}...", tenantId);
        // outbox_events: no tenant_id column — delete by aggregate_id (transaction ids)
        jdbc.update(
            "DELETE FROM outbox_events WHERE aggregate_id IN " +
            "(SELECT id::text FROM transactions WHERE tenant_id = ?::uuid)", tenantId.toString());
        jdbc.update("DELETE FROM reconciliation_matches WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM reconciliation_discrepancies WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM entries WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM transactions WHERE tenant_id = ?", tenantId);
        // accounts has self-referential parent_id; null children first
        jdbc.update("UPDATE accounts SET parent_id = NULL WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM accounts WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bank_statements WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM receivables WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM file_imports WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE tenant_id = ?)", tenantId);
        jdbc.update("DELETE FROM users WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM tenants WHERE id = ?", tenantId);
        log.info("[DemoSeeder] Dados removidos com sucesso.");
    }

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    private Tenant createTenant() {
        Tenant t = Tenant.builder()
                .name("Cafeteria Mirante")
                .slug(DEMO_SLUG)
                .active(true)
                .build();
        return tenantRepo.save(t);
    }

    private User createUser(Tenant tenant) {
        User u = User.builder()
                .tenant(tenant)
                .email("demo@confera.dev")
                .passwordHash(passwordEncoder.encode("demo1234"))
                .fullName("Visitante Demo")
                .role(Role.OWNER)
                .active(true)
                .build();
        return userRepo.save(u);
    }

    // -------------------------------------------------------------------------
    // Ledger — Chart of accounts
    // -------------------------------------------------------------------------

    private Map<String, Account> createAccounts(UUID tenantId) {
        Account cash = accountRepo.save(Account.builder()
                .tenantId(tenantId).name("Caixa / Banco").code("1.1.01")
                .type(AccountType.ASSET).normalBalance(Direction.DEBIT).build());
        Account ar = accountRepo.save(Account.builder()
                .tenantId(tenantId).name("Contas a Receber").code("1.1.02")
                .type(AccountType.ASSET).normalBalance(Direction.DEBIT).build());
        Account revenue = accountRepo.save(Account.builder()
                .tenantId(tenantId).name("Receita de Vendas").code("4.1.01")
                .type(AccountType.REVENUE).normalBalance(Direction.CREDIT).build());
        Account fees = accountRepo.save(Account.builder()
                .tenantId(tenantId).name("Taxas de Adquirente").code("5.1.01")
                .type(AccountType.EXPENSE).normalBalance(Direction.DEBIT).build());

        return Map.of("cash", cash, "ar", ar, "revenue", revenue, "fees", fees);
    }

    // -------------------------------------------------------------------------
    // File imports
    // -------------------------------------------------------------------------

    private Map<String, UUID> createAcquirerFileImports(UUID tenantId, UUID userId,
                                                         LocalDate start, LocalDate end) {
        Map<String, UUID> result = new LinkedHashMap<>();
        String[] acquirers = {"STONE", "CIELO", "REDE", "MERCADOPAGO"};

        Set<YearMonth> months = new LinkedHashSet<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusMonths(1)) {
            months.add(YearMonth.from(d));
        }
        months.add(YearMonth.from(end));

        for (String acq : acquirers) {
            for (YearMonth ym : months) {
                String label = ym.format(DateTimeFormatter.ofPattern("MMMuuuu", new Locale("pt", "BR")));
                String fileName = "extrato-" + acq.toLowerCase() + "-" + label.toLowerCase() + ".csv";
                String storageKey = "demo/acquirer/" + acq.toLowerCase() + "/" + ym + ".csv";
                FileImport fi = fileImportRepo.save(FileImport.builder()
                        .tenantId(tenantId).userId(userId)
                        .fileName(fileName).fileType(FileType.CSV_ADQUIRENTE)
                        .storageKey(storageKey).status(ImportStatus.DONE)
                        .totalRecords(0).processedRecords(0).build());
                result.put(acq + "_" + ym, fi.getId());
            }
        }
        return result;
    }

    private Map<String, UUID> createBankFileImports(UUID tenantId, UUID userId,
                                                     LocalDate start, LocalDate end) {
        Map<String, UUID> result = new LinkedHashMap<>();

        Set<YearMonth> months = new LinkedHashSet<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusMonths(1)) {
            months.add(YearMonth.from(d));
        }
        months.add(YearMonth.from(end));

        for (YearMonth ym : months) {
            String label = ym.format(DateTimeFormatter.ofPattern("MMMuuuu", new Locale("pt", "BR")));
            String fileName = "extrato-bancario-" + label.toLowerCase() + ".ofx";
            FileImport fi = fileImportRepo.save(FileImport.builder()
                    .tenantId(tenantId).userId(userId)
                    .fileName(fileName).fileType(FileType.OFX)
                    .storageKey("demo/bank/" + ym + ".ofx")
                    .status(ImportStatus.DONE)
                    .totalRecords(0).processedRecords(0).build());
            result.put(ym.toString(), fi.getId());
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Bulk data generation (90-day period)
    // -------------------------------------------------------------------------

    private void generateBulkData(Random rng, UUID tenantId,
                                   Map<String, Account> accounts,
                                   Map<String, UUID> acquirerImports,
                                   Map<String, UUID> bankImports,
                                   LocalDate start, LocalDate end) {

        Account cash = accounts.get("cash");
        Account ar = accounts.get("ar");
        Account revenue = accounts.get("revenue");
        Account fees = accounts.get("fees");

        List<Receivable> receivables = new ArrayList<>();
        List<BankStatement> bankStatements = new ArrayList<>();
        List<Transaction> ledgerTx = new ArrayList<>();
        List<ReconciliationMatch> matches = new ArrayList<>();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            int count = dailyCount(rng, date.getDayOfWeek());
            for (int i = 0; i < count; i++) {
                SaleType saleType = randomSaleType(rng);
                ReceivableSource source = randomAcquirer(rng, saleType);
                long gross = randomAmountCents(rng);
                int installments = (saleType == SaleType.CREDIT_INSTALLMENT) ? (2 + rng.nextInt(5)) : 1;
                long fee = computeFeeCents(saleType, source, gross);
                long net = gross - fee;
                int settleDays = settlementOffset(saleType);
                String nsu = nsu(rng);
                String auth = auth(rng);

                String acqKey = acquirerKey(source);
                UUID acquirerFileId = acquirerImports.get(acqKey + "_" + YearMonth.from(date));
                UUID bankFileId = bankImports.get(YearMonth.from(date).toString());

                if (installments == 1) {
                    LocalDate settleDate = date.plusDays(settleDays);
                    boolean settled = !settleDate.isAfter(end);

                    Receivable rec = Receivable.builder()
                            .tenantId(tenantId).fileImportId(acquirerFileId)
                            .source(source).saleDate(date)
                            .expectedSettlementDate(settleDate)
                            .grossAmountCents(gross).feeAmountCents(fee)
                            .netAmountCents(net)
                            .installmentNumber(1).totalInstallments(1)
                            .nsu(nsu).authorizationCode(auth)
                            .reconciliationStatus(settled ? ReconciliationStatus.MATCHED : ReconciliationStatus.PENDING)
                            .build();
                    receivables.add(rec);

                    // Ledger: sale
                    ledgerTx.add(buildSaleTx(tenantId, date, gross, source.name(), nsu, ar, revenue));

                    if (settled) {
                        UUID bankFileSettleId = bankImports.get(YearMonth.from(settleDate).toString());
                        BankStatement bs = BankStatement.builder()
                                .tenantId(tenantId).fileImportId(bankFileSettleId != null ? bankFileSettleId : bankFileId)
                                .bankCode("077").agency("0001").accountNumber("123456-7")
                                .transactionDate(settleDate).valueDate(settleDate)
                                .amountCents(net)
                                .description(statementDesc(source, nsu))
                                .documentNumber(nsu)
                                .reconciliationStatus(ReconciliationStatus.MATCHED)
                                .build();
                        bankStatements.add(bs);

                        // Ledger: settlement
                        ledgerTx.add(buildSettlementTx(tenantId, settleDate, gross, fee, source.name(), nsu, cash, ar, fees));

                        // Defer match creation — we need saved IDs; mark for post-save
                        // We store position indices to link after save
                        matches.add(null); // placeholder: index = bankStatements.size()-1
                    }
                } else {
                    // Parcelado: one receivable per installment
                    long grossPer = gross / installments;
                    long feePer = computeFeeCents(SaleType.CREDIT_INSTALLMENT, source, grossPer);
                    long netPer = grossPer - feePer;

                    // Single sale ledger entry for full amount
                    ledgerTx.add(buildSaleTx(tenantId, date, grossPer * installments, source.name() + " " + installments + "x", nsu, ar, revenue));

                    for (int inst = 1; inst <= installments; inst++) {
                        LocalDate settleDate = date.plusDays(30L * inst);
                        boolean settled = !settleDate.isAfter(end);

                        Receivable rec = Receivable.builder()
                                .tenantId(tenantId).fileImportId(acquirerFileId)
                                .source(source).saleDate(date)
                                .expectedSettlementDate(settleDate)
                                .grossAmountCents(grossPer).feeAmountCents(feePer)
                                .netAmountCents(netPer)
                                .installmentNumber(inst).totalInstallments(installments)
                                .nsu(nsu + String.format("%02d", inst)).authorizationCode(auth)
                                .reconciliationStatus(settled ? ReconciliationStatus.MATCHED : ReconciliationStatus.PENDING)
                                .build();
                        receivables.add(rec);

                        if (settled) {
                            UUID bankFileSettleId = bankImports.get(YearMonth.from(settleDate).toString());
                            BankStatement bs = BankStatement.builder()
                                    .tenantId(tenantId).fileImportId(bankFileSettleId != null ? bankFileSettleId : bankFileId)
                                    .bankCode("077").agency("0001").accountNumber("123456-7")
                                    .transactionDate(settleDate).valueDate(settleDate)
                                    .amountCents(netPer)
                                    .description(statementDesc(source, nsu) + " " + inst + "/" + installments)
                                    .documentNumber(nsu + String.format("%02d", inst))
                                    .reconciliationStatus(ReconciliationStatus.MATCHED)
                                    .build();
                            bankStatements.add(bs);
                            ledgerTx.add(buildSettlementTx(tenantId, settleDate, grossPer, feePer, source.name(), nsu, cash, ar, fees));
                            matches.add(null); // placeholder
                        }
                    }
                }
            }
        }

        // Persist in batches
        log.info("[DemoSeeder] Persistindo {} recebíveis, {} extratos bancários, {} lançamentos...",
                receivables.size(), bankStatements.size(), ledgerTx.size());

        receivableRepo.saveAll(receivables);
        List<BankStatement> savedBs = bankStatementRepo.saveAll(bankStatements);
        transactionRepo.saveAll(ledgerTx);

        // Build actual matches now that both receivables and bankStatements have IDs
        // Match by document number (nsu) between settled receivable and bank statement
        buildAndSaveMatches(tenantId, receivables, savedBs);
    }

    private void buildAndSaveMatches(UUID tenantId, List<Receivable> receivables, List<BankStatement> savedBs) {
        // Index bank statements by documentNumber for fast lookup
        Map<String, BankStatement> bsByDoc = new HashMap<>();
        for (BankStatement bs : savedBs) {
            if (bs.getDocumentNumber() != null) {
                bsByDoc.put(bs.getDocumentNumber(), bs);
            }
        }

        List<ReconciliationMatch> matches = new ArrayList<>();
        for (Receivable rec : receivables) {
            if (rec.getReconciliationStatus() != ReconciliationStatus.MATCHED) continue;
            String docKey = rec.getNsu() != null ? rec.getNsu() : "";
            BankStatement bs = bsByDoc.get(docKey);
            if (bs == null) continue;
            matches.add(ReconciliationMatch.builder()
                    .tenantId(tenantId)
                    .bankStatementId(bs.getId())
                    .receivableId(rec.getId())
                    .matchType(MatchType.EXACT)
                    .differenceCents(0L)
                    .build());
        }
        matchRepo.saveAll(matches);
    }

    // -------------------------------------------------------------------------
    // Curated discrepancy cases
    // -------------------------------------------------------------------------

    private void createCuratedCases(UUID tenantId, UUID userId,
                                     Map<String, Account> accounts,
                                     Map<String, UUID> acquirerImports,
                                     Map<String, UUID> bankImports,
                                     LocalDate anchor) {
        Account cash = accounts.get("cash");
        Account ar = accounts.get("ar");
        Account revenue = accounts.get("revenue");
        Account fees = accounts.get("fees");

        UUID stoneImport = acquirerImports.get("STONE_" + YearMonth.from(anchor.minusDays(25)));
        UUID cieloImport = acquirerImports.get("CIELO_" + YearMonth.from(anchor.minusDays(20)));
        UUID bankImportAnchor = bankImports.get(YearMonth.from(anchor).toString());
        if (bankImportAnchor == null) {
            bankImportAnchor = bankImports.values().iterator().next();
        }

        // ---- Case 1: FEE_DIVERGENCE — Stone cobrou taxa maior que a contratada ----
        {
            LocalDate saleDate = anchor.minusDays(25);
            LocalDate settleDate = saleDate.plusDays(1);
            long gross = 68000L; // R$680,00
            long contractedFee = 1904L; // 2.8%
            long actualFee = 2720L;     // 4.0% (cobrado)
            long expectedNet = gross - contractedFee; // R$661,96
            long actualNet = gross - actualFee;       // R$652,80
            long diff = expectedNet - actualNet;       // R$8,16

            Receivable rec = receivableRepo.save(Receivable.builder()
                    .tenantId(tenantId).fileImportId(stoneImport != null ? stoneImport : acquirerImports.values().iterator().next())
                    .source(ReceivableSource.STONE).saleDate(saleDate)
                    .expectedSettlementDate(settleDate)
                    .grossAmountCents(gross).feeAmountCents(contractedFee)
                    .netAmountCents(expectedNet)
                    .nsu("FEE001").authorizationCode("A00001")
                    .reconciliationStatus(ReconciliationStatus.PARTIAL)
                    .build());

            BankStatement bs = bankStatementRepo.save(BankStatement.builder()
                    .tenantId(tenantId).fileImportId(bankImportAnchor)
                    .bankCode("077").agency("0001").accountNumber("123456-7")
                    .transactionDate(settleDate).valueDate(settleDate)
                    .amountCents(actualNet)
                    .description("Crédito Stone — venda " + saleDate)
                    .documentNumber("FEE001")
                    .reconciliationStatus(ReconciliationStatus.PARTIAL)
                    .build());

            transactionRepo.save(buildSaleTx(tenantId, saleDate, gross, "Stone crédito", "FEE001", ar, revenue));
            transactionRepo.save(buildSettlementTx(tenantId, settleDate, gross, actualFee, "Stone", "FEE001", cash, ar, fees));

            discrepancyRepo.save(ReconciliationDiscrepancy.builder()
                    .tenantId(tenantId).type(DiscrepancyType.FEE_DIVERGENCE)
                    .bankStatementId(bs.getId()).receivableId(rec.getId())
                    .amountCents(diff)
                    .description("Taxa Stone cobrada 1,2% acima do contratado na venda NSU FEE001 de " +
                            saleDate + " — contratada 2,8% (R$19,04), cobrada 4,0% (R$27,20), " +
                            "diferença de R$8,16 não creditada.")
                    .build());
        }

        // ---- Case 2: MISSING_CREDIT — recebível sem crédito bancário ----
        {
            LocalDate saleDate = anchor.minusDays(20);
            LocalDate settleDate = saleDate; // Pix D+0
            long gross = 128000L; // R$1.280,00

            Receivable rec = receivableRepo.save(Receivable.builder()
                    .tenantId(tenantId).fileImportId(bankImportAnchor)
                    .source(ReceivableSource.PIX).saleDate(saleDate)
                    .expectedSettlementDate(settleDate)
                    .grossAmountCents(gross).feeAmountCents(0L)
                    .netAmountCents(gross)
                    .nsu("PIX002").authorizationCode("P00002")
                    .reconciliationStatus(ReconciliationStatus.UNMATCHED)
                    .build());

            transactionRepo.save(buildSaleTx(tenantId, saleDate, gross, "Pix", "PIX002", ar, revenue));

            discrepancyRepo.save(ReconciliationDiscrepancy.builder()
                    .tenantId(tenantId).type(DiscrepancyType.MISSING_CREDIT)
                    .receivableId(rec.getId())
                    .amountCents(gross)
                    .description("Pix de R$1.280,00 registrado como venda em " + saleDate +
                            " (NSU PIX002) nunca apareceu no extrato bancário. " +
                            "Possível falha de liquidação na chave Pix da loja.")
                    .build());
        }

        // ---- Case 3: CHARGEBACK — estorno de venda de crédito ----
        {
            LocalDate originalSaleDate = anchor.minusDays(35);
            LocalDate originalSettleDate = originalSaleDate.plusDays(1);
            LocalDate chargebackDate = anchor.minusDays(15);
            long gross = 34000L; // R$340,00
            long fee = 952L;     // 2.8%
            long net = gross - fee;

            Receivable originalRec = receivableRepo.save(Receivable.builder()
                    .tenantId(tenantId).fileImportId(cieloImport != null ? cieloImport : acquirerImports.values().iterator().next())
                    .source(ReceivableSource.CIELO).saleDate(originalSaleDate)
                    .expectedSettlementDate(originalSettleDate)
                    .grossAmountCents(gross).feeAmountCents(fee)
                    .netAmountCents(net)
                    .nsu("CBK003").authorizationCode("C00003")
                    .reconciliationStatus(ReconciliationStatus.MATCHED)
                    .build());

            BankStatement originalBs = bankStatementRepo.save(BankStatement.builder()
                    .tenantId(tenantId).fileImportId(bankImportAnchor)
                    .bankCode("077").agency("0001").accountNumber("123456-7")
                    .transactionDate(originalSettleDate).valueDate(originalSettleDate)
                    .amountCents(net).description("Crédito Cielo — venda " + originalSaleDate)
                    .documentNumber("CBK003")
                    .reconciliationStatus(ReconciliationStatus.MATCHED)
                    .build());

            matchRepo.save(ReconciliationMatch.builder()
                    .tenantId(tenantId)
                    .bankStatementId(originalBs.getId()).receivableId(originalRec.getId())
                    .matchType(MatchType.EXACT).differenceCents(0L)
                    .notes("Match original antes do chargeback").build());

            // Chargeback entry (negative)
            BankStatement chargebackBs = bankStatementRepo.save(BankStatement.builder()
                    .tenantId(tenantId).fileImportId(bankImportAnchor)
                    .bankCode("077").agency("0001").accountNumber("123456-7")
                    .transactionDate(chargebackDate).valueDate(chargebackDate)
                    .amountCents(-gross)
                    .description("CHARGEBACK Cielo — estorno venda " + originalSaleDate + " NSU CBK003")
                    .documentNumber("CBK003-CB")
                    .reconciliationStatus(ReconciliationStatus.UNMATCHED)
                    .build());

            transactionRepo.save(buildSaleTx(tenantId, originalSaleDate, gross, "Cielo crédito", "CBK003", ar, revenue));
            transactionRepo.save(buildSettlementTx(tenantId, originalSettleDate, gross, fee, "Cielo", "CBK003", cash, ar, fees));

            discrepancyRepo.save(ReconciliationDiscrepancy.builder()
                    .tenantId(tenantId).type(DiscrepancyType.CHARGEBACK)
                    .bankStatementId(chargebackBs.getId()).receivableId(originalRec.getId())
                    .amountCents(gross)
                    .description("Chargeback de R$340,00 recebido em " + chargebackDate +
                            " referente à venda Cielo de " + originalSaleDate +
                            " (NSU CBK003). Cliente contestou a cobrança junto à operadora.")
                    .build());
        }

        // ---- Case 4: DATE_DIVERGENCE — liquidação 2 dias atrasada ----
        {
            LocalDate saleDate = anchor.minusDays(10);
            LocalDate expectedSettle = saleDate.plusDays(1);
            LocalDate actualSettle = saleDate.plusDays(3); // 2 dias de atraso
            long gross = 21500L; // R$215,00
            long fee = 323L;     // 1.5% débito Rede
            long net = gross - fee;

            Receivable rec = receivableRepo.save(Receivable.builder()
                    .tenantId(tenantId).fileImportId(acquirerImports.getOrDefault(
                            "REDE_" + YearMonth.from(saleDate),
                            acquirerImports.values().iterator().next()))
                    .source(ReceivableSource.REDE).saleDate(saleDate)
                    .expectedSettlementDate(expectedSettle)
                    .grossAmountCents(gross).feeAmountCents(fee)
                    .netAmountCents(net)
                    .nsu("DAT004").authorizationCode("R00004")
                    .reconciliationStatus(ReconciliationStatus.MATCHED)
                    .build());

            BankStatement bs = bankStatementRepo.save(BankStatement.builder()
                    .tenantId(tenantId).fileImportId(bankImportAnchor)
                    .bankCode("077").agency("0001").accountNumber("123456-7")
                    .transactionDate(actualSettle).valueDate(actualSettle)
                    .amountCents(net)
                    .description("Crédito Rede débito — " + saleDate)
                    .documentNumber("DAT004")
                    .reconciliationStatus(ReconciliationStatus.MATCHED)
                    .build());

            transactionRepo.save(buildSaleTx(tenantId, saleDate, gross, "Rede débito", "DAT004", ar, revenue));
            transactionRepo.save(buildSettlementTx(tenantId, actualSettle, gross, fee, "Rede", "DAT004", cash, ar, fees));

            matchRepo.save(ReconciliationMatch.builder()
                    .tenantId(tenantId)
                    .bankStatementId(bs.getId()).receivableId(rec.getId())
                    .matchType(MatchType.FUZZY).differenceCents(0L)
                    .notes("Valores batem, mas data de liquidação chegou 2 dias após o previsto").build());

            discrepancyRepo.save(ReconciliationDiscrepancy.builder()
                    .tenantId(tenantId).type(DiscrepancyType.DATE_DIVERGENCE)
                    .bankStatementId(bs.getId()).receivableId(rec.getId())
                    .amountCents(0L)
                    .description("Débito Rede de R$215,00 (NSU DAT004) liquidado em " + actualSettle +
                            " — 2 dias após o esperado (" + expectedSettle + "). " +
                            "Conciliado via match FUZZY por valor e documento.")
                    .build());
        }

        // ---- Case 5: Match EXACT perfeito em destaque ----
        {
            LocalDate saleDate = anchor.minusDays(7);
            long gross = 8990L; // R$89,90
            String nsu = "PIX005";

            Receivable rec = receivableRepo.save(Receivable.builder()
                    .tenantId(tenantId).fileImportId(bankImportAnchor)
                    .source(ReceivableSource.PIX).saleDate(saleDate)
                    .expectedSettlementDate(saleDate)
                    .grossAmountCents(gross).feeAmountCents(0L)
                    .netAmountCents(gross)
                    .nsu(nsu).authorizationCode("P00005")
                    .reconciliationStatus(ReconciliationStatus.MATCHED)
                    .build());

            BankStatement bs = bankStatementRepo.save(BankStatement.builder()
                    .tenantId(tenantId).fileImportId(bankImportAnchor)
                    .bankCode("077").agency("0001").accountNumber("123456-7")
                    .transactionDate(saleDate).valueDate(saleDate)
                    .amountCents(gross)
                    .description("Pix recebido — " + nsu)
                    .documentNumber(nsu)
                    .reconciliationStatus(ReconciliationStatus.MATCHED)
                    .build());

            transactionRepo.save(buildSaleTx(tenantId, saleDate, gross, "Pix", nsu, ar, revenue));
            transactionRepo.save(buildSettlementTx(tenantId, saleDate, gross, 0L, "Pix", nsu, cash, ar, fees));

            matchRepo.save(ReconciliationMatch.builder()
                    .tenantId(tenantId)
                    .bankStatementId(bs.getId()).receivableId(rec.getId())
                    .matchType(MatchType.EXACT).differenceCents(0L)
                    .notes("Happy path: valor R$89,90, data " + saleDate + " e NSU " + nsu + " idênticos nos dois lados.")
                    .build());
        }

        // ---- Case 6: Crédito bancário órfão — dinheiro sem venda correspondente ----
        {
            LocalDate creditDate = anchor.minusDays(3);
            long amount = 215000L; // R$2.150,00

            BankStatement bs = bankStatementRepo.save(BankStatement.builder()
                    .tenantId(tenantId).fileImportId(bankImportAnchor)
                    .bankCode("077").agency("0001").accountNumber("123456-7")
                    .transactionDate(creditDate).valueDate(creditDate)
                    .amountCents(amount)
                    .description("TED recebida — sem identificação de origem")
                    .documentNumber("TED006")
                    .reconciliationStatus(ReconciliationStatus.UNMATCHED)
                    .build());

            discrepancyRepo.save(ReconciliationDiscrepancy.builder()
                    .tenantId(tenantId).type(DiscrepancyType.UNEXPECTED_DEBIT)
                    .bankStatementId(bs.getId())
                    .amountCents(amount)
                    .description("TED de R$2.150,00 creditada em " + creditDate +
                            " sem venda correspondente registrada no sistema. " +
                            "Possível devolução de fornecedor ou repasse de outra conta — requer identificação manual.")
                    .build());
        }
    }

    // -------------------------------------------------------------------------
    // Ledger helpers
    // -------------------------------------------------------------------------

    private Transaction buildSaleTx(UUID tenantId, LocalDate date, long grossCents,
                                     String method, String nsu,
                                     Account ar, Account revenue) {
        OffsetDateTime odt = date.atTime(10, 0).atOffset(BRT);
        Transaction tx = Transaction.builder()
                .tenantId(tenantId)
                .description("Venda " + method + " — NSU " + nsu)
                .occurredAt(odt)
                .build();
        tx.getEntries().add(Entry.builder().transaction(tx).account(ar)
                .tenantId(tenantId).amountCents(grossCents).direction(Direction.DEBIT).build());
        tx.getEntries().add(Entry.builder().transaction(tx).account(revenue)
                .tenantId(tenantId).amountCents(grossCents).direction(Direction.CREDIT).build());
        return tx;
    }

    private Transaction buildSettlementTx(UUID tenantId, LocalDate settleDate, long grossCents,
                                           long feeCents, String acquirer, String nsu,
                                           Account cash, Account ar, Account fees) {
        long net = grossCents - feeCents;
        OffsetDateTime odt = settleDate.atTime(3, 0).atOffset(BRT);
        Transaction tx = Transaction.builder()
                .tenantId(tenantId)
                .description("Liquidação " + acquirer + " — NSU " + nsu)
                .occurredAt(odt)
                .build();
        tx.getEntries().add(Entry.builder().transaction(tx).account(cash)
                .tenantId(tenantId).amountCents(net > 0 ? net : grossCents).direction(Direction.DEBIT).build());
        if (feeCents > 0) {
            tx.getEntries().add(Entry.builder().transaction(tx).account(fees)
                    .tenantId(tenantId).amountCents(feeCents).direction(Direction.DEBIT).build());
        }
        tx.getEntries().add(Entry.builder().transaction(tx).account(ar)
                .tenantId(tenantId).amountCents(grossCents).direction(Direction.CREDIT).build());
        return tx;
    }

    // -------------------------------------------------------------------------
    // Random generation helpers
    // -------------------------------------------------------------------------

    private int dailyCount(Random rng, DayOfWeek dow) {
        int base = switch (dow) {
            case MONDAY -> 15 + rng.nextInt(15);
            case SATURDAY, SUNDAY -> 40 + rng.nextInt(21);
            default -> 25 + rng.nextInt(26);
        };
        return base;
    }

    private long randomAmountCents(Random rng) {
        // log-normal: E[X] ≈ R$22, σ=0.55 in log space
        double logVal = 2.94 + 0.55 * rng.nextGaussian();
        long cents = Math.round(Math.exp(logVal) * 100);
        cents = (cents / 10) * 10; // round to nearest R$0.10
        return Math.max(800L, Math.min(12000L, cents));
    }

    private SaleType randomSaleType(Random rng) {
        int r = rng.nextInt(100);
        if (r < 55) return SaleType.PIX;
        if (r < 62) return SaleType.DEBIT;
        if (r < 82) return SaleType.CREDIT_SIGHT;
        if (r < 90) return SaleType.CREDIT_INSTALLMENT;
        return SaleType.BOLETO;
    }

    private ReceivableSource randomAcquirer(Random rng, SaleType saleType) {
        return switch (saleType) {
            case PIX -> ReceivableSource.PIX;
            case BOLETO -> ReceivableSource.BOLETO;
            default -> {
                int r = rng.nextInt(100);
                if (r < 40) yield ReceivableSource.STONE;
                if (r < 70) yield ReceivableSource.CIELO;
                if (r < 90) yield ReceivableSource.REDE;
                yield ReceivableSource.MERCADOPAGO;
            }
        };
    }

    private long computeFeeCents(SaleType saleType, ReceivableSource source, long grossCents) {
        double rate = switch (saleType) {
            case PIX -> 0.0;
            case BOLETO -> 0.0; // flat fee applied separately below
            case DEBIT -> switch (source) {
                case STONE -> 0.0169;
                case CIELO -> 0.0189;
                case REDE -> 0.0159;
                case MERCADOPAGO -> 0.0199;
                default -> 0.018;
            };
            case CREDIT_SIGHT -> switch (source) {
                case STONE -> 0.0279;
                case CIELO -> 0.0299;
                case REDE -> 0.0269;
                case MERCADOPAGO -> 0.0319;
                default -> 0.029;
            };
            case CREDIT_INSTALLMENT -> switch (source) {
                case STONE -> 0.0459;
                case CIELO -> 0.0499;
                case REDE -> 0.0439;
                case MERCADOPAGO -> 0.0519;
                default -> 0.047;
            };
        };
        if (saleType == SaleType.BOLETO) return 349L; // R$3,49 flat
        return Math.round(grossCents * rate);
    }

    private int settlementOffset(SaleType saleType) {
        return switch (saleType) {
            case PIX -> 0;
            case DEBIT, CREDIT_SIGHT -> 1;
            case CREDIT_INSTALLMENT -> 30;
            case BOLETO -> 3;
        };
    }

    private String nsu(Random rng) {
        return String.format("%08d", rng.nextInt(99_999_999));
    }

    private String auth(Random rng) {
        return String.format("%06d", rng.nextInt(999_999));
    }

    private String acquirerKey(ReceivableSource source) {
        return switch (source) {
            case STONE -> "STONE";
            case CIELO -> "CIELO";
            case REDE -> "REDE";
            case MERCADOPAGO -> "MERCADOPAGO";
            default -> "STONE";
        };
    }

    private String statementDesc(ReceivableSource source, String nsu) {
        return switch (source) {
            case PIX -> "Pix recebido";
            case CIELO -> "Crédito Cielo";
            case REDE -> "Crédito Rede";
            case MERCADOPAGO -> "Crédito Mercado Pago";
            case BOLETO -> "Liquidação boleto";
            default -> "Crédito Stone";
        } + " — " + nsu;
    }

    private enum SaleType { PIX, DEBIT, CREDIT_SIGHT, CREDIT_INSTALLMENT, BOLETO }
}