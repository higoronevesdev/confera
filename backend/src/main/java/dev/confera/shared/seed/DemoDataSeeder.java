package dev.confera.shared.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

    private final DemoSeedService demoSeedService;

    @Override
    public void run(ApplicationArguments args) {
        boolean forceReset = args.containsOption("demo.force-reset");
        demoSeedService.seed(forceReset);
    }
}