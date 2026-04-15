package com.chess.demo.repository;

import com.chess.demo.entity.PuzzleTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PuzzleTagRepository extends JpaRepository<PuzzleTag, UUID> {

    Optional<PuzzleTag> findBySlugIgnoreCase(String slug);
}
