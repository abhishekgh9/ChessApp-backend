package com.chess.demo.service;

import com.chess.demo.common.ApiException;
import com.chess.demo.dto.analysis.GameAnalysisMoveResponse;
import com.chess.demo.dto.analysis.GameAnalysisPlayerResponse;
import com.chess.demo.dto.analysis.GameAnalysisResponse;
import com.chess.demo.entity.Game;
import com.chess.demo.entity.GameAnalysis;
import com.chess.demo.entity.GameAnalysisMove;
import com.chess.demo.entity.GameMove;
import com.chess.demo.entity.User;
import com.chess.demo.repository.GameAnalysisRepository;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PostGameAnalysisService {

    private final ChessEngineService chessEngineService;
    private final GameAnalysisRepository gameAnalysisRepository;

    public PostGameAnalysisService(ChessEngineService chessEngineService,
                                   GameAnalysisRepository gameAnalysisRepository) {
        this.chessEngineService = chessEngineService;
        this.gameAnalysisRepository = gameAnalysisRepository;
    }

    @Transactional
    public GameAnalysisResponse analyzeGame(Game game, User requestedUser, List<GameMove> moves) {
        if (!"FINISHED".equalsIgnoreCase(game.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "game_analysis_requires_finished_game");
        }

        GameAnalysis cached = gameAnalysisRepository.findByGame(game)
                .filter(existing -> isFresh(existing, game, moves))
                .orElse(null);
        if (cached != null) {
            return toResponse(cached, requestedUser);
        }

        Board board = new Board();
        ChessEngineService.AnalysisInfo currentAnalysis = chessEngineService.analyzeFen(board.getFen());
        List<GameAnalysisMoveResponse> analyzedMoves = new ArrayList<>();
        PlayerAccumulator white = new PlayerAccumulator(resolvePlayerId(game.getWhitePlayer()), "white", resolveDisplayedRating(game, Side.WHITE));
        PlayerAccumulator black = new PlayerAccumulator(resolvePlayerId(game.getBlackPlayer()), "black", resolveDisplayedRating(game, Side.BLACK));

        for (GameMove gameMove : moves) {
            Side moverSide = board.getSideToMove();
            String uciMove = toUciMove(gameMove);
            applyUciMove(board, uciMove);

            ChessEngineService.AnalysisInfo nextAnalysis = chessEngineService.analyzeFen(board.getFen());
            String classification = AnalysisScoring.classifyMove(currentAnalysis, nextAnalysis, uciMove, moverSide);
            double moveLoss = AnalysisScoring.moveLoss(currentAnalysis.evaluation(), nextAnalysis.evaluation(), moverSide);
            double moveAccuracy = AnalysisScoring.moveAccuracy(classification, moveLoss);

            PlayerAccumulator accumulator = moverSide == Side.WHITE ? white : black;
            accumulator.recordMove(moveAccuracy);

            analyzedMoves.add(new GameAnalysisMoveResponse(
                    gameMove.getMoveNumber(),
                    gameMove.getMoveColor(),
                    gameMove.getPlayer() == null ? null : gameMove.getPlayer().getId(),
                    uciMove,
                    currentAnalysis.bestMove(),
                    nextAnalysis.evaluation(),
                    classification,
                    moveAccuracy
            ));

            currentAnalysis = nextAnalysis;
        }

        GameAnalysisPlayerResponse whiteResponse = white.toResponse(resolveOpponentRating(game, Side.WHITE), resultScore(game.getResult(), Side.WHITE));
        GameAnalysisPlayerResponse blackResponse = black.toResponse(resolveOpponentRating(game, Side.BLACK), resultScore(game.getResult(), Side.BLACK));
        GameAnalysisPlayerResponse requestedPlayerResponse = requestedPlayer(game, requestedUser, whiteResponse, blackResponse);

        GameAnalysisResponse response = new GameAnalysisResponse(
                game.getId(),
                game.getStatus(),
                game.getResult(),
                overallAccuracy(whiteResponse, blackResponse),
                whiteResponse,
                blackResponse,
                requestedPlayerResponse,
                analyzedMoves
        );
        saveAnalysis(game, response, requestedPlayerResponse, moves);
        return response;
    }

    private boolean isFresh(GameAnalysis analysis, Game game, List<GameMove> moves) {
        return analysis.getSourceMoveCount() != null
                && analysis.getSourceMoveCount() == moves.size()
                && analysis.getSourceGameUpdatedAt() != null
                && game.getUpdatedAt() != null
                && analysis.getSourceGameUpdatedAt().equals(game.getUpdatedAt());
    }

    private void saveAnalysis(Game game,
                              GameAnalysisResponse response,
                              GameAnalysisPlayerResponse requestedPlayer,
                              List<GameMove> moves) {
        GameAnalysis analysis = gameAnalysisRepository.findByGame(game).orElseGet(GameAnalysis::new);
        analysis.setGame(game);
        analysis.setGameStatus(response.status());
        analysis.setGameResult(response.result());
        analysis.setOverallAccuracy(response.overallAccuracy());
        analysis.setWhitePlayer(game.getWhitePlayer());
        analysis.setWhiteAccuracy(response.white() == null ? null : response.white().accuracy());
        analysis.setWhiteCurrentRating(response.white() == null ? null : response.white().currentRating());
        analysis.setWhiteProvisionalRating(response.white() == null ? null : response.white().provisionalRating());
        analysis.setWhiteRatingDelta(response.white() == null ? null : response.white().ratingDelta());
        analysis.setWhiteMovesAnalyzed(response.white() == null ? null : response.white().movesAnalyzed());
        analysis.setBlackPlayer(game.getBlackPlayer());
        analysis.setBlackAccuracy(response.black() == null ? null : response.black().accuracy());
        analysis.setBlackCurrentRating(response.black() == null ? null : response.black().currentRating());
        analysis.setBlackProvisionalRating(response.black() == null ? null : response.black().provisionalRating());
        analysis.setBlackRatingDelta(response.black() == null ? null : response.black().ratingDelta());
        analysis.setBlackMovesAnalyzed(response.black() == null ? null : response.black().movesAnalyzed());
        analysis.setSourceGameUpdatedAt(game.getUpdatedAt());
        analysis.setSourceMoveCount(moves.size());

        analysis.getMoves().clear();
        for (GameAnalysisMoveResponse moveResponse : response.moves()) {
            GameAnalysisMove analysisMove = new GameAnalysisMove();
            analysisMove.setAnalysis(analysis);
            analysisMove.setPlayer(resolveMovePlayer(game, moveResponse.playerId()));
            analysisMove.setMoveNumber(moveResponse.moveNumber());
            analysisMove.setMoveColor(moveResponse.color());
            analysisMove.setUciMove(moveResponse.uciMove());
            analysisMove.setBestMove(moveResponse.bestMove());
            analysisMove.setEvaluationAfter(moveResponse.evaluationAfter());
            analysisMove.setClassification(moveResponse.classification());
            analysisMove.setAccuracy(moveResponse.accuracy());
            analysis.getMoves().add(analysisMove);
        }
        gameAnalysisRepository.save(analysis);
    }

    private User resolveMovePlayer(Game game, UUID playerId) {
        if (playerId == null) {
            return null;
        }
        if (game.getWhitePlayer() != null && playerId.equals(game.getWhitePlayer().getId())) {
            return game.getWhitePlayer();
        }
        if (game.getBlackPlayer() != null && playerId.equals(game.getBlackPlayer().getId())) {
            return game.getBlackPlayer();
        }
        return null;
    }

    private GameAnalysisResponse toResponse(GameAnalysis analysis, User requestedUser) {
        GameAnalysisPlayerResponse white = analysis.getWhitePlayer() == null ? null : new GameAnalysisPlayerResponse(
                analysis.getWhitePlayer().getId(),
                "white",
                analysis.getWhiteAccuracy(),
                analysis.getWhiteCurrentRating(),
                analysis.getWhiteProvisionalRating(),
                analysis.getWhiteRatingDelta(),
                defaultInteger(analysis.getWhiteMovesAnalyzed())
        );
        GameAnalysisPlayerResponse black = analysis.getBlackPlayer() == null ? null : new GameAnalysisPlayerResponse(
                analysis.getBlackPlayer().getId(),
                "black",
                analysis.getBlackAccuracy(),
                analysis.getBlackCurrentRating(),
                analysis.getBlackProvisionalRating(),
                analysis.getBlackRatingDelta(),
                defaultInteger(analysis.getBlackMovesAnalyzed())
        );
        GameAnalysisPlayerResponse requested = requestedPlayer(analysis.getGame(), requestedUser, white, black);

        List<GameAnalysisMoveResponse> moves = analysis.getMoves().stream()
                .map(move -> new GameAnalysisMoveResponse(
                        move.getMoveNumber(),
                        move.getMoveColor(),
                        move.getPlayer() == null ? null : move.getPlayer().getId(),
                        move.getUciMove(),
                        move.getBestMove(),
                        move.getEvaluationAfter(),
                        move.getClassification(),
                        move.getAccuracy()
                ))
                .toList();

        return new GameAnalysisResponse(
                analysis.getGame().getId(),
                analysis.getGameStatus(),
                analysis.getGameResult(),
                analysis.getOverallAccuracy(),
                white,
                black,
                requested,
                moves
        );
    }

    private int defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private UUID resolvePlayerId(User player) {
        return player == null ? null : player.getId();
    }

    private int resolveDisplayedRating(Game game, Side side) {
        User player = side == Side.WHITE ? game.getWhitePlayer() : game.getBlackPlayer();
        if (player != null && player.getRating() != null) {
            return player.getRating();
        }
        if (Boolean.TRUE.equals(game.getBotGame())) {
            return botRating(game.getBotLevel());
        }
        return 1500;
    }

    private int resolveOpponentRating(Game game, Side side) {
        return resolveDisplayedRating(game, side == Side.WHITE ? Side.BLACK : Side.WHITE);
    }

    private double resultScore(String result, Side side) {
        if (result == null || result.isBlank()) {
            return 0.5;
        }
        return switch (result) {
            case "WHITE_WIN" -> side == Side.WHITE ? 1.0 : 0.0;
            case "BLACK_WIN" -> side == Side.BLACK ? 1.0 : 0.0;
            case "DRAW" -> 0.5;
            default -> 0.5;
        };
    }

    private int botRating(Integer level) {
        int bounded = Math.max(1, Math.min(10, level == null ? 1 : level));
        return switch (bounded) {
            case 1 -> 800;
            case 2 -> 1000;
            case 3 -> 1200;
            case 4 -> 1400;
            case 5 -> 1600;
            case 6 -> 1750;
            case 7 -> 1900;
            case 8 -> 2050;
            case 9 -> 2200;
            default -> 2350;
        };
    }

    private void applyUciMove(Board board, String uciMove) {
        for (Move legalMove : board.legalMoves()) {
            if (legalMove.toString().equalsIgnoreCase(uciMove)) {
                board.doMove(legalMove);
                return;
            }
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "game_contains_illegal_moves");
    }

    private String toUciMove(GameMove move) {
        String promotion = move.getPromotion() == null || move.getPromotion().isBlank() ? "" : move.getPromotion().toLowerCase();
        return move.getFromSquare().toLowerCase() + move.getToSquare().toLowerCase() + promotion;
    }

    private Double overallAccuracy(GameAnalysisPlayerResponse white, GameAnalysisPlayerResponse black) {
        double totalAccuracy = 0.0;
        int totalMoves = 0;
        if (white.accuracy() != null && white.movesAnalyzed() > 0) {
            totalAccuracy += white.accuracy() * white.movesAnalyzed();
            totalMoves += white.movesAnalyzed();
        }
        if (black.accuracy() != null && black.movesAnalyzed() > 0) {
            totalAccuracy += black.accuracy() * black.movesAnalyzed();
            totalMoves += black.movesAnalyzed();
        }
        if (totalMoves == 0) {
            return null;
        }
        return AnalysisScoring.round(totalAccuracy / totalMoves);
    }

    private GameAnalysisPlayerResponse requestedPlayer(Game game,
                                                       User requestedUser,
                                                       GameAnalysisPlayerResponse white,
                                                       GameAnalysisPlayerResponse black) {
        if (game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(requestedUser.getId())) {
            return white;
        }
        if (game.getBlackPlayer() != null && game.getBlackPlayer().getId().equals(requestedUser.getId())) {
            return black;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "not_game_participant");
    }

    private static final class PlayerAccumulator {
        private final UUID playerId;
        private final String color;
        private final int currentRating;
        private double accuracyTotal;
        private int moveCount;

        private PlayerAccumulator(UUID playerId, String color, int currentRating) {
            this.playerId = playerId;
            this.color = color;
            this.currentRating = currentRating;
        }

        private void recordMove(double accuracy) {
            accuracyTotal += accuracy;
            moveCount++;
        }

        private GameAnalysisPlayerResponse toResponse(int opponentRating, double resultScore) {
            Double accuracy = moveCount == 0 ? null : AnalysisScoring.round(accuracyTotal / moveCount);
            int provisionalRating = moveCount == 0
                    ? currentRating
                    : calculateProvisionalRating(opponentRating, accuracy, resultScore);
            return new GameAnalysisPlayerResponse(
                    playerId,
                    color,
                    accuracy,
                    currentRating,
                    provisionalRating,
                    provisionalRating - currentRating,
                    moveCount
            );
        }

        private int calculateProvisionalRating(int opponentRating, Double accuracy, double resultScore) {
            double safeAccuracy = accuracy == null ? 75.0 : accuracy;

            // Treat this as a one-game performance estimate rather than a real rating change.
            // Result drives the estimate, accuracy nudges it, and short games are damped.
            double resultPerformance = opponentRating + ((resultScore - 0.5) * 500.0);
            double accuracyAdjustment = (safeAccuracy - 75.0) * 7.0;
            double rawPerformance = resultPerformance + accuracyAdjustment;

            double confidence = Math.min(1.0, Math.sqrt(moveCount / 18.0));
            double estimate = currentRating + ((rawPerformance - currentRating) * confidence);
            return Math.max(100, Math.min(3000, (int) Math.round(estimate)));
        }
    }
}
