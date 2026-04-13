package com.chess.demo.repository;

import com.chess.demo.entity.FidePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface FidePlayerRepository extends JpaRepository<FidePlayer, Integer>, JpaSpecificationExecutor<FidePlayer> {

    Optional<FidePlayer> findTopByOrderByUpdatedAtDesc();
}
