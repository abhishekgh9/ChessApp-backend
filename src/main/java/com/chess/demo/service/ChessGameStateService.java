package com.chess.demo.service;

import com.chess.demo.common.ApiException;
import com.chess.demo.entity.GameMove;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveConversionException;
import com.github.bhlangonijr.chesslib.move.MoveList;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChessGameStateService {

    public AppliedMove applyMove(List<GameMove> existingMoves, String from, String to, String promotion) {
        Reconstruction reconstruction = reconstruct(existingMoves);
        Board board = reconstruction.board();
        MoveList moveList = reconstruction.moveList();

        Move legalMove = resolveLegalMove(board, from, to, promotion);
        if (!board.doMove(legalMove, true)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "illegal_move");
        }
        moveList.add(legalMove);

        String san = lastSan(moveList);
        String fen = board.getFen();
        PositionSnapshot snapshot = toSnapshot(board);

        return new AppliedMove(
                legalMove.toString(),
                san,
                fen,
                toMoveText(moveList),
                snapshot.turnColor(),
                snapshot.halfmoveClock(),
                snapshot.fullmoveNumber(),
                snapshot.checkmate(),
                snapshot.stalemate(),
                snapshot.draw(),
                snapshot.drawReason()
        );
    }

    public PositionSnapshot snapshot(List<GameMove> moves) {
        Reconstruction reconstruction = reconstruct(moves);
        return toSnapshot(reconstruction.board());
    }

    private Reconstruction reconstruct(List<GameMove> moves) {
        Board board = new Board();
        MoveList moveList = new MoveList(board.getFen());
        for (GameMove move : moves) {
            Move historicalMove = moveFromSquares(
                    move.getFromSquare(),
                    move.getToSquare(),
                    move.getPromotion(),
                    board.getSideToMove()
            );
            if (!board.doMove(historicalMove, true)) {
                throw new ApiException(HttpStatus.CONFLICT, "invalid_game_history");
            }
            moveList.add(historicalMove);
        }
        return new Reconstruction(board, moveList);
    }

    private Move resolveLegalMove(Board board, String from, String to, String promotion) {
        Square fromSquare = squareFrom(from);
        Square toSquare = squareFrom(to);
        Piece promotionPiece = promotionPiece(board.getSideToMove(), promotion);

        boolean promotionMoveAvailable = false;
        for (Move legalMove : board.legalMoves()) {
            if (!legalMove.getFrom().equals(fromSquare) || !legalMove.getTo().equals(toSquare)) {
                continue;
            }

            boolean legalIsPromotion = !Piece.NONE.equals(legalMove.getPromotion());
            if (legalIsPromotion) {
                promotionMoveAvailable = true;
            }

            if (promotionPiece == null && !legalIsPromotion) {
                return legalMove;
            }
            if (promotionPiece != null && legalMove.getPromotion().equals(promotionPiece)) {
                return legalMove;
            }
        }

        if (promotionPiece == null && promotionMoveAvailable) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "promotion_required");
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "illegal_move");
    }

    private Move moveFromSquares(String from, String to, String promotion, Side sideToMove) {
        return new Move(squareFrom(from), squareFrom(to), promotionPiece(sideToMove, promotion));
    }

    private Square squareFrom(String value) {
        try {
            return Square.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_square");
        }
    }

    private Piece promotionPiece(Side sideToMove, String promotion) {
        if (promotion == null || promotion.isBlank()) {
            return Piece.NONE;
        }
        String fenSymbol = sideToMove == Side.WHITE ? promotion.toUpperCase() : promotion.toLowerCase();
        Piece piece = Piece.fromFenSymbol(fenSymbol);
        if (piece == Piece.NONE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_promotion");
        }
        return piece;
    }

    private String toMoveText(MoveList moveList) {
        try {
            return moveList.toSanWithMoveNumbers().trim();
        } catch (MoveConversionException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "san_encoding_failed");
        }
    }

    private String lastSan(MoveList moveList) {
        try {
            String[] sanMoves = moveList.toSanArray();
            return sanMoves.length == 0 ? "" : sanMoves[sanMoves.length - 1];
        } catch (MoveConversionException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "san_encoding_failed");
        }
    }

    private PositionSnapshot toSnapshot(Board board) {
        String fen = board.getFen();
        String[] fenParts = fen.split(" ");
        int halfmoveClock = fenParts.length > 4 ? parseCounter(fenParts[4], "fen_counter_invalid") : 0;
        int fullmoveNumber = fenParts.length > 5 ? parseCounter(fenParts[5], "fen_counter_invalid") : 1;

        boolean checkmate = board.isMated();
        boolean stalemate = board.isStaleMate();
        boolean draw = !checkmate && !stalemate && board.isDraw();

        String drawReason = null;
        if (draw) {
            if (board.isRepetition()) {
                drawReason = "THREEFOLD_REPETITION";
            } else if (board.isInsufficientMaterial()) {
                drawReason = "INSUFFICIENT_MATERIAL";
            } else if (halfmoveClock >= 100) {
                drawReason = "FIFTY_MOVE_RULE";
            } else {
                drawReason = "DRAW";
            }
        }

        return new PositionSnapshot(
                fen,
                board.getSideToMove().value().toLowerCase(),
                halfmoveClock,
                fullmoveNumber,
                checkmate,
                stalemate,
                draw,
                drawReason
        );
    }

    private int parseCounter(String value, String errorCode) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.CONFLICT, errorCode);
        }
    }

    private record Reconstruction(Board board, MoveList moveList) {
    }

    public record PositionSnapshot(
            String fen,
            String turnColor,
            int halfmoveClock,
            int fullmoveNumber,
            boolean checkmate,
            boolean stalemate,
            boolean draw,
            String drawReason
    ) {
    }

    public record AppliedMove(
            String uci,
            String san,
            String fen,
            String moveText,
            String turnColor,
            int halfmoveClock,
            int fullmoveNumber,
            boolean checkmate,
            boolean stalemate,
            boolean draw,
            String drawReason
    ) {
    }
}
