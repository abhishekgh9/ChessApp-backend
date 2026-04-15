package com.chess.demo.dto.puzzle;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PuzzleAttemptRequest(
        @NotBlank
        @Pattern(regexp = "^[a-h][1-8][a-h][1-8][qrbnQRBN]?$", message = "move must be valid UCI notation")
        String move,
        @Min(value = 0, message = "timeSpentSeconds must be 0 or greater")
        Integer timeSpentSeconds,
        @Min(value = 0, message = "hintsUsed must be 0 or greater")
        Integer hintsUsed
) {
}
