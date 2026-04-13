package com.chess.demo.dto.analysis;

import java.util.UUID;

public record GameAnalysisPlayerResponse(
        UUID playerId,
        String color,
        Double accuracy,
        Integer currentRating,
        Integer provisionalRating,
        Integer ratingDelta,
        Integer movesAnalyzed
) {
}
