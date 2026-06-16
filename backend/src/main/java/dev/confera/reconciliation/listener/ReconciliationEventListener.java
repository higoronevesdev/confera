package dev.confera.reconciliation.listener;

import dev.confera.config.RabbitConfig;
import dev.confera.ingestion.dto.FileImportCompletedEvent;
import dev.confera.reconciliation.service.ReconciliationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationEventListener {

    private final ReconciliationEngine reconciliationEngine;

    @RabbitListener(queues = RabbitConfig.RECONCILIATION_QUEUE)
    public void onFileImportCompleted(FileImportCompletedEvent event) {
        log.info("[Reconciliation] Triggering for tenant {} after file import {} ({} records)",
            event.tenantId(), event.importId(), event.totalRecords());
        try {
            reconciliationEngine.runForTenant(event.tenantId());
        } catch (Exception e) {
            log.error("[Reconciliation] Failed for tenant {}: {}", event.tenantId(), e.getMessage(), e);
        }
    }
}