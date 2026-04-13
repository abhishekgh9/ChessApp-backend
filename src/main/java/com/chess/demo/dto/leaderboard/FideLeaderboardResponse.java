package com.chess.demo.dto.leaderboard;

import java.time.Instant;
import java.util.List;

public record FideLeaderboardResponse(
        String timeControl,
        String gender,
        String division,
        String country,
        String query,
        Integer page,
        Integer size,
        Long totalEntries,
        Instant lastSyncedAt,
        List<FideLeaderboardEntryResponse> entries
) {
}
