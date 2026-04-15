package com.chess.demo.service;

import com.chess.demo.common.ApiException;
import com.chess.demo.dto.puzzle.PuzzleAttemptRequest;
import com.chess.demo.dto.puzzle.PuzzleAttemptResponse;
import com.chess.demo.dto.puzzle.PuzzleDailyResponse;
import com.chess.demo.dto.puzzle.PuzzlePageResponse;
import com.chess.demo.dto.puzzle.PuzzleProgressResponse;
import com.chess.demo.dto.puzzle.PuzzleSummaryResponse;
import com.chess.demo.entity.Puzzle;
import com.chess.demo.entity.PuzzleAttempt;
import com.chess.demo.entity.PuzzleSolutionStep;
import com.chess.demo.entity.PuzzleTag;
import com.chess.demo.entity.User;
import com.chess.demo.repository.PuzzleAttemptRepository;
import com.chess.demo.repository.PuzzleRepository;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PuzzleService {

    private static final List<String> TERMINAL_STATUSES = List.of("COMPLETED", "FAILED");
    private static final List<String> SUCCESS_STATUSES = List.of("CORRECT", "COMPLETED");
    private static final List<String> FAILURE_STATUSES = List.of("INCORRECT", "FAILED");

    private final PuzzleRepository puzzleRepository;
    private final PuzzleAttemptRepository puzzleAttemptRepository;

    public PuzzleService(PuzzleRepository puzzleRepository,
                         PuzzleAttemptRepository puzzleAttemptRepository) {
        this.puzzleRepository = puzzleRepository;
        this.puzzleAttemptRepository = puzzleAttemptRepository;
    }

    @Transactional(readOnly = true)
    public PuzzlePageResponse listPuzzles(String difficulty, String theme, int page, int size) {
        Page<Puzzle> puzzles = puzzleRepository.searchActive(
                normalizeFilter(difficulty),
                normalizeFilter(theme),
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt", "id"))
        );
        return new PuzzlePageResponse(
                puzzles.map(this::toSummary).getContent(),
                puzzles.getNumber(),
                puzzles.getSize(),
                puzzles.getTotalElements(),
                puzzles.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PuzzleDailyResponse getDailyPuzzle() {
        List<Puzzle> puzzles = puzzleRepository.findByActiveTrueOrderByCreatedAtAscIdAsc();
        if (puzzles.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "puzzle_not_found");
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int index = Math.floorMod((int) today.toEpochDay(), puzzles.size());
        PuzzleSummaryResponse summary = toSummary(puzzles.get(index));
        return new PuzzleDailyResponse(
                summary.id(),
                summary.title(),
                summary.description(),
                summary.fen(),
                summary.difficulty(),
                summary.primaryTheme(),
                summary.tags(),
                summary.maxWrongAttempts(),
                summary.totalSolutionSteps(),
                today
        );
    }

    @Transactional(readOnly = true)
    public PuzzleSummaryResponse getPuzzle(UUID id) {
        return toSummary(getActivePuzzle(id));
    }

    @Transactional
    public PuzzleAttemptResponse submitAttempt(UUID id, User user, PuzzleAttemptRequest request) {
        Puzzle puzzle = getActivePuzzle(id);
        List<PuzzleAttempt> previousAttempts = puzzleAttemptRepository.findByPuzzleAndUserOrderByCreatedAtAsc(puzzle, user);
        PuzzleProgressState progressState = buildProgressState(puzzle, previousAttempts);
        if (progressState.locked()) {
            throw new ApiException(HttpStatus.CONFLICT, "puzzle_attempt_locked");
        }

        String normalizedMove = normalizeMove(request.move());
        Board board = prepareBoard(puzzle, progressState);
        Move legalMove = resolveLegalMove(board, normalizedMove);
        if (legalMove == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "illegal_move");
        }

        PuzzleAttempt attempt = new PuzzleAttempt();
        attempt.setPuzzle(puzzle);
        attempt.setUser(user);
        attempt.setAttemptNumber(previousAttempts.size() + 1);
        attempt.setSolutionStepNumber(progressState.expectedStep().getStepNumber());
        attempt.setSubmittedMoveUci(normalizedMove);
        attempt.setTimeSpentSeconds(defaultIfNull(request.timeSpentSeconds()));
        attempt.setHintsUsed(defaultIfNull(request.hintsUsed()));

        String status;
        String responseFen = board.getFen();
        if (normalizedMove.equalsIgnoreCase(progressState.expectedStep().getMoveUci())) {
            board.doMove(legalMove);
            responseFen = applyOpponentReplies(board, progressState.orderedSteps(), progressState.expectedGlobalIndex() + 1);
            status = hasRemainingPlayerStep(progressState.orderedSteps(), progressState.expectedGlobalIndex() + 1)
                    ? "CORRECT"
                    : "COMPLETED";
        } else {
            int wrongAttemptCount = countWrongAttempts(previousAttempts) + 1;
            status = wrongAttemptCount >= puzzle.getMaxWrongAttempts() ? "FAILED" : "INCORRECT";
        }

        attempt.setStatus(status);
        PuzzleAttempt savedAttempt = puzzleAttemptRepository.save(attempt);
        int totalSteps = countPlayerSolutionSteps(progressState.orderedSteps());
        int solvedSteps = countSuccessfulPlayerSteps(previousAttempts) + (SUCCESS_STATUSES.contains(status) ? 1 : 0);
        int currentStreak = calculateStreaks(user).currentStreak();
        int wrongAttemptsUsed = countWrongAttempts(previousAttempts)
                + ("INCORRECT".equals(status) || "FAILED".equals(status) ? 1 : 0);
        return new PuzzleAttemptResponse(
                savedAttempt.getId(),
                puzzle.getId(),
                status.toLowerCase(Locale.ROOT),
                SUCCESS_STATUSES.contains(status),
                "COMPLETED".equals(status),
                "FAILED".equals(status),
                savedAttempt.getAttemptNumber(),
                Math.max(0, puzzle.getMaxWrongAttempts() - wrongAttemptsUsed),
                solvedSteps,
                totalSteps,
                "COMPLETED".equals(status) ? calculateScore(puzzle, previousAttempts, savedAttempt) : 0,
                currentStreak,
                responseFen,
                buildAttemptMessage(status)
        );
    }

    @Transactional(readOnly = true)
    public PuzzleProgressResponse getProgress(User user) {
        long attemptedCount = puzzleAttemptRepository.countDistinctAttemptedPuzzles(user);
        long solvedCount = puzzleAttemptRepository.countDistinctSolvedPuzzles(user);
        StreakSummary streakSummary = calculateStreaks(user);
        double successRate = attemptedCount == 0
                ? 0.0
                : BigDecimal.valueOf((solvedCount * 100.0) / attemptedCount)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
        return new PuzzleProgressResponse(
                attemptedCount,
                solvedCount,
                successRate,
                streakSummary.currentStreak(),
                streakSummary.bestStreak()
        );
    }

    private Puzzle getActivePuzzle(UUID id) {
        return puzzleRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "puzzle_not_found"));
    }

    private PuzzleSummaryResponse toSummary(Puzzle puzzle) {
        List<String> tagNames = puzzle.getTags().stream()
                .map(PuzzleTag::getSlug)
                .sorted()
                .toList();
        return new PuzzleSummaryResponse(
                puzzle.getId(),
                puzzle.getTitle(),
                puzzle.getDescription(),
                puzzle.getFen(),
                puzzle.getDifficulty().toLowerCase(Locale.ROOT),
                puzzle.getPrimaryTheme().toLowerCase(Locale.ROOT),
                tagNames,
                puzzle.getMaxWrongAttempts(),
                countPlayerSolutionSteps(orderedSteps(puzzle.getSolutionSteps()))
        );
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeMove(String move) {
        String normalized = move == null ? "" : move.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[a-h][1-8][a-h][1-8][qrbn]?$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_move_format");
        }
        return normalized;
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    private PuzzleProgressState buildProgressState(Puzzle puzzle, List<PuzzleAttempt> previousAttempts) {
        List<PuzzleSolutionStep> orderedSteps = orderedSteps(puzzle.getSolutionSteps());
        if (orderedSteps.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "puzzle_solution_missing");
        }

        boolean failed = previousAttempts.stream().anyMatch(attempt -> "FAILED".equals(attempt.getStatus()));
        int solvedPlayerSteps = countSuccessfulPlayerSteps(previousAttempts);
        List<Integer> playerStepIndexes = new ArrayList<>();
        for (int index = 0; index < orderedSteps.size(); index++) {
            if (!Boolean.TRUE.equals(orderedSteps.get(index).getOpponentMove())) {
                playerStepIndexes.add(index);
            }
        }

        boolean locked = failed || solvedPlayerSteps >= playerStepIndexes.size();
        int expectedGlobalIndex = locked ? -1 : playerStepIndexes.get(solvedPlayerSteps);
        PuzzleSolutionStep expectedStep = locked ? null : orderedSteps.get(expectedGlobalIndex);
        return new PuzzleProgressState(locked, orderedSteps, expectedStep, expectedGlobalIndex);
    }

    private Board prepareBoard(Puzzle puzzle, PuzzleProgressState progressState) {
        try {
            Board board = new Board();
            board.loadFromFen(puzzle.getFen());
            for (int index = 0; index < progressState.expectedGlobalIndex(); index++) {
                applyUciMove(board, progressState.orderedSteps().get(index).getMoveUci());
            }
            return board;
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "puzzle_invalid_state");
        }
    }

    private Move resolveLegalMove(Board board, String uciMove) {
        for (Move move : board.legalMoves()) {
            if (move.toString().equalsIgnoreCase(uciMove)) {
                return move;
            }
        }
        return null;
    }

    private void applyUciMove(Board board, String uciMove) {
        Move move = resolveLegalMove(board, uciMove);
        if (move == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "puzzle_invalid_state");
        }
        board.doMove(move);
    }

    private String applyOpponentReplies(Board board, List<PuzzleSolutionStep> orderedSteps, int startIndex) {
        for (int index = startIndex; index < orderedSteps.size(); index++) {
            PuzzleSolutionStep step = orderedSteps.get(index);
            if (!Boolean.TRUE.equals(step.getOpponentMove())) {
                break;
            }
            applyUciMove(board, step.getMoveUci());
        }
        return board.getFen();
    }

    private boolean hasRemainingPlayerStep(List<PuzzleSolutionStep> orderedSteps, int startIndex) {
        for (int index = startIndex; index < orderedSteps.size(); index++) {
            if (!Boolean.TRUE.equals(orderedSteps.get(index).getOpponentMove())) {
                return true;
            }
        }
        return false;
    }

    private int countWrongAttempts(List<PuzzleAttempt> attempts) {
        int count = 0;
        for (PuzzleAttempt attempt : attempts) {
            if (FAILURE_STATUSES.contains(attempt.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private int countSuccessfulPlayerSteps(List<PuzzleAttempt> attempts) {
        int count = 0;
        for (PuzzleAttempt attempt : attempts) {
            if (SUCCESS_STATUSES.contains(attempt.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private int countPlayerSolutionSteps(List<PuzzleSolutionStep> orderedSteps) {
        int count = 0;
        for (PuzzleSolutionStep step : orderedSteps) {
            if (!Boolean.TRUE.equals(step.getOpponentMove())) {
                count++;
            }
        }
        return count;
    }

    private int calculateScore(Puzzle puzzle, List<PuzzleAttempt> previousAttempts, PuzzleAttempt attempt) {
        int baseScore = switch (puzzle.getDifficulty().toUpperCase(Locale.ROOT)) {
            case "HARD" -> 150;
            case "MEDIUM" -> 100;
            default -> 60;
        };
        boolean firstTrySolve = previousAttempts.isEmpty();
        int score = baseScore;
        if (firstTrySolve) {
            score += 40;
        }
        score -= attempt.getHintsUsed() * 10;
        return Math.max(10, score);
    }

    private String buildAttemptMessage(String status) {
        return switch (status) {
            case "CORRECT" -> "Correct move. Continue with the next solution step.";
            case "COMPLETED" -> "Puzzle solved.";
            case "FAILED" -> "Incorrect move. Puzzle failed.";
            default -> "Incorrect move. Try again.";
        };
    }

    private StreakSummary calculateStreaks(User user) {
        List<PuzzleAttempt> attempts = puzzleAttemptRepository.findByUserAndStatusInOrderByCreatedAtAsc(user, TERMINAL_STATUSES);
        int current = 0;
        int best = 0;
        for (PuzzleAttempt attempt : attempts) {
            if ("COMPLETED".equals(attempt.getStatus())) {
                current++;
                best = Math.max(best, current);
            } else {
                current = 0;
            }
        }
        return new StreakSummary(current, best);
    }

    private List<PuzzleSolutionStep> orderedSteps(List<PuzzleSolutionStep> steps) {
        return steps.stream()
                .sorted(Comparator.comparing(PuzzleSolutionStep::getStepNumber))
                .toList();
    }

    private record PuzzleProgressState(
            boolean locked,
            List<PuzzleSolutionStep> orderedSteps,
            PuzzleSolutionStep expectedStep,
            int expectedGlobalIndex
    ) {
    }

    private record StreakSummary(int currentStreak, int bestStreak) {
    }
}
