package com.chess.demo.service;

import com.chess.demo.common.ApiException;
import com.chess.demo.dto.puzzle.PuzzleAttemptRequest;
import com.chess.demo.entity.Puzzle;
import com.chess.demo.entity.PuzzleSolutionStep;
import com.chess.demo.entity.User;
import com.chess.demo.repository.PuzzleAttemptRepository;
import com.chess.demo.repository.PuzzleRepository;
import com.chess.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class PuzzleServiceTest {

    @Autowired
    private PuzzleService puzzleService;

    @Autowired
    private PuzzleRepository puzzleRepository;

    @Autowired
    private PuzzleAttemptRepository puzzleAttemptRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        puzzleAttemptRepository.deleteAll();
        puzzleRepository.deleteAll();
        userRepository.deleteAll();
        user = createUser("puzzle-user");
    }

    @Test
    void submitAttemptRejectsIllegalMove() {
        Puzzle puzzle = createPuzzle(
                "illegal-check",
                "Illegal Move Test",
                "6k1/6pp/8/3Q4/8/8/8/6K1 w - - 0 1",
                "easy",
                "mate",
                "d5d8"
        );

        ApiException exception = assertThrows(ApiException.class,
                () -> puzzleService.submitAttempt(puzzle.getId(), user, new PuzzleAttemptRequest("a1a2", 3, 0)));

        assertEquals("illegal_move", exception.getMessage());
    }

    @Test
    void submitAttemptMarksCorrectAndIncorrectOutcomes() {
        Puzzle puzzle = createPuzzle(
                "attempt-outcomes",
                "Attempt Outcomes",
                "6k1/6pp/8/3Q4/8/8/8/6K1 w - - 0 1",
                "easy",
                "mate",
                "d5d8"
        );

        var incorrect = puzzleService.submitAttempt(puzzle.getId(), user, new PuzzleAttemptRequest("d5d6", 4, 0));
        assertEquals("incorrect", incorrect.status());
        assertEquals(1, incorrect.attemptCount());

        var failed = puzzleService.submitAttempt(puzzle.getId(), user, new PuzzleAttemptRequest("d5d6", 6, 0));
        assertEquals("failed", failed.status());
        assertEquals(0, failed.remainingAttempts());

        User secondUser = createUser("solver-two");
        var completed = puzzleService.submitAttempt(puzzle.getId(), secondUser, new PuzzleAttemptRequest("d5d8", 2, 0));
        assertEquals("completed", completed.status());
        assertEquals(1, completed.solvedSteps());
        assertEquals(1, completed.totalSteps());
    }

    @Test
    void dailyPuzzleSelectionIsConsistentForTheSameDay() {
        createPuzzle("daily-one", "Daily One", "6k1/6pp/8/3Q4/8/8/8/6K1 w - - 0 1", "easy", "mate", "d5d8");
        createPuzzle("daily-two", "Daily Two", "6k1/6pp/8/8/8/8/8/4R1K1 w - - 0 1", "easy", "rook", "e1e8");
        createPuzzle("daily-three", "Daily Three", "6k1/6pp/8/8/8/8/4Q3/6K1 w - - 0 1", "medium", "queen", "e2e8");

        var first = puzzleService.getDailyPuzzle();
        var second = puzzleService.getDailyPuzzle();

        assertEquals(first.id(), second.id());
        assertEquals(first.dailyDate(), second.dailyDate());
    }

    @Test
    void progressAggregationTracksSolvedFailedAndStreaks() {
        Puzzle firstPuzzle = createPuzzle("progress-one", "Progress One", "6k1/6pp/8/3Q4/8/8/8/6K1 w - - 0 1", "easy", "mate", "d5d8");
        Puzzle secondPuzzle = createPuzzle("progress-two", "Progress Two", "6k1/6pp/8/8/8/8/8/4R1K1 w - - 0 1", "easy", "rook", "e1e8");

        puzzleService.submitAttempt(firstPuzzle.getId(), user, new PuzzleAttemptRequest("d5d8", 5, 0));
        puzzleService.submitAttempt(secondPuzzle.getId(), user, new PuzzleAttemptRequest("e1e7", 5, 0));
        puzzleService.submitAttempt(secondPuzzle.getId(), user, new PuzzleAttemptRequest("e1e7", 5, 0));

        var progress = puzzleService.getProgress(user);

        assertEquals(2, progress.attemptedCount());
        assertEquals(1, progress.solvedCount());
        assertEquals(50.0, progress.successRate());
        assertEquals(0, progress.currentStreak());
        assertEquals(1, progress.bestStreak());
    }

    private User createUser(String username) {
        User entity = new User();
        entity.setUsername(username);
        entity.setEmail(username + "@example.com");
        entity.setPasswordHash("hashed-password");
        return userRepository.save(entity);
    }

    private Puzzle createPuzzle(String slug,
                                String title,
                                String fen,
                                String difficulty,
                                String primaryTheme,
                                String moveUci) {
        Puzzle puzzle = new Puzzle();
        puzzle.setSlug(slug);
        puzzle.setTitle(title);
        puzzle.setDescription(title);
        puzzle.setFen(fen);
        puzzle.setDifficulty(difficulty.toUpperCase());
        puzzle.setPrimaryTheme(primaryTheme.toUpperCase());
        puzzle.setMaxWrongAttempts(2);

        PuzzleSolutionStep step = new PuzzleSolutionStep();
        step.setPuzzle(puzzle);
        step.setStepNumber(1);
        step.setMoveUci(moveUci);
        step.setMoveSan(moveUci);
        step.setSideToMove("white");
        step.setOpponentMove(false);
        puzzle.getSolutionSteps().add(step);

        return puzzleRepository.save(puzzle);
    }
}
