package com.chess.demo.controller;

import com.chess.demo.entity.Puzzle;
import com.chess.demo.entity.PuzzleSolutionStep;
import com.chess.demo.entity.User;
import com.chess.demo.repository.PuzzleAttemptRepository;
import com.chess.demo.repository.PuzzleRepository;
import com.chess.demo.repository.UserRepository;
import com.chess.demo.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PuzzleControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PuzzleRepository puzzleRepository;

    @Autowired
    private PuzzleAttemptRepository puzzleAttemptRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Puzzle puzzle;
    private String token;

    @BeforeEach
    void setUp() {
        puzzleAttemptRepository.deleteAll();
        puzzleRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setUsername("security-user");
        user.setEmail("security-user@example.com");
        user.setPasswordHash("hashed-password");
        userRepository.save(user);
        token = jwtUtil.generateToken(user.getUsername());

        puzzle = new Puzzle();
        puzzle.setSlug("security-puzzle");
        puzzle.setTitle("Security Puzzle");
        puzzle.setDescription("Security Puzzle");
        puzzle.setFen("6k1/6pp/8/3Q4/8/8/8/6K1 w - - 0 1");
        puzzle.setDifficulty("EASY");
        puzzle.setPrimaryTheme("MATE");

        PuzzleSolutionStep step = new PuzzleSolutionStep();
        step.setPuzzle(puzzle);
        step.setStepNumber(1);
        step.setMoveUci("d5d8");
        step.setMoveSan("Qd8#");
        step.setSideToMove("white");
        step.setOpponentMove(false);
        puzzle.getSolutionSteps().add(step);
        puzzle = puzzleRepository.save(puzzle);
    }

    @Test
    void publicPuzzleListingDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/puzzles"))
                .andExpect(status().isOk());
    }

    @Test
    void progressEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/puzzles/me/progress"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void attemptEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/puzzles/{id}/attempts", puzzle.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"move":"d5d8","timeSpentSeconds":4,"hintsUsed":0}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedAttemptEndpointSucceeds() throws Exception {
        mockMvc.perform(post("/api/puzzles/{id}/attempts", puzzle.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"move":"d5d8","timeSpentSeconds":4,"hintsUsed":0}
                                """))
                .andExpect(status().isOk());
    }
}
