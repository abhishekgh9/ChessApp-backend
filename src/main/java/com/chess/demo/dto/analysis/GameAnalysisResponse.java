package com.chess.demo.dto.analysis;

import java.util.List;
import java.util.UUID;

public record GameAnalysisResponse(
        UUID gameId,
        String status,
        String result,
        Double overallAccuracy,
        GameAnalysisPlayerResponse white,
        GameAnalysisPlayerResponse black,
        GameAnalysisPlayerResponse requestedPlayer,
        List<GameAnalysisMoveResponse> moves
) {
}
