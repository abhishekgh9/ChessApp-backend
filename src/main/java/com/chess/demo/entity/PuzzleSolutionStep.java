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
@Table(name = "puzzle_solution_steps")
@Getter
@Setter
public class PuzzleSolutionStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "puzzle_id", nullable = false)
    private Puzzle puzzle;

    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;

    @Column(name = "move_uci", nullable = false, length = 5)
    private String moveUci;

    @Column(name = "move_san", length = 30)
    private String moveSan;

    @Column(name = "side_to_move", nullable = false, length = 5)
    private String sideToMove;

    @Column(name = "is_opponent_move", nullable = false)
    private Boolean opponentMove = false;
}
