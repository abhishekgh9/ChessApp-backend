package com.chess.demo.service;

import com.chess.demo.dto.leaderboard.FideLeaderboardEntryResponse;
import com.chess.demo.dto.leaderboard.FideLeaderboardResponse;
import com.chess.demo.entity.FidePlayer;
import com.chess.demo.repository.FidePlayerRepository;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FideLeaderboardService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final FidePlayerRepository fidePlayerRepository;

    public FideLeaderboardService(FidePlayerRepository fidePlayerRepository) {
        this.fidePlayerRepository = fidePlayerRepository;
    }

    @Transactional(readOnly = true)
    public FideLeaderboardResponse getLeaderboard(String query,
                                                  String timeControl,
                                                  String country,
                                                  String gender,
                                                  String division,
                                                  Integer page,
                                                  Integer size,
                                                  boolean activeOnly) {
        String normalizedTimeControl = normalizeTimeControl(timeControl);
        String normalizedGender = normalizeGender(gender);
        String normalizedDivision = normalizeDivision(division);
        int normalizedPage = page == null || page < 0 ? 0 : page;
        int normalizedSize = size == null || size <= 0
                ? DEFAULT_PAGE_SIZE
                : Math.min(size, MAX_PAGE_SIZE);

        Specification<FidePlayer> specification = buildSpecification(
                query,
                normalizedTimeControl,
                country,
                normalizedGender,
                normalizedDivision,
                activeOnly
        );

        Pageable pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(Sort.Order.desc(ratingField(normalizedTimeControl)), Sort.Order.asc("name"))
        );

        Page<FidePlayer> resultPage = fidePlayerRepository.findAll(specification, pageable);
        int rankOffset = normalizedPage * normalizedSize;

        AtomicInteger rankCounter = new AtomicInteger(rankOffset + 1);
        List<FideLeaderboardEntryResponse> entries = resultPage.getContent().stream()
                .map(player -> toResponse(player, normalizedTimeControl, rankCounter.getAndIncrement()))
                .toList();

        Instant lastSyncedAt = fidePlayerRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "updatedAt")))
                .stream()
                .findFirst()
                .map(FidePlayer::getUpdatedAt)
                .orElse(null);

        return new FideLeaderboardResponse(
                normalizedTimeControl,
                normalizedGender,
                normalizedDivision,
                normalizeCountry(country),
                normalizeQuery(query),
                normalizedPage,
                normalizedSize,
                resultPage.getTotalElements(),
                lastSyncedAt,
                entries
        );
    }

    private Specification<FidePlayer> buildSpecification(String query,
                                                         String timeControl,
                                                         String country,
                                                         String gender,
                                                         String division,
                                                         boolean activeOnly) {
        String ratingField = ratingField(timeControl);
        String inactivityField = inactivityField(timeControl);

        Specification<FidePlayer> specification = Specification.where(hasRating(ratingField));
        specification = andIfPresent(specification, nameContains(query));
        specification = andIfPresent(specification, countryEquals(country));
        specification = andIfPresent(specification, genderMatches(gender));
        specification = andIfPresent(specification, divisionMatches(division));
        specification = andIfPresent(specification, activeOnly ? activeOnly(inactivityField) : null);
        return specification;
    }

    private Specification<FidePlayer> andIfPresent(Specification<FidePlayer> base,
                                                   Specification<FidePlayer> candidate) {
        return candidate == null ? base : base.and(candidate);
    }

    private Specification<FidePlayer> hasRating(String ratingField) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get(ratingField));
    }

    private Specification<FidePlayer> nameContains(String query) {
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery == null) {
            return null;
        }
        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + normalizedQuery.toLowerCase(Locale.ROOT) + "%");
    }

    private Specification<FidePlayer> countryEquals(String country) {
        String normalizedCountry = normalizeCountry(country);
        if (normalizedCountry == null) {
            return null;
        }
        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.equal(criteriaBuilder.upper(root.get("federation")), normalizedCountry);
    }

    private Specification<FidePlayer> genderMatches(String gender) {
        return switch (gender) {
            case "male" -> (root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(root.get("sex")),
                            criteriaBuilder.notEqual(criteriaBuilder.upper(root.get("sex")), "F")
                    );
            case "female" -> (root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.equal(criteriaBuilder.upper(root.get("sex")), "F");
            default -> null;
        };
    }

    private Specification<FidePlayer> divisionMatches(String division) {
        int currentYear = Year.now().getValue();
        return switch (division) {
            case "junior" -> (root, criteriaQuery, criteriaBuilder) -> {
                Expression<Integer> age = criteriaBuilder.diff(criteriaBuilder.literal(currentYear), root.get("birthYear").as(Integer.class));
                return criteriaBuilder.and(
                        criteriaBuilder.isNotNull(root.get("birthYear")),
                        criteriaBuilder.le(age, 20)
                );
            };
            case "senior" -> (root, criteriaQuery, criteriaBuilder) -> {
                Expression<Integer> age = criteriaBuilder.diff(criteriaBuilder.literal(currentYear), root.get("birthYear").as(Integer.class));
                return criteriaBuilder.and(
                        criteriaBuilder.isNotNull(root.get("birthYear")),
                        criteriaBuilder.ge(age, 50)
                );
            };
            default -> null;
        };
    }

    private Specification<FidePlayer> activeOnly(String inactivityField) {
        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.isFalse(root.get(inactivityField));
    }

    private FideLeaderboardEntryResponse toResponse(FidePlayer player, String timeControl, int rank) {
        return new FideLeaderboardEntryResponse(
                rank,
                player.getFideId(),
                player.getName(),
                player.getTitle(),
                player.getFederation(),
                player.getSex(),
                player.getBirthYear(),
                timeControl,
                ratingFor(player, timeControl),
                gamesFor(player, timeControl),
                inactiveFor(player, timeControl)
        );
    }

    private Integer ratingFor(FidePlayer player, String timeControl) {
        return switch (timeControl) {
            case "rapid" -> player.getRapidRating();
            case "blitz" -> player.getBlitzRating();
            default -> player.getStandardRating();
        };
    }

    private Integer gamesFor(FidePlayer player, String timeControl) {
        return switch (timeControl) {
            case "rapid" -> player.getRapidGames();
            case "blitz" -> player.getBlitzGames();
            default -> player.getStandardGames();
        };
    }

    private Boolean inactiveFor(FidePlayer player, String timeControl) {
        return switch (timeControl) {
            case "rapid" -> player.getRapidInactive();
            case "blitz" -> player.getBlitzInactive();
            default -> player.getStandardInactive();
        };
    }

    private String ratingField(String timeControl) {
        return switch (timeControl) {
            case "rapid" -> "rapidRating";
            case "blitz" -> "blitzRating";
            default -> "standardRating";
        };
    }

    private String inactivityField(String timeControl) {
        return switch (timeControl) {
            case "rapid" -> "rapidInactive";
            case "blitz" -> "blitzInactive";
            default -> "standardInactive";
        };
    }

    private String normalizeTimeControl(String timeControl) {
        if (timeControl == null || timeControl.isBlank()) {
            return "standard";
        }
        return switch (timeControl.trim().toLowerCase(Locale.ROOT)) {
            case "standard", "classical" -> "standard";
            case "rapid" -> "rapid";
            case "blitz" -> "blitz";
            default -> "standard";
        };
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return "open";
        }
        return switch (gender.trim().toLowerCase(Locale.ROOT)) {
            case "female", "women", "girl", "girls" -> "female";
            case "male", "men", "boy", "boys" -> "male";
            default -> "open";
        };
    }

    private String normalizeDivision(String division) {
        if (division == null || division.isBlank()) {
            return "open";
        }
        return switch (division.trim().toLowerCase(Locale.ROOT)) {
            case "junior", "juniors", "u20" -> "junior";
            case "senior", "seniors", "50+", "65+" -> "senior";
            default -> "open";
        };
    }

    private String normalizeCountry(String country) {
        if (country == null || country.isBlank()) {
            return null;
        }
        return country.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim();
    }
}
