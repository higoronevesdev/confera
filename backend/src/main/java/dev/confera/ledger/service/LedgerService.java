package dev.confera.ledger.service;

import dev.confera.ledger.dto.*;
import dev.confera.ledger.entity.*;
import dev.confera.ledger.exception.*;
import dev.confera.ledger.repository.*;
import dev.confera.shared.outbox.OutboxEvent;
import dev.confera.shared.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final EntryRepository entryRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public TransactionResponse post(UUID tenantId, PostTransactionRequest request) {
        if (request.idempotencyKey() != null) {
            transactionRepository.findByIdempotencyKey(request.idempotencyKey())
                .map(tx -> transactionRepository.findByIdWithEntries(tx.getId()).orElse(tx))
                .map(TransactionResponse::from)
                .ifPresent(existing -> { throw new IdempotentTransactionException(existing); });
        }

        validateBalance(request.entries());

        Map<String, Account> accountsByCode = resolveAccounts(tenantId, request.entries());

        Transaction transaction = Transaction.builder()
            .tenantId(tenantId)
            .description(request.description())
            .occurredAt(request.occurredAt())
            .idempotencyKey(request.idempotencyKey())
            .build();

        for (EntryRequest req : request.entries()) {
            Entry entry = Entry.builder()
                .transaction(transaction)
                .account(accountsByCode.get(req.accountCode()))
                .tenantId(tenantId)
                .amountCents(req.amountCents())
                .direction(req.direction())
                .build();
            transaction.getEntries().add(entry);
        }

        Transaction saved = transactionRepository.save(transaction);
        saveOutboxEvent(saved, saved.getEntries(), "TransactionPosted");

        return TransactionResponse.from(saved, saved.getEntries());
    }

    @Transactional
    public TransactionResponse reverse(UUID tenantId, UUID transactionId) {
        Transaction original = transactionRepository.findByIdWithEntries(transactionId)
            .filter(t -> t.getTenantId().equals(tenantId))
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        Transaction reversal = Transaction.builder()
            .tenantId(tenantId)
            .description("REVERSAL: " + original.getDescription())
            .occurredAt(OffsetDateTime.now(ZoneOffset.UTC))
            .build();

        for (Entry originalEntry : original.getEntries()) {
            Entry entry = Entry.builder()
                .transaction(reversal)
                .account(originalEntry.getAccount())
                .tenantId(tenantId)
                .amountCents(originalEntry.getAmountCents())
                .direction(originalEntry.getDirection().opposite())
                .build();
            reversal.getEntries().add(entry);
        }

        Transaction saved = transactionRepository.save(reversal);
        saveOutboxEvent(saved, saved.getEntries(), "TransactionReversed");

        return TransactionResponse.from(saved, saved.getEntries());
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(UUID tenantId, UUID transactionId) {
        return transactionRepository.findByIdWithEntries(transactionId)
            .filter(t -> t.getTenantId().equals(tenantId))
            .map(TransactionResponse::from)
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID tenantId, String accountCode) {
        Account account = accountRepository.findByTenantIdAndCode(tenantId, accountCode)
            .orElseThrow(() -> new AccountNotFoundException(accountCode));

        long totalDebit  = entryRepository.sumAmountByAccountIdAndDirection(account.getId(), Direction.DEBIT);
        long totalCredit = entryRepository.sumAmountByAccountIdAndDirection(account.getId(), Direction.CREDIT);
        long balance = account.getNormalBalance() == Direction.DEBIT
            ? totalDebit - totalCredit
            : totalCredit - totalDebit;

        return new BalanceResponse(account.getId(), account.getCode(), account.getName(),
            balance, totalDebit, totalCredit);
    }

    @Transactional(readOnly = true)
    public Page<EntryResponse> getStatement(UUID tenantId, String accountCode, LocalDate from, LocalDate to, Pageable pageable) {
        Account account = accountRepository.findByTenantIdAndCode(tenantId, accountCode)
            .orElseThrow(() -> new AccountNotFoundException(accountCode));

        OffsetDateTime fromDt = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toDt   = to.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        return entryRepository.findStatementEntries(account.getId(), fromDt, toDt, pageable)
            .map(EntryResponse::from);
    }

    private void validateBalance(List<EntryRequest> entries) {
        long totalDebit  = entries.stream().filter(e -> e.direction() == Direction.DEBIT).mapToLong(EntryRequest::amountCents).sum();
        long totalCredit = entries.stream().filter(e -> e.direction() == Direction.CREDIT).mapToLong(EntryRequest::amountCents).sum();
        if (totalDebit != totalCredit) {
            throw new LedgerImbalanceException(totalDebit, totalCredit);
        }
    }

    private Map<String, Account> resolveAccounts(UUID tenantId, List<EntryRequest> entries) {
        Map<String, Account> map = new LinkedHashMap<>();
        for (EntryRequest req : entries) {
            map.computeIfAbsent(req.accountCode(), code ->
                accountRepository.findByTenantIdAndCode(tenantId, code)
                    .orElseThrow(() -> new AccountNotFoundException(code)));
        }
        return map;
    }

    private void saveOutboxEvent(Transaction tx, List<Entry> entries, String eventType) {
        long totalDebit  = entries.stream().filter(e -> e.getDirection() == Direction.DEBIT).mapToLong(Entry::getAmountCents).sum();
        long totalCredit = entries.stream().filter(e -> e.getDirection() == Direction.CREDIT).mapToLong(Entry::getAmountCents).sum();

        String payload = "{\"transactionId\":\"%s\",\"tenantId\":\"%s\",\"occurredAt\":\"%s\",\"entryCount\":%d,\"totalDebitCents\":%d,\"totalCreditCents\":%d}"
            .formatted(tx.getId(), tx.getTenantId(), tx.getOccurredAt(), entries.size(), totalDebit, totalCredit);

        String routingKey = "TransactionPosted".equals(eventType)
            ? "ledger.transaction.posted"
            : "ledger.transaction.reversed";

        outboxEventRepository.save(OutboxEvent.builder()
            .aggregateType("Transaction")
            .aggregateId(tx.getId().toString())
            .eventType(eventType)
            .routingKey(routingKey)
            .payload(payload)
            .build());
    }
}