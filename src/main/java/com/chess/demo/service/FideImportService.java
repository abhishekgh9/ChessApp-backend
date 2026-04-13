package com.chess.demo.service;

import com.chess.demo.common.ApiException;
import com.chess.demo.entity.FidePlayer;
import com.chess.demo.repository.FidePlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FideImportService {
    private static final String OFFICIAL_COMBINED_XML_ZIP_URL = "https://ratings.fide.com/download/players_list_xml.zip";
    private static final int BATCH_SIZE = 500;
    private static final Logger log = LoggerFactory.getLogger(FideImportService.class);

    private final FidePlayerRepository fidePlayerRepository;
    private final FidePlayerXmlParser fidePlayerXmlParser;
    private final ResourceLoader resourceLoader;
    private final TransactionTemplate requiresNewTransactionTemplate;
    private final String combinedSource;
    private final String standardSource;
    private final String rapidSource;
    private final String blitzSource;

    public FideImportService(FidePlayerRepository fidePlayerRepository,
                             FidePlayerXmlParser fidePlayerXmlParser,
                             ResourceLoader resourceLoader,
                             PlatformTransactionManager transactionManager,
                             @Value("${fide.import.combined-source:}") String combinedSource,
                             @Value("${fide.import.standard-source:}") String standardSource,
                             @Value("${fide.import.rapid-source:}") String rapidSource,
                             @Value("${fide.import.blitz-source:}") String blitzSource) {
        this.fidePlayerRepository = fidePlayerRepository;
        this.fidePlayerXmlParser = fidePlayerXmlParser;
        this.resourceLoader = resourceLoader;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        this.combinedSource = combinedSource;
        this.standardSource = standardSource;
        this.rapidSource = rapidSource;
        this.blitzSource = blitzSource;
    }

    public FideImportSummary sync(String timeControl, String overrideSource) {
        String normalizedTimeControl = normalizeTimeControl(timeControl);
        if ("all".equals(normalizedTimeControl)) {
            return syncCombined(overrideSource);
        }

        String source = overrideSource == null || overrideSource.isBlank()
                ? configuredSource(normalizedTimeControl)
                : overrideSource.trim();

        if (source == null || source.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "fide_source_not_configured");
        }

        AtomicInteger importedCount = new AtomicInteger();
        AtomicInteger parsedCount = new AtomicInteger();
        List<FidePlayer> batch = new ArrayList<>(BATCH_SIZE);

        parseSource(source, record -> {
            parsedCount.incrementAndGet();
            batch.add(toPlayer(record, normalizedTimeControl));
            flushBatchIfNeeded(batch, importedCount, normalizedTimeControl);
        });

        flushBatch(batch, importedCount, normalizedTimeControl);
        if (parsedCount.get() == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "fide_import_no_records_found");
        }

        return new FideImportSummary(normalizedTimeControl, source, importedCount.get());
    }

    public FideImportSummary syncCombined(String overrideSource) {
        String source = overrideSource == null || overrideSource.isBlank()
                ? resolvedCombinedSource()
                : overrideSource.trim();

        if (source == null || source.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "fide_source_not_configured");
        }

        AtomicInteger importedCount = new AtomicInteger();
        AtomicInteger parsedCount = new AtomicInteger();
        List<FidePlayer> batch = new ArrayList<>(BATCH_SIZE);

        parseSource(source, record -> {
            parsedCount.incrementAndGet();
            batch.add(toPlayer(record));
            flushBatchIfNeeded(batch, importedCount, "combined");
        });

        flushBatch(batch, importedCount, "combined");
        if (parsedCount.get() == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "fide_import_no_records_found");
        }

        return new FideImportSummary("all", source, importedCount.get());
    }

    private void parseSource(String source, java.util.function.Consumer<FidePlayerXmlParser.FidePlayerXmlRecord> consumer) {
        Resource resource = resourceLoader.getResource(source);
        if (!resource.exists()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "fide_source_not_found");
        }

        try (InputStream rawInput = resource.getInputStream();
             BufferedInputStream bufferedInput = new BufferedInputStream(rawInput)) {
            bufferedInput.mark(4);
            byte[] header = bufferedInput.readNBytes(4);
            bufferedInput.reset();

            if (header.length >= 2 && header[0] == 'P' && header[1] == 'K') {
                parseZip(bufferedInput, consumer);
                return;
            }
            fidePlayerXmlParser.parse(bufferedInput, consumer);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "fide_source_read_failed");
        }
    }

    private void parseZip(InputStream inputStream, java.util.function.Consumer<FidePlayerXmlParser.FidePlayerXmlRecord> consumer) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase(Locale.ROOT).endsWith(".xml")) {
                    fidePlayerXmlParser.parse(zipInputStream, consumer);
                    return;
                }
            }
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "fide_zip_missing_xml");
    }

    private void flushBatchIfNeeded(List<FidePlayer> batch, AtomicInteger importedCount, String importType) {
        if (batch.size() >= BATCH_SIZE) {
            flushBatch(batch, importedCount, importType);
        }
    }

    private void flushBatch(List<FidePlayer> batch, AtomicInteger importedCount, String importType) {
        if (batch.isEmpty()) {
            return;
        }
        List<FidePlayer> playersToPersist = List.copyOf(batch);
        requiresNewTransactionTemplate.executeWithoutResult(status -> fidePlayerRepository.saveAll(playersToPersist));
        importedCount.addAndGet(playersToPersist.size());
        if (importedCount.get() % 5000 == 0 || playersToPersist.size() < BATCH_SIZE) {
            log.info("Imported {} FIDE {} players so far", importedCount.get(), importType);
        }
        batch.clear();
    }

    private FidePlayer toPlayer(FidePlayerXmlParser.FidePlayerXmlRecord record, String timeControl) {
        FidePlayer player = fidePlayerRepository.findById(record.fideId())
                .orElseGet(FidePlayer::new);
        populateIdentity(player, record);
        updateSingleRatingFields(player, timeControl, record);
        return player;
    }

    private FidePlayer toPlayer(FidePlayerXmlParser.FidePlayerXmlRecord record) {
        FidePlayer player = fidePlayerRepository.findById(record.fideId())
                .orElseGet(FidePlayer::new);
        populateIdentity(player, record);
        updateCombinedRatingFields(player, record);
        return player;
    }

    private void populateIdentity(FidePlayer player, FidePlayerXmlParser.FidePlayerXmlRecord record) {
        player.setFideId(record.fideId());
        player.setName(record.name());
        player.setTitle(clean(record.title()));
        player.setFederation(clean(record.federation()));
        player.setSex(clean(record.sex()));
        player.setBirthYear(record.birthYear());
    }

    private void updateSingleRatingFields(FidePlayer player,
                                          String timeControl,
                                          FidePlayerXmlParser.FidePlayerXmlRecord record) {
        switch (timeControl) {
            case "rapid" -> {
                player.setRapidRating(record.rapidRating());
                player.setRapidGames(record.rapidGames());
                player.setRapidK(record.rapidK());
                player.setRapidInactive(record.rapidInactive());
            }
            case "blitz" -> {
                player.setBlitzRating(record.blitzRating());
                player.setBlitzGames(record.blitzGames());
                player.setBlitzK(record.blitzK());
                player.setBlitzInactive(record.blitzInactive());
            }
            default -> {
                player.setStandardRating(record.standardRating());
                player.setStandardGames(record.standardGames());
                player.setStandardK(record.standardK());
                player.setStandardInactive(record.standardInactive());
            }
        }
    }

    private void updateCombinedRatingFields(FidePlayer player, FidePlayerXmlParser.FidePlayerXmlRecord record) {
        player.setStandardRating(record.standardRating());
        player.setRapidRating(record.rapidRating());
        player.setBlitzRating(record.blitzRating());
        player.setStandardGames(record.standardGames());
        player.setRapidGames(record.rapidGames());
        player.setBlitzGames(record.blitzGames());
        player.setStandardK(record.standardK());
        player.setRapidK(record.rapidK());
        player.setBlitzK(record.blitzK());
        player.setStandardInactive(record.standardInactive());
        player.setRapidInactive(record.rapidInactive());
        player.setBlitzInactive(record.blitzInactive());
    }

    private String configuredSource(String timeControl) {
        return switch (timeControl) {
            case "rapid" -> rapidSource;
            case "blitz" -> blitzSource;
            default -> standardSource;
        };
    }

    public boolean hasExplicitSource(String timeControl) {
        String normalizedTimeControl = normalizeTimeControl(timeControl);
        String source = "all".equals(normalizedTimeControl)
                ? combinedSource
                : configuredSource(normalizedTimeControl);
        return source != null && !source.isBlank();
    }

    public boolean hasImportedPlayers() {
        return fidePlayerRepository.count() > 0;
    }

    public Instant latestImportedAt() {
        return fidePlayerRepository.findTopByOrderByUpdatedAtDesc()
                .map(FidePlayer::getUpdatedAt)
                .orElse(null);
    }

    private String resolvedCombinedSource() {
        if (combinedSource != null && !combinedSource.isBlank()) {
            return combinedSource;
        }
        return OFFICIAL_COMBINED_XML_ZIP_URL;
    }

    private String normalizeTimeControl(String timeControl) {
        if (timeControl == null || timeControl.isBlank()) {
            return "standard";
        }
        return switch (timeControl.trim().toLowerCase(Locale.ROOT)) {
            case "standard", "classical" -> "standard";
            case "rapid" -> "rapid";
            case "blitz" -> "blitz";
            case "all", "combined" -> "all";
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_time_control");
        };
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record FideImportSummary(
            String timeControl,
            String source,
            int importedCount
    ) {
    }
}
