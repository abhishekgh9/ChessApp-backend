package com.chess.demo.repository;

import com.chess.demo.entity.Puzzle;
import com.chess.demo.entity.PuzzleAttempt;
import com.chess.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PuzzleAttemptRepository extends JpaRepository<PuzzleAttempt, UUID> {

    List<PuzzleAttempt> findByPuzzleAndUserOrderByCreatedAtAsc(Puzzle puzzle, User user);

    List<PuzzleAttempt> findByUserAndStatusInOrderByCreatedAtAsc(User user, Collection<String> statuses);

    @Query("""
            select count(distinct pa.puzzle.id)
            from PuzzleAttempt pa
            where pa.user = :user
            """)
    long countDistinctAttemptedPuzzles(@Param("user") User user);

    @Query("""
            select count(distinct pa.puzzle.id)
            from PuzzleAttempt pa
            where pa.user = :user
              and pa.status = 'COMPLETED'
            """)
    long countDistinctSolvedPuzzles(@Param("user") User user);
}
