package com.chess.demo.repository;

import com.chess.demo.entity.Game;
import com.chess.demo.entity.GameAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GameAnalysisRepository extends JpaRepository<GameAnalysis, UUID> {

    Optional<GameAnalysis> findByGame(Game game);
}
