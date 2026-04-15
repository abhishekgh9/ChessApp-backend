package com.chess.demo.dto.puzzle;

import java.util.List;
import java.util.UUID;

public record PuzzleSummaryResponse(
        UUID id,
        String title,
        String description,
        String fen,
        String difficulty,
        String primaryTheme,
        List<String> tags,
        Integer maxWrongAttempts,
        Integer totalSolutionSteps
) {
}
