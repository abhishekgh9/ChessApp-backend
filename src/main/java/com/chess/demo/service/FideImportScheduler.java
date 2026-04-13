package com.chess.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

@Component
public class FideImportScheduler {

    private static final Logger log = LoggerFactory.getLogger(FideImportScheduler.class);

    private final FideImportService fideImportService;
    private final boolean refreshEnabled;

    public FideImportScheduler(FideImportService fideImportService,
                               @Value("${fide.import.refresh-enabled:true}") boolean refreshEnabled) {
        this.fideImportService = fideImportService;
        this.refreshEnabled = refreshEnabled;
    }

    @Scheduled(cron = "${fide.import.refresh-cron:0 0 6 10 * *}", zone = "UTC")
    public void refreshMonthly() {
        if (!refreshEnabled) {
            return;
        }

        Instant latestImportedAt = fideImportService.latestImportedAt();
        if (latestImportedAt != null) {
            YearMonth importedMonth = YearMonth.from(latestImportedAt.atZone(ZoneOffset.UTC));
            YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
            if (!importedMonth.isBefore(currentMonth)) {
                log.info("Skipping scheduled FIDE import because current month data already exists");
                return;
            }
        }

        try {
            FideImportService.FideImportSummary summary = fideImportService.sync("all", null);
            log.info("Scheduled FIDE import completed with {} players from {}", summary.importedCount(), summary.source());
        } catch (RuntimeException exception) {
            log.warn("Scheduled FIDE import failed: {}", exception.getMessage());
        }
    }
}
