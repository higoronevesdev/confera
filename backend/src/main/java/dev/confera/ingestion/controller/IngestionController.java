package dev.confera.ingestion.controller;

import dev.confera.identity.security.UserPrincipal;
import dev.confera.ingestion.dto.FileImportResponse;
import dev.confera.ingestion.service.FileImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/ingestion/imports")
@RequiredArgsConstructor
public class IngestionController {

    private final FileImportService fileImportService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileImportResponse> upload(
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal UserPrincipal principal
    ) throws IOException {
        FileImportResponse response = fileImportService.importFile(
            principal.tenantId(), principal.userId(), file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<FileImportResponse>> list(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(fileImportService.listImports(principal.tenantId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileImportResponse> getById(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(fileImportService.getImport(principal.tenantId(), id));
    }
}