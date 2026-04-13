package com.chess.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "fide_players")
@Getter
@Setter
public class FidePlayer {

    @Id
    @Column(name = "fide_id", nullable = false)
    private Integer fideId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 20)
    private String title;

    @Column(length = 4)
    private String federation;

    @Column(length = 1)
    private String sex;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "standard_rating")
    private Integer standardRating;

    @Column(name = "rapid_rating")
    private Integer rapidRating;

    @Column(name = "blitz_rating")
    private Integer blitzRating;

    @Column(name = "standard_games")
    private Integer standardGames;

    @Column(name = "rapid_games")
    private Integer rapidGames;

    @Column(name = "blitz_games")
    private Integer blitzGames;

    @Column(name = "standard_k")
    private Integer standardK;

    @Column(name = "rapid_k")
    private Integer rapidK;

    @Column(name = "blitz_k")
    private Integer blitzK;

    @Column(name = "standard_inactive", nullable = false)
    private Boolean standardInactive = false;

    @Column(name = "rapid_inactive", nullable = false)
    private Boolean rapidInactive = false;

    @Column(name = "blitz_inactive", nullable = false)
    private Boolean blitzInactive = false;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
