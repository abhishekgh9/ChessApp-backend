package com.chess.demo.dto.puzzle;

import java.util.List;

public record PuzzlePageResponse(
        List<PuzzleSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
