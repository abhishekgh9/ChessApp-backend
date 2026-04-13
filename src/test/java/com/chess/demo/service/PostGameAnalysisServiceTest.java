package com.chess.demo.service;

import com.chess.demo.common.ApiException;
import com.chess.demo.entity.Game;
import com.chess.demo.entity.GameAnalysis;
import com.chess.demo.entity.GameMove;
import com.chess.demo.entity.User;
import org.junit.jupiter.api.Test;
import com.chess.demo.repository.GameAnalysisRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostGameAnalysisServiceTest {

    private final GameAnalysisRepository gameAnalysisRepository = mock(GameAnalysisRepository.class);

    @Test
    void analyzeGameReturnsWholeGameAccuracyAndRequestedPlayersProvisionalRating() {
        User white = user(1500);
        User black = user(1600);
        Game game = new Game();
        game.setId(UUID.randomUUID());
        game.setStatus("FINISHED");
        game.setResult("WHITE_WIN");
        game.setWhitePlayer(white);
        game.setBlackPlayer(black);

        List<GameMove> moves = List.of(
                move(game, white, 1, "white", "e2", "e4", null),
                move(game, black, 2, "black", "e7", "e5", null),
                move(game, white, 3, "white", "g1", "f3", null),
                move(game, black, 4, "black", "b8", "c6", null)
        );

        when(gameAnalysisRepository.findByGame(game)).thenReturn(java.util.Optional.empty());
        when(gameAnalysisRepository.save(any(GameAnalysis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostGameAnalysisService service = new PostGameAnalysisService(new FakeChessEngineService(), gameAnalysisRepository);
        var response = service.analyzeGame(game, white, moves);

        assertEquals(88.48, response.white().accuracy());
        assertEquals(98.2, response.black().accuracy());
        assertEquals(93.34, response.overallAccuracy());
        assertEquals(1648, response.requestedPlayer().provisionalRating());
        assertEquals(148, response.requestedPlayer().ratingDelta());
        assertEquals(2, response.moves().stream().filter(move -> "white".equals(move.color())).count());
    }

    @Test
    void analyzeGameRejectsActiveGames() {
        Game game = new Game();
        game.setStatus("ACTIVE");
        User user = user(1500);
        game.setWhitePlayer(user);

        PostGameAnalysisService service = new PostGameAnalysisService(new FakeChessEngineService(), gameAnalysisRepository);

        ApiException exception = assertThrows(ApiException.class, () -> service.analyzeGame(game, user, List.of()));
        assertEquals("game_analysis_requires_finished_game", exception.getMessage());
    }

    private User user(int rating) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRating(rating);
        return user;
    }

    private GameMove move(Game game,
                          User player,
                          int number,
                          String color,
                          String from,
                          String to,
                          String promotion) {
        GameMove move = new GameMove();
        move.setGame(game);
        move.setPlayer(player);
        move.setMoveNumber(number);
        move.setMoveColor(color);
        move.setFromSquare(from);
        move.setToSquare(to);
        move.setPromotion(promotion);
        move.setSan(from + "-" + to);
        move.setFenAfter("");
        return move;
    }

    private static final class FakeChessEngineService implements ChessEngineService {

        private int analysisCalls;

        @Override
        public PositionInfo describePosition(List<String> uciMoves) {
            return new PositionInfo("startpos", Set.of());
        }

        @Override
        public PositionInfo describeFen(String fen) {
            return new PositionInfo(fen, Set.of());
        }

        @Override
        public String findBestMove(List<String> uciMoves, int level) {
            return null;
        }

        @Override
        public AnalysisInfo analyzePosition(List<String> uciMoves) {
            return analyzeFen("startpos");
        }

        @Override
        public AnalysisInfo analyzeFen(String fen) {
            return switch (analysisCalls++) {
                case 0 -> new AnalysisInfo("e2e4", 0.0, List.of("e2e4"));
                case 1 -> new AnalysisInfo("c7c5", 0.4, List.of("c7c5"));
                case 2 -> new AnalysisInfo("e7e5", 0.35, List.of("e7e5"));
                case 3 -> new AnalysisInfo("d2d4", 0.02, List.of("d2d4"));
                default -> new AnalysisInfo("b8c6", 0.05, List.of("b8c6"));
            };
        }
    }
}
