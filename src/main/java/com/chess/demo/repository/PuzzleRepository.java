package com.chess.demo.repository;

import com.chess.demo.entity.Puzzle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PuzzleRepository extends JpaRepository<Puzzle, UUID> {

    @EntityGraph(attributePaths = {"tags", "solutionSteps"})
    Optional<Puzzle> findByIdAndActiveTrue(UUID id);

        @EntityGraph(attributePaths = {"tags"})
        @Query("""
            select distinct p
            from Puzzle p
            left join p.tags t
            where p.active = true
              and (:difficulty is null or lower(p.difficulty) = :difficulty)
              and (:theme is null or lower(p.primaryTheme) = :theme
               or lower(t.slug) = :theme
               or lower(t.name) = :theme)
            """)
    Page<Puzzle> searchActive(@Param("difficulty") String difficulty,
                              @Param("theme") String theme,
                              Pageable pageable);

    @EntityGraph(attributePaths = {"tags", "solutionSteps"})
    List<Puzzle> findByActiveTrueOrderByCreatedAtAscIdAsc();
}
