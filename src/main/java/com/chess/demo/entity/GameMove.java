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
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_moves")
@Getter
@Setter
public class GameMove {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private User player;

    @Column(name = "move_number", nullable = false)
    private Integer moveNumber;

    @Column(name = "move_color", nullable = false, length = 5)
    private String moveColor;

    @Column(name = "from_square", nullable = false, length = 5)
    private String fromSquare;

    @Column(name = "to_square", nullable = false, length = 5)
    private String toSquare;

    @Column(name = "promotion", length = 5)
    private String promotion;

    @Column(name = "san", nullable = false, length = 30)
    private String san;

    @Column(name = "fen_after", nullable = false, length = 255)
    private String fenAfter;

    @Column(name = "halfmove_clock")
    private Integer halfmoveClock;

    @Column(name = "fullmove_number")
    private Integer fullmoveNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
