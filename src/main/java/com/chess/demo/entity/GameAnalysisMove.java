package com.chess.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "game_analysis_moves")
@Getter
@Setter
public class GameAnalysisMove {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private GameAnalysis analysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private User player;

    @Column(name = "move_number", nullable = false)
    private Integer moveNumber;

    @Column(name = "move_color", nullable = false, length = 5)
    private String moveColor;

    @Column(name = "uci_move", nullable = false, length = 5)
    private String uciMove;

    @Column(name = "best_move", length = 5)
    private String bestMove;

    @Column(name = "evaluation_after")
    private Double evaluationAfter;

    @Column(name = "classification", nullable = false, length = 20)
    private String classification;

    @Column(name = "accuracy", nullable = false)
    private Double accuracy;
}
