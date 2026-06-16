package dev.confera.ledger.controller;

import dev.confera.identity.security.UserPrincipal;
import dev.confera.ledger.dto.EntryResponse;
import dev.confera.ledger.dto.PostTransactionRequest;
import dev.confera.ledger.dto.TransactionResponse;
import dev.confera.ledger.exception.IdempotentTransactionException;
import dev.confera.ledger.service.LedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/ledger")
@RequiredArgsConstructor
public class TransactionController {

    private final LedgerService ledgerService;

    @ExceptionHandler(IdempotentTransactionException.class)
    public ResponseEntity<TransactionResponse> handleIdempotent(IdempotentTransactionException ex) {
        return ResponseEntity.ok(ex.getExisting());
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> post(
        @Valid @RequestBody PostTransactionRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        TransactionResponse response = ledgerService.post(principal.tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionResponse> getById(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ledgerService.getById(principal.tenantId(), id));
    }

    @PostMapping("/transactions/{id}/reverse")
    public ResponseEntity<TransactionResponse> reverse(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.reverse(principal.tenantId(), id));
    }

    @GetMapping("/accounts/{code}/statement")
    public ResponseEntity<Page<EntryResponse>> getStatement(
        @PathVariable String code,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ledgerService.getStatement(principal.tenantId(), code, from, to, pageable));
    }
}