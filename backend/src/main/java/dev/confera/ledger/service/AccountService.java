package dev.confera.ledger.service;

import dev.confera.ledger.dto.AccountResponse;
import dev.confera.ledger.dto.CreateAccountRequest;
import dev.confera.ledger.entity.Account;
import dev.confera.ledger.entity.Direction;
import dev.confera.ledger.exception.AccountAlreadyExistsException;
import dev.confera.ledger.exception.AccountNotFoundException;
import dev.confera.ledger.repository.AccountRepository;
import dev.confera.ledger.repository.EntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final EntryRepository entryRepository;

    @Transactional
    public AccountResponse createAccount(UUID tenantId, CreateAccountRequest request) {
        if (accountRepository.existsByTenantIdAndCode(tenantId, request.code())) {
            throw new AccountAlreadyExistsException(request.code());
        }

        if (request.parentId() != null) {
            accountRepository.findById(request.parentId())
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new AccountNotFoundException(request.parentId()));
        }

        Account saved = accountRepository.save(Account.builder()
            .tenantId(tenantId)
            .name(request.name())
            .code(request.code())
            .type(request.type())
            .normalBalance(request.normalBalance())
            .parentId(request.parentId())
            .build());

        return AccountResponse.from(saved, 0L);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts(UUID tenantId) {
        return accountRepository.findByTenantIdAndActiveTrue(tenantId).stream()
            .map(account -> AccountResponse.from(account, computeBalance(account)))
            .toList();
    }

    @Transactional(readOnly = true)
    public Account findByCode(UUID tenantId, String code) {
        return accountRepository.findByTenantIdAndCode(tenantId, code)
            .orElseThrow(() -> new AccountNotFoundException(code));
    }

    long computeBalance(Account account) {
        long totalDebit  = entryRepository.sumAmountByAccountIdAndDirection(account.getId(), Direction.DEBIT);
        long totalCredit = entryRepository.sumAmountByAccountIdAndDirection(account.getId(), Direction.CREDIT);
        return account.getNormalBalance() == Direction.DEBIT
            ? totalDebit - totalCredit
            : totalCredit - totalDebit;
    }
}