package com.chess.demo.service;

import com.github.bhlangonijr.chesslib.Side;

final class AnalysisScoring {

    private AnalysisScoring() {
    }

    static String classifyMove(ChessEngineService.AnalysisInfo beforeMove,
                               ChessEngineService.AnalysisInfo afterMove,
                               String playedMove,
                               Side moverSide) {
        if (playedMove != null
                && beforeMove.bestMove() != null
                && playedMove.equalsIgnoreCase(beforeMove.bestMove())) {
            return "best";
        }
        if (beforeMove.evaluation() == null || afterMove.evaluation() == null) {
            return "good";
        }

        double loss = moveLoss(beforeMove.evaluation(), afterMove.evaluation(), moverSide);
        if (loss <= 0.20) {
            return "excellent";
        }
        if (loss <= 0.50) {
            return "good";
        }
        if (loss <= 1.00) {
            return "inaccuracy";
        }
        if (loss <= 2.00) {
            return "mistake";
        }
        return "blunder";
    }

    static double moveLoss(Double beforeEvaluation, Double afterEvaluation, Side moverSide) {
        if (beforeEvaluation == null || afterEvaluation == null) {
            return 0.0;
        }
        double loss = moverSide == Side.WHITE
                ? beforeEvaluation - afterEvaluation
                : afterEvaluation - beforeEvaluation;
        return Math.max(0.0, loss);
    }

    static double moveAccuracy(String classification, double moveLoss) {
        if ("best".equals(classification) || moveLoss <= 0.0) {
            return 100.0;
        }

        // Approximate the "school-grade" style that Chess.com describes publicly for CAPS:
        // tiny losses remain in the high 90s, while bigger mistakes fall off non-linearly.
        double score = 100.0 * Math.exp(-0.65 * Math.pow(moveLoss, 0.82));
        return round(Math.max(0.0, Math.min(100.0, score)));
    }

    static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
