package dev.confera.reconciliation.service;

import dev.confera.ingestion.entity.BankStatement;
import dev.confera.ingestion.entity.Receivable;
import dev.confera.ingestion.entity.ReconciliationStatus;
import dev.confera.ingestion.repository.BankStatementRepository;
import dev.confera.ingestion.repository.ReceivableRepository;
import dev.confera.reconciliation.entity.*;
import dev.confera.reconciliation.repository.ReconciliationDiscrepancyRepository;
import dev.confera.reconciliation.repository.ReconciliationMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationEngine {

    private final BankStatementRepository bankStatementRepo;
    private final ReceivableRepository receivableRepo;
    private final ReconciliationMatchRepository matchRepo;
    private final ReconciliationDiscrepancyRepository discrepancyRepo;
    private final ApplicationContext ctx;

    public record RunResult(
        int totalBankStatements,
        int totalReceivables,
        int exactMatches,
        int fuzzyMatches,
        int unmatched,
        int discrepanciesCreated
    ) {}

    private record PassResult(int matchCount, int discrepancyCount) {}

    public RunResult runForTenant(UUID tenantId) {
        long totalBS = bankStatementRepo.countByTenantIdAndReconciliationStatus(tenantId, ReconciliationStatus.PENDING);
        long totalRV = receivableRepo.countByTenantIdAndReconciliationStatus(tenantId, ReconciliationStatus.PENDING);

        log.info("[Reconciliation] Tenant {}: {} bank statements, {} receivables pending",
            tenantId, totalBS, totalRV);

        ReconciliationEngine self = ctx.getBean(ReconciliationEngine.class);

        PassResult pass1 = self.executeExactPass(tenantId);
        log.info("[Reconciliation] Pass 1 (EXACT): {} matches", pass1.matchCount());

        PassResult pass2 = self.executeFuzzyPass(tenantId);
        log.info("[Reconciliation] Pass 2 (FUZZY): {} matches, {} discrepancies",
            pass2.matchCount(), pass2.discrepancyCount());

        PassResult pass3 = self.executeUnmatchedPass(tenantId);
        log.info("[Reconciliation] Pass 3 (UNMATCHED): {} items signaled", pass3.discrepancyCount());

        return new RunResult(
            (int) totalBS, (int) totalRV,
            pass1.matchCount(), pass2.matchCount(),
            pass3.discrepancyCount(),
            pass1.discrepancyCount() + pass2.discrepancyCount() + pass3.discrepancyCount()
        );
    }

    @Transactional
    public PassResult executeExactPass(UUID tenantId) {
        List<BankStatement> bsPending = bankStatementRepo
            .findByTenantIdAndReconciliationStatus(tenantId, ReconciliationStatus.PENDING);
        List<Receivable> rvPending = receivableRepo
            .findByTenantIdAndReconciliationStatus(tenantId, ReconciliationStatus.PENDING);

        // Build O(1) lookup maps by NSU and authorization code
        Map<String, Receivable> byNsu = new HashMap<>();
        Map<String, Receivable> byAuthCode = new HashMap<>();
        for (Receivable rv : rvPending) {
            if (rv.getNsu() != null && !rv.getNsu().isBlank()) {
                byNsu.put(rv.getNsu(), rv);
            }
            if (rv.getAuthorizationCode() != null && !rv.getAuthorizationCode().isBlank()) {
                byAuthCode.put(rv.getAuthorizationCode(), rv);
            }
        }

        Set<UUID> matchedRvIds = new HashSet<>();
        List<ReconciliationMatch> newMatches = new ArrayList<>();

        for (BankStatement bs : bsPending) {
            String docNum = bs.getDocumentNumber();
            if (docNum == null || docNum.isBlank()) continue;

            Receivable candidate = byNsu.containsKey(docNum) ? byNsu.get(docNum) : byAuthCode.get(docNum);
            if (candidate == null || matchedRvIds.contains(candidate.getId())) continue;

            if (!candidate.getNetAmountCents().equals(bs.getAmountCents())) continue;
            if (!candidate.getExpectedSettlementDate().equals(bs.getTransactionDate())) continue;

            bs.setReconciliationStatus(ReconciliationStatus.MATCHED);
            candidate.setReconciliationStatus(ReconciliationStatus.MATCHED);
            matchedRvIds.add(candidate.getId());

            newMatches.add(ReconciliationMatch.builder()
                .tenantId(tenantId)
                .bankStatementId(bs.getId())
                .receivableId(candidate.getId())
                .matchType(MatchType.EXACT)
                .differenceCents(0L)
                .build());
        }

        matchRepo.saveAll(newMatches);
        return new PassResult(newMatches.size(), 0);
    }

    @Transactional
    public PassResult executeFuzzyPass(UUID tenantId) {
        List<BankStatement> bsPending = bankStatementRepo
            .findByTenantIdAndReconciliationStatus(tenantId, ReconciliationStatus.PENDING);
        List<Receivable> rvPending = receivableRepo
            .findByTenantIdAndReconciliationStatus(tenantId, ReconciliationStatus.PENDING);

        Set<UUID> matchedRvIds = new HashSet<>();
        List<ReconciliationMatch> newMatches = new ArrayList<>();
        List<ReconciliationDiscrepancy> newDiscrepancies = new ArrayList<>();

        for (BankStatement bs : bsPending) {
            Optional<Receivable> candidate = rvPending.stream()
                .filter(rv -> !matchedRvIds.contains(rv.getId()))
                .filter(rv -> Math.abs(bs.getAmountCents() - rv.getNetAmountCents()) <= 500)
                .filter(rv -> {
                    LocalDate expected = rv.getExpectedSettlementDate();
                    return !bs.getTransactionDate().isBefore(expected.minusDays(2))
                        && !bs.getTransactionDate().isAfter(expected.plusDays(2));
                })
                .findFirst();

            if (candidate.isEmpty()) continue;

            Receivable rv = candidate.get();
            long diff = bs.getAmountCents() - rv.getNetAmountCents();

            bs.setReconciliationStatus(ReconciliationStatus.MATCHED);
            rv.setReconciliationStatus(ReconciliationStatus.MATCHED);
            matchedRvIds.add(rv.getId());

            newMatches.add(ReconciliationMatch.builder()
                .tenantId(tenantId)
                .bankStatementId(bs.getId())
                .receivableId(rv.getId())
                .matchType(MatchType.FUZZY)
                .differenceCents(diff)
                .build());

            if (diff != 0) {
                newDiscrepancies.add(ReconciliationDiscrepancy.builder()
                    .tenantId(tenantId)
                    .type(DiscrepancyType.FEE_DIVERGENCE)
                    .bankStatementId(bs.getId())
                    .receivableId(rv.getId())
                    .amountCents(Math.abs(diff))
                    .description("Fee divergence of %d cents in fuzzy match".formatted(Math.abs(diff)))
                    .status(DiscrepancyStatus.OPEN)
                    .build());
            }
        }

        matchRepo.saveAll(newMatches);
        discrepancyRepo.saveAll(newDiscrepancies);
        return new PassResult(newMatches.size(), newDiscrepancies.size());
    }

    @Transactional
    public PassResult executeUnmatchedPass(UUID tenantId) {
        List<BankStatement> bsPending = bankStatementRepo
            .findByTenantIdAndReconciliationStatus(tenantId, ReconciliationStatus.PENDING);
        List<Receivable> rvPending = receivableRepo
            .findByTenantIdAndReconciliationStatus(tenantId, ReconciliationStatus.PENDING);

        LocalDate today = LocalDate.now();
        List<ReconciliationDiscrepancy> newDiscrepancies = new ArrayList<>();

        for (BankStatement bs : bsPending) {
            bs.setReconciliationStatus(ReconciliationStatus.UNMATCHED);
            newDiscrepancies.add(ReconciliationDiscrepancy.builder()
                .tenantId(tenantId)
                .type(DiscrepancyType.UNEXPECTED_DEBIT)
                .bankStatementId(bs.getId())
                .amountCents(Math.abs(bs.getAmountCents()))
                .description("Bank statement with no matching receivable: " + bs.getDescription())
                .status(DiscrepancyStatus.OPEN)
                .build());
        }

        for (Receivable rv : rvPending) {
            if (rv.getExpectedSettlementDate().isBefore(today)) {
                rv.setReconciliationStatus(ReconciliationStatus.UNMATCHED);
                newDiscrepancies.add(ReconciliationDiscrepancy.builder()
                    .tenantId(tenantId)
                    .type(DiscrepancyType.MISSING_CREDIT)
                    .receivableId(rv.getId())
                    .amountCents(rv.getNetAmountCents())
                    .description("Expected receivable not received by settlement date %s"
                        .formatted(rv.getExpectedSettlementDate()))
                    .status(DiscrepancyStatus.OPEN)
                    .build());
            }
        }

        discrepancyRepo.saveAll(newDiscrepancies);
        return new PassResult(0, newDiscrepancies.size());
    }
}