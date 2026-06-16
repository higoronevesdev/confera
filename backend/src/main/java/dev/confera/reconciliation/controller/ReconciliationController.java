package dev.confera.reconciliation.controller;

import dev.confera.identity.security.UserPrincipal;
import dev.confera.reconciliation.dto.*;
import dev.confera.reconciliation.entity.DiscrepancyStatus;
import dev.confera.reconciliation.service.ReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping("/run")
    public ResponseEntity<ReconciliationSummaryResponse> run(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(reconciliationService.triggerReconciliation(principal.tenantId()));
    }

    @GetMapping("/matches")
    public ResponseEntity<Page<MatchResponse>> listMatches(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(reconciliationService.listMatches(principal.tenantId(), pageable));
    }

    @GetMapping("/discrepancies")
    public ResponseEntity<Page<DiscrepancyResponse>> listDiscrepancies(
        @RequestParam(required = false) DiscrepancyStatus status,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
            reconciliationService.listDiscrepancies(principal.tenantId(), status, pageable));
    }

    @PutMapping("/discrepancies/{id}/resolve")
    public ResponseEntity<DiscrepancyResponse> resolve(
        @PathVariable UUID id,
        @Valid @RequestBody ResolveDiscrepancyRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
            reconciliationService.resolveDiscrepancy(
                principal.tenantId(), id, principal.userId(), request));
    }

    @PostMapping("/matches/manual")
    public ResponseEntity<MatchResponse> createManual(
        @Valid @RequestBody ManualMatchRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            reconciliationService.createManualMatch(
                principal.tenantId(), principal.userId(), request));
    }
}