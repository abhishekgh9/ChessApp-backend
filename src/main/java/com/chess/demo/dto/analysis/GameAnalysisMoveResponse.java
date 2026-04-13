package com.chess.demo.dto.analysis;

import java.util.UUID;

public record GameAnalysisMoveResponse(
        Integer moveNumber,
        String color,
        UUID playerId,
        String uciMove,
        String bestMove,
        Double evaluationAfter,
        String classification,
        Double accuracy
) {
}
