package dev.confera.ledger.controller;

import dev.confera.identity.security.UserPrincipal;
import dev.confera.ledger.dto.AccountResponse;
import dev.confera.ledger.dto.BalanceResponse;
import dev.confera.ledger.dto.CreateAccountRequest;
import dev.confera.ledger.service.AccountService;
import dev.confera.ledger.service.LedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ledger/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final LedgerService ledgerService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
        @Valid @RequestBody CreateAccountRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        AccountResponse response = accountService.createAccount(principal.tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAccounts(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(accountService.listAccounts(principal.tenantId()));
    }

    @GetMapping("/{code}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
        @PathVariable String code,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ledgerService.getBalance(principal.tenantId(), code));
    }
}