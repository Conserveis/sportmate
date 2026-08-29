package com.sportmate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sportmate.entity.Review;
import com.sportmate.entity.User;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    Optional<Review> findByEvent_IdAndDirection(Integer eventId, String direction);

    // ---- รีวิวที่ผู้จัดคนนี้ได้รับ ----
    @Query("""
        SELECT r FROM Review r
        WHERE r.event.post.owner = :owner AND r.direction = 'to_owner'
        ORDER BY r.reviewDate DESC
    """)
    List<Review> findReviewsForOwner(@Param("owner") User owner);

    @Query("""
        SELECT COUNT(r) FROM Review r
        WHERE r.event.post.owner = :owner AND r.direction = 'to_owner'
    """)
    long countReviewsForOwner(@Param("owner") User owner);

    // ---- รีวิวที่ผู้ใช้คนนี้ได้รับ "ในฐานะผู้เข้าร่วม" ----
    @Query("""
        SELECT r FROM Review r
        WHERE r.event.user = :user AND r.direction = 'to_participant'
        ORDER BY r.reviewDate DESC
    """)
    List<Review> findReviewsForParticipant(@Param("user") User user);

    @Query("""
        SELECT COUNT(r) FROM Review r
        WHERE r.event.user = :user AND r.direction = 'to_participant'
    """)
    long countReviewsForParticipant(@Param("user") User user);

    @Query("""
        SELECT AVG(r.reviewScore) FROM Review r
        WHERE r.event.user = :user AND r.direction = 'to_participant'
    """)
    Double avgScoreForParticipant(@Param("user") User user);
}