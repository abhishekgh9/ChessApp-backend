package com.chess.demo.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "games")
@Getter
@Setter
public class Game {

    public static final String STARTING_FEN = "rn1qkbnr/pppbpppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 1";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "white_player_id")
    private User whitePlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "black_player_id")
    private User blackPlayer;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "result", length = 20)
    private String result;

    @Column(name = "result_reason", length = 40)
    private String resultReason;

    @Column(name = "fen", nullable = false, length = 255)
    private String fen = "startpos";

    @Column(name = "pgn", nullable = false, columnDefinition = "TEXT")
    private String pgn = "";

    @Column(name = "history_json", nullable = false, columnDefinition = "TEXT")
    private String historyJson = "[]";

    @Column(name = "time_control", nullable = false, length = 15)
    private String timeControl = "10+0";

    @Column(name = "white_time_remaining", nullable = false)
    private Integer whiteTimeRemaining = 600;

    @Column(name = "black_time_remaining", nullable = false)
    private Integer blackTimeRemaining = 600;

    @Column(name = "turn_color", nullable = false, length = 5)
    private String turnColor = "white";

    @Column(name = "last_move_from", length = 5)
    private String lastMoveFrom;

    @Column(name = "last_move_to", length = 5)
    private String lastMoveTo;

    @Column(name = "last_move_san", length = 30)
    private String lastMoveSan;

    @Column(name = "rated", nullable = false)
    private Boolean rated = false;

    @Column(name = "draw_offered_by")
    private UUID drawOfferedBy;

    @Column(name = "is_bot_game", nullable = false)
    private Boolean botGame = false;

    @Column(name = "bot_level")
    private Integer botLevel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("moveNumber ASC")
    private List<GameMove> moves = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<GameChatMessage> chatMessages = new ArrayList<>();

    @OneToOne(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private GameAnalysis analysis;

    @PrePersist
    void applyDefaults() {
        if (fen == null || fen.isBlank()) {
            fen = "startpos";
        }
        if (pgn == null) {
            pgn = "";
        }
        if (historyJson == null || historyJson.isBlank()) {
            historyJson = "[]";
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
        if (timeControl == null || timeControl.isBlank()) {
            timeControl = "10+0";
        }
        if (turnColor == null || turnColor.isBlank()) {
            turnColor = "white";
        }
    }
}
