package com.chess.demo.controller;

import com.chess.demo.dto.puzzle.PuzzleAttemptRequest;
import com.chess.demo.dto.puzzle.PuzzleAttemptResponse;
import com.chess.demo.dto.puzzle.PuzzleDailyResponse;
import com.chess.demo.dto.puzzle.PuzzlePageResponse;
import com.chess.demo.dto.puzzle.PuzzleProgressResponse;
import com.chess.demo.dto.puzzle.PuzzleSummaryResponse;
import com.chess.demo.service.CurrentUserService;
import com.chess.demo.service.PuzzleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/puzzles")
@Validated
public class PuzzleController {

    private final PuzzleService puzzleService;
    private final CurrentUserService currentUserService;

    public PuzzleController(PuzzleService puzzleService, CurrentUserService currentUserService) {
        this.puzzleService = puzzleService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<PuzzlePageResponse> listPuzzles(
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String theme,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(puzzleService.listPuzzles(difficulty, theme, page, size));
    }

    @GetMapping("/daily")
    public ResponseEntity<PuzzleDailyResponse> getDailyPuzzle() {
        return ResponseEntity.ok(puzzleService.getDailyPuzzle());
    }

    @GetMapping("/me/progress")
    public ResponseEntity<PuzzleProgressResponse> getMyProgress(Principal principal) {
        return ResponseEntity.ok(puzzleService.getProgress(currentUserService.requireUser(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PuzzleSummaryResponse> getPuzzle(@PathVariable UUID id) {
        return ResponseEntity.ok(puzzleService.getPuzzle(id));
    }

    @PostMapping("/{id}/attempts")
    public ResponseEntity<PuzzleAttemptResponse> submitAttempt(@PathVariable UUID id,
                                                               Principal principal,
                                                               @Valid @RequestBody PuzzleAttemptRequest request) {
        return ResponseEntity.ok(puzzleService.submitAttempt(id, currentUserService.requireUser(principal), request));
    }
}
