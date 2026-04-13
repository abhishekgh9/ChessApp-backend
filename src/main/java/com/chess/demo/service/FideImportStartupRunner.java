package com.chess.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class FideImportStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FideImportStartupRunner.class);

    private final FideImportService fideImportService;
    private final boolean syncOnStartup;

    public FideImportStartupRunner(FideImportService fideImportService,
                                   @Value("${fide.import.sync-on-startup:true}") boolean syncOnStartup) {
        this.fideImportService = fideImportService;
        this.syncOnStartup = syncOnStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!syncOnStartup) {
            return;
        }
        if (fideImportService.hasImportedPlayers()) {
            log.info("Skipping FIDE import at startup because data already exists");
            return;
        }
        log.info("Starting FIDE import in background at startup");
        CompletableFuture.runAsync(() -> {
            try {
                FideImportService.FideImportSummary summary = fideImportService.sync("all", null);
                log.info("Imported {} FIDE players from {}", summary.importedCount(), summary.source());
            } catch (RuntimeException exception) {
                log.warn("Skipping FIDE import at startup: {}", exception.getMessage());
            }
        });
    }
}
