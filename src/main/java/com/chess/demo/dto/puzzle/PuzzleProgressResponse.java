package com.chess.demo.dto.puzzle;

public record PuzzleProgressResponse(
        long attemptedCount,
        long solvedCount,
        double successRate,
        int currentStreak,
        int bestStreak
) {
}
