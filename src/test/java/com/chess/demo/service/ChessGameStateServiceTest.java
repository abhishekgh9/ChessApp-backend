package com.chess.demo.service;

import com.chess.demo.common.ApiException;
import com.chess.demo.entity.GameMove;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChessGameStateServiceTest {

    private final ChessGameStateService service = new ChessGameStateService();

    @Test
    void applyMoveGeneratesCanonicalFenAndSan() {
        ChessGameStateService.AppliedMove move = service.applyMove(List.of(), "e2", "e4", null);

        assertEquals("e2e4", move.uci());
        assertEquals("e4", move.san());
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", move.fen());
        assertEquals("1. e4", move.moveText());
        assertEquals("black", move.turnColor());
        assertEquals(0, move.halfmoveClock());
        assertEquals(1, move.fullmoveNumber());
    }

    @Test
    void applyMoveRejectsIllegalMove() {
        ApiException exception = assertThrows(ApiException.class,
                () -> service.applyMove(List.of(), "e2", "e5", null));

        assertEquals("illegal_move", exception.getMessage());
    }

    @Test
    void snapshotDetectsCheckmate() {
        List<GameMove> moves = new ArrayList<>();

        append(moves, service.applyMove(moves, "f2", "f3", null));
        append(moves, service.applyMove(moves, "e7", "e5", null));
        append(moves, service.applyMove(moves, "g2", "g4", null));
        append(moves, service.applyMove(moves, "d8", "h4", null));

        ChessGameStateService.PositionSnapshot snapshot = service.snapshot(moves);

        assertTrue(snapshot.checkmate());
        assertEquals("white", snapshot.turnColor());
    }

    private void append(List<GameMove> moves, ChessGameStateService.AppliedMove appliedMove) {
        GameMove move = new GameMove();
        move.setFromSquare(appliedMove.uci().substring(0, 2));
        move.setToSquare(appliedMove.uci().substring(2, 4));
        move.setPromotion(appliedMove.uci().length() > 4 ? appliedMove.uci().substring(4) : null);
        moves.add(move);
    }
}
