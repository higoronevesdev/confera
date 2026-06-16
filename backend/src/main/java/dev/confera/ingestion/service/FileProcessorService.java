package dev.confera.ingestion.service;

import dev.confera.config.RabbitConfig;
import dev.confera.ingestion.dto.FileImportEvent;
import dev.confera.ingestion.entity.*;
import dev.confera.ingestion.parser.*;
import dev.confera.ingestion.repository.BankStatementRepository;
import dev.confera.ingestion.repository.FileImportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileProcessorService {

    private final FileImportRepository fileImportRepository;
    private final BankStatementRepository bankStatementRepository;
    private final StorageService storageService;
    private final OFXParser ofxParser;
    private final CNAB240Parser cnab240Parser;
    private final CNAB400Parser cnab400Parser;
    private final ApplicationContext ctx;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onFileImportCreated(FileImportEvent event) {
        log.info("Processing file import: importId={} fileType={}", event.importId(), event.fileType());
        try {
            ctx.getBean(FileProcessorService.class).processFile(event);
        } catch (Exception e) {
            log.error("Error processing file import {}: {}", event.importId(), e.getMessage(), e);
            ctx.getBean(FileProcessorService.class).markFailed(event.importId(), e.getMessage());
        }
    }

    @Transactional
    public void processFile(FileImportEvent event) throws Exception {
        FileImport fileImport = fileImportRepository.findById(event.importId())
            .orElseThrow(() -> new IllegalStateException("FileImport not found: " + event.importId()));

        fileImport.setStatus(ImportStatus.PROCESSING);
        fileImportRepository.save(fileImport);

        List<BankStatementData> records;
        try (InputStream input = storageService.downloadFile(event.storageKey())) {
            records = switch (event.fileType()) {
                case OFX      -> ofxParser.parse(input);
                case CNAB_240 -> cnab240Parser.parse(input);
                case CNAB_400 -> cnab400Parser.parse(input);
                case CSV_ADQUIRENTE -> {
                    log.warn("CSV_ADQUIRENTE parser not yet implemented for import {}", event.importId());
                    yield List.of();
                }
            };
        }

        List<BankStatement> statements = records.stream()
            .map(data -> BankStatement.builder()
                .tenantId(event.tenantId())
                .fileImportId(event.importId())
                .bankCode(data.bankCode())
                .agency(data.agency())
                .accountNumber(data.accountNumber())
                .transactionDate(data.transactionDate())
                .valueDate(data.valueDate())
                .amountCents(data.amountCents())
                .description(data.description())
                .documentNumber(data.documentNumber())
                .build())
            .toList();

        bankStatementRepository.saveAll(statements);

        fileImport.setStatus(ImportStatus.DONE);
        fileImport.setTotalRecords(records.size());
        fileImport.setProcessedRecords(statements.size());
        fileImportRepository.save(fileImport);

        log.info("File import {} completed: {} records processed", event.importId(), statements.size());
    }

    @Transactional
    public void markFailed(UUID importId, String errorMessage) {
        fileImportRepository.findById(importId).ifPresent(fi -> {
            fi.setStatus(ImportStatus.ERROR);
            fi.setErrorMessage(errorMessage != null
                ? errorMessage.substring(0, Math.min(errorMessage.length(), 2000))
                : "Unknown error");
            fileImportRepository.save(fi);
        });
    }
}