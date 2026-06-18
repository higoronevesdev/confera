package dev.confera.admin;

import dev.confera.identity.entity.Tenant;
import dev.confera.identity.repository.TenantRepository;
import dev.confera.shared.seed.DemoSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin/demo")
@Profile("demo")
@RequiredArgsConstructor
@Slf4j
public class DemoResetController {

    private static final String KEY_HEADER = "X-Demo-Reset-Key";

    @Value("${confera.demo.reset-key}")
    private String configuredResetKey;

    private final DemoSeedService demoSeedService;
    private final TenantRepository tenantRepository;

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset(
            @RequestHeader(value = KEY_HEADER, required = false) String providedKey) {

        if (providedKey == null || !providedKey.equals(configuredResetKey)) {
            log.warn("[DemoReset] Tentativa com chave inválida ou ausente.");
            return ResponseEntity.status(403).body(Map.of(
                    "status", 403,
                    "message", "Forbidden: chave de reset inválida ou ausente."
            ));
        }

        log.info("[DemoReset] Reset solicitado via endpoint — iniciando...");
        long t0 = System.currentTimeMillis();

        Optional<Tenant> existing = tenantRepository.findBySlug(DemoSeedService.DEMO_SLUG);
        existing.ifPresent(t -> demoSeedService.teardown(t.getId()));
        demoSeedService.seed(false);

        long elapsed = System.currentTimeMillis() - t0;
        log.info("[DemoReset] Concluído em {}ms.", elapsed);

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Dados demo restaurados com sucesso.",
                "elapsedMs", elapsed,
                "resetAt", Instant.now().toString()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(
            @RequestHeader(value = KEY_HEADER, required = false) String providedKey) {

        if (providedKey == null || !providedKey.equals(configuredResetKey)) {
            return ResponseEntity.status(403).body(Map.of("status", 403, "message", "Forbidden"));
        }

        boolean present = demoSeedService.isDemoDataPresent();
        return ResponseEntity.ok(Map.of(
                "demoDataPresent", present,
                "slug", DemoSeedService.DEMO_SLUG
        ));
    }
}