package com.chess.demo.dto.leaderboard;

public record FideLeaderboardEntryResponse(
        Integer rank,
        Integer fideId,
        String name,
        String title,
        String country,
        String gender,
        Integer birthYear,
        String timeControl,
        Integer rating,
        Integer gamesPlayed,
        Boolean inactive
) {
}
