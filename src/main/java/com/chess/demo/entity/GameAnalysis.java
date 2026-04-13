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
@Table(name = "game_analyses")
@Getter
@Setter
public class GameAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false, unique = true)
    private Game game;

    @Column(name = "game_status", nullable = false, length = 20)
    private String gameStatus;

    @Column(name = "game_result", length = 20)
    private String gameResult;

    @Column(name = "overall_accuracy")
    private Double overallAccuracy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "white_player_id")
    private User whitePlayer;

    @Column(name = "white_accuracy")
    private Double whiteAccuracy;

    @Column(name = "white_current_rating")
    private Integer whiteCurrentRating;

    @Column(name = "white_provisional_rating")
    private Integer whiteProvisionalRating;

    @Column(name = "white_rating_delta")
    private Integer whiteRatingDelta;

    @Column(name = "white_moves_analyzed")
    private Integer whiteMovesAnalyzed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "black_player_id")
    private User blackPlayer;

    @Column(name = "black_accuracy")
    private Double blackAccuracy;

    @Column(name = "black_current_rating")
    private Integer blackCurrentRating;

    @Column(name = "black_provisional_rating")
    private Integer blackProvisionalRating;

    @Column(name = "black_rating_delta")
    private Integer blackRatingDelta;

    @Column(name = "black_moves_analyzed")
    private Integer blackMovesAnalyzed;

    @Column(name = "source_game_updated_at", nullable = false)
    private Instant sourceGameUpdatedAt;

    @Column(name = "source_move_count", nullable = false)
    private Integer sourceMoveCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("moveNumber ASC")
    private List<GameAnalysisMove> moves = new ArrayList<>();
}
