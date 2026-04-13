package com.chess.demo.service;

import com.chess.demo.common.ApiException;
import com.chess.demo.dto.analysis.AnalysisResponse;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AnalysisService {

    private final ChessEngineService chessEngineService;

    public AnalysisService(ChessEngineService chessEngineService) {
        this.chessEngineService = chessEngineService;
    }

    public AnalysisResponse analyzePgn(String pgn) {
        try {
            List<String> playedMoves = parsePlayedMoves(pgn);
            Board board = new Board();
            List<Double> evaluationSeries = new ArrayList<>();
            List<String> moveClassifications = new ArrayList<>();

            ChessEngineService.AnalysisInfo currentAnalysis = chessEngineService.analyzeFen(board.getFen());
            for (String playedMove : playedMoves) {
                Side moverSide = board.getSideToMove();
                applyUciMove(board, playedMove);
                ChessEngineService.AnalysisInfo nextAnalysis = chessEngineService.analyzeFen(board.getFen());
                if (nextAnalysis.evaluation() != null) {
                    evaluationSeries.add(nextAnalysis.evaluation());
                }
                moveClassifications.add(classifyMove(currentAnalysis, nextAnalysis, playedMove, moverSide));
                currentAnalysis = nextAnalysis;
            }

            return new AnalysisResponse(
                    UUID.randomUUID(),
                    "PGN",
                    currentAnalysis.bestMove(),
                    currentAnalysis.evaluation(),
                    evaluationSeries,
                    moveClassifications,
                    true
            );
        } catch (RuntimeException exception) {
            if (exception instanceof ApiException) {
                throw exception;
            }
            return invalidResponse("PGN");
        }
    }

    public AnalysisResponse analyzeFen(String fen) {
        try {
            Board board = new Board();
            board.loadFromFen(fen);
            ChessEngineService.AnalysisInfo analysis = chessEngineService.analyzeFen(board.getFen());
            List<Double> evaluationSeries = analysis.evaluation() == null
                    ? List.of()
                    : List.of(analysis.evaluation());
            return new AnalysisResponse(
                    UUID.randomUUID(),
                    "FEN",
                    analysis.bestMove(),
                    analysis.evaluation(),
                    evaluationSeries,
                    List.of(),
                    true
            );
        } catch (RuntimeException exception) {
            if (exception instanceof ApiException) {
                throw exception;
            }
            return invalidResponse("FEN");
        }
    }

    private AnalysisResponse invalidResponse(String sourceType) {
        return new AnalysisResponse(
                UUID.randomUUID(),
                sourceType,
                null,
                null,
                List.of(),
                List.of(),
                false
        );
    }

    private List<String> parsePlayedMoves(String pgn) {
        String normalized = normalizePgn(pgn);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Missing PGN moves");
        }

        if (containsCoordinateMoves(normalized)) {
            return parseCoordinateMoveList(normalized);
        }

        MoveList moves = new MoveList();
        moves.loadFromSan(normalized);

        List<String> playedMoves = new ArrayList<>();
        for (Move move : moves) {
            playedMoves.add(move.toString().toLowerCase());
        }
        return playedMoves;
    }

    private String normalizePgn(String pgn) {
        String normalized = pgn == null ? "" : pgn.trim();
        normalized = normalized.replaceAll("(?m)^\\s*\\[[^\\]]*]\\s*$", " ");
        normalized = normalized.replaceAll("\\{[^}]*}", " ");
        normalized = normalized.replaceAll(";[^\\r\\n]*", " ");
        normalized = normalized.replaceAll("\\$\\d+", " ");
        while (normalized.contains("(")) {
            String updated = normalized.replaceAll("\\([^()]*\\)", " ");
            if (updated.equals(normalized)) {
                break;
            }
            normalized = updated;
        }
        normalized = normalized.replaceAll("\\b(1-0|0-1|1/2-1/2|\\*)\\b", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    private boolean containsCoordinateMoves(String value) {
        return value.matches(".*\\b[a-h][1-8]-[a-h][1-8](=[qrbn])?\\b.*")
                || value.matches(".*\\b[a-h][1-8][a-h][1-8][qrbn]?\\b.*");
    }

    private List<String> parseCoordinateMoveList(String movesText) {
        List<String> playedMoves = new ArrayList<>();
        for (String token : movesText.split("\\s+")) {
            String cleaned = token.trim().toLowerCase();
            if (cleaned.isBlank()
                    || cleaned.endsWith(".")
                    || cleaned.matches("^\\d+\\.(\\.\\.)?$")
                    || cleaned.matches("^(1-0|0-1|1/2-1/2|\\*)$")) {
                continue;
            }

            String normalized = cleaned.replace("-", "").replace("=", "");
            if (!normalized.matches("^[a-h][1-8][a-h][1-8][qrbn]?$")) {
                throw new IllegalArgumentException("Unsupported coordinate move: " + token);
            }
            playedMoves.add(normalized);
        }
        if (playedMoves.isEmpty()) {
            throw new IllegalArgumentException("Missing coordinate moves");
        }
        return playedMoves;
    }

    private void applyUciMove(Board board, String uciMove) {
        for (Move legalMove : board.legalMoves()) {
            if (legalMove.toString().equalsIgnoreCase(uciMove)) {
                board.doMove(legalMove);
                return;
            }
        }
        throw new IllegalArgumentException("Illegal move: " + uciMove);
    }

    private String classifyMove(ChessEngineService.AnalysisInfo beforeMove,
                                ChessEngineService.AnalysisInfo afterMove,
                                String playedMove,
                                Side moverSide) {
        return AnalysisScoring.classifyMove(beforeMove, afterMove, playedMove, moverSide);
    }
}
