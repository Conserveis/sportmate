package com.sportmate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sportmate.entity.Review;
import com.sportmate.entity.User;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    Optional<Review> findByEvent_Id(Integer eventId);

    /**
     * รีวิวทั้งหมดที่ "ผู้จัดคนนี้" ได้รับ
     * เส้นทางความสัมพันธ์: Review -> Event -> Post -> owner
     * (คนรีวิวคือ r.event.user ซึ่งเป็นผู้เข้าร่วมกิจกรรมนั้น)
     */
    @Query("""
        SELECT r FROM Review r
        WHERE r.event.post.owner = :owner
        ORDER BY r.reviewDate DESC
    """)
    List<Review> findReviewsForOwner(@Param("owner") User owner);

    /** จำนวนรีวิวที่ผู้จัดคนนี้ได้รับ */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.event.post.owner = :owner")
    long countReviewsForOwner(@Param("owner") User owner);
}
