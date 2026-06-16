package dev.confera.reconciliation.service;

import dev.confera.ingestion.entity.BankStatement;
import dev.confera.ingestion.entity.Receivable;
import dev.confera.ingestion.entity.ReconciliationStatus;
import dev.confera.ingestion.repository.BankStatementRepository;
import dev.confera.ingestion.repository.ReceivableRepository;
import dev.confera.reconciliation.dto.*;
import dev.confera.reconciliation.entity.*;
import dev.confera.reconciliation.exception.AlreadyMatchedException;
import dev.confera.reconciliation.exception.DiscrepancyNotFoundException;
import dev.confera.reconciliation.repository.ReconciliationDiscrepancyRepository;
import dev.confera.reconciliation.repository.ReconciliationMatchRepository;
import dev.confera.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final ReconciliationEngine reconciliationEngine;
    private final ReconciliationMatchRepository matchRepo;
    private final ReconciliationDiscrepancyRepository discrepancyRepo;
    private final BankStatementRepository bankStatementRepo;
    private final ReceivableRepository receivableRepo;

    public ReconciliationSummaryResponse triggerReconciliation(UUID tenantId) {
        long startMs = System.currentTimeMillis();
        ReconciliationEngine.RunResult result = reconciliationEngine.runForTenant(tenantId);
        long elapsedMs = System.currentTimeMillis() - startMs;

        return new ReconciliationSummaryResponse(
            tenantId,
            result.totalBankStatements(),
            result.totalReceivables(),
            result.exactMatches(),
            result.fuzzyMatches(),
            result.unmatched(),
            result.discrepanciesCreated(),
            elapsedMs
        );
    }

    @Transactional(readOnly = true)
    public Page<MatchResponse> listMatches(UUID tenantId, Pageable pageable) {
        return matchRepo.findByTenantId(tenantId, pageable).map(MatchResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<DiscrepancyResponse> listDiscrepancies(UUID tenantId, DiscrepancyStatus status, Pageable pageable) {
        Page<ReconciliationDiscrepancy> page = status != null
            ? discrepancyRepo.findByTenantIdAndStatus(tenantId, status, pageable)
            : discrepancyRepo.findByTenantId(tenantId, pageable);
        return page.map(DiscrepancyResponse::from);
    }

    @Transactional
    public DiscrepancyResponse resolveDiscrepancy(UUID tenantId, UUID discrepancyId,
                                                   UUID userId, ResolveDiscrepancyRequest request) {
        if (request.status() != DiscrepancyStatus.RESOLVED && request.status() != DiscrepancyStatus.IGNORED) {
            throw new BadRequestException("Status must be RESOLVED or IGNORED");
        }

        ReconciliationDiscrepancy discrepancy = discrepancyRepo
            .findByIdAndTenantId(discrepancyId, tenantId)
            .orElseThrow(() -> new DiscrepancyNotFoundException(discrepancyId));

        discrepancy.setStatus(request.status());
        discrepancy.setResolvedAt(LocalDateTime.now());
        discrepancy.setResolvedByUserId(userId);

        return DiscrepancyResponse.from(discrepancyRepo.save(discrepancy));
    }

    @Transactional
    public MatchResponse createManualMatch(UUID tenantId, UUID userId, ManualMatchRequest request) {
        BankStatement bs = bankStatementRepo.findById(request.bankStatementId())
            .filter(b -> b.getTenantId().equals(tenantId))
            .orElseThrow(() -> new dev.confera.shared.exception.ApiException(
                "BankStatement not found: " + request.bankStatementId(), HttpStatus.NOT_FOUND) {});

        Receivable rv = receivableRepo.findById(request.receivableId())
            .filter(r -> r.getTenantId().equals(tenantId))
            .orElseThrow(() -> new dev.confera.shared.exception.ApiException(
                "Receivable not found: " + request.receivableId(), HttpStatus.NOT_FOUND) {});

        if (bs.getReconciliationStatus() == ReconciliationStatus.MATCHED) {
            throw new AlreadyMatchedException("BankStatement", bs.getId());
        }
        if (rv.getReconciliationStatus() == ReconciliationStatus.MATCHED) {
            throw new AlreadyMatchedException("Receivable", rv.getId());
        }

        long diff = bs.getAmountCents() - rv.getNetAmountCents();

        ReconciliationMatch match = matchRepo.save(ReconciliationMatch.builder()
            .tenantId(tenantId)
            .bankStatementId(bs.getId())
            .receivableId(rv.getId())
            .matchType(MatchType.MANUAL)
            .matchedByUserId(userId)
            .differenceCents(diff)
            .notes(request.notes())
            .build());

        bs.setReconciliationStatus(ReconciliationStatus.MATCHED);
        rv.setReconciliationStatus(ReconciliationStatus.MATCHED);
        bankStatementRepo.save(bs);
        receivableRepo.save(rv);

        return MatchResponse.from(match);
    }
}