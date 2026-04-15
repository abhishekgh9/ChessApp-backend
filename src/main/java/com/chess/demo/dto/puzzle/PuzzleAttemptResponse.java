package com.chess.demo.dto.puzzle;

import java.util.UUID;

public record PuzzleAttemptResponse(
        UUID attemptId,
        UUID puzzleId,
        String status,
        boolean correct,
        boolean completed,
        boolean failed,
        int attemptCount,
        int remainingAttempts,
        int solvedSteps,
        int totalSteps,
        Integer awardedScore,
        Integer currentStreak,
        String fen,
        String message
) {
}
