package com.sportmate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sportmate.entity.Post;
import com.sportmate.entity.TournamentMatch;

public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Integer> {

    List<TournamentMatch> findByPostOrderByRoundAscSlotAsc(Post post);

    Optional<TournamentMatch> findByPostAndRoundAndSlot(Post post, Integer round, Integer slot);

    boolean existsByPost(Post post);

    long countByPostAndStatus(Post post, String status);
}
