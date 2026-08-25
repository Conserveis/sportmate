package com.sportmate.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sportmate.dto.SportCount;
import com.sportmate.entity.Event;
import com.sportmate.entity.Post;
import com.sportmate.entity.User;

public interface EventRepository extends JpaRepository<Event, Integer> {

    Optional<Event> findByUserAndPost(User user, Post post);

    // จำนวนผู้เข้าร่วมที่ยัง active (pending/approved) ของโพสต์หนึ่ง
    @Query("""
        SELECT COUNT(e) FROM Event e
        WHERE e.post = :post AND e.status IN ('pending','approved')
    """)
    long countActiveJoins(@Param("post") Post post);

    // รายชื่อผู้เข้าร่วมของโพสต์
    @Query("""
        SELECT e FROM Event e
        WHERE e.post = :post AND e.status IN ('pending','approved')
        ORDER BY e.joinDate ASC
    """)
    List<Event> findParticipants(@Param("post") Post post);

    // ประวัติการเข้าร่วมทั้งหมดของผู้ใช้ (ยังไม่ยกเลิก)
    @Query("""
        SELECT e FROM Event e
        WHERE e.user = :user AND e.status IN ('pending','approved')
        ORDER BY e.post.datePlay DESC
    """)
    List<Event> findJoinedByUser(@Param("user") User user);

    // หน้า "จัดเก็บกิจกรรม": โพสต์ที่ user เข้าร่วมแล้วและ "หมดเวลาเข้าร่วม" (DatePlay < now)
    @Query("""
        SELECT e FROM Event e
        WHERE e.user = :user
          AND e.status IN ('pending','approved')
          AND e.post.datePlay < :now
        ORDER BY e.post.datePlay DESC
    """)
    List<Event> findArchivedByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    // กิจกรรมที่ยังกำลังจะมาถึง (สำหรับโปรไฟล์)
    @Query("""
        SELECT e FROM Event e
        WHERE e.user = :user
          AND e.status IN ('pending','approved')
          AND e.post.datePlay >= :now
        ORDER BY e.post.datePlay ASC
    """)
    List<Event> findUpcomingByUser(@Param("user") User user, @Param("now") LocalDateTime now);
    // ===== โปรไฟล์สาธารณะ: สถิติฝั่งผู้เข้าร่วม =====

    // จำนวนครั้งการเข้าร่วมทั้งหมด (ที่ยังไม่ยกเลิก)
    @Query("""
        SELECT COUNT(e) FROM Event e
        WHERE e.user = :user AND e.status IN ('pending','approved')
    """)
    long countJoinsByUser(@Param("user") User user);

    // เข้าร่วมและกิจกรรมจบไปแล้วกี่ครั้ง
    @Query("""
        SELECT COUNT(e) FROM Event e
        WHERE e.user = :user
          AND e.status IN ('pending','approved')
          AND e.post.datePlay < :now
    """)
    long countAttendedByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * นับการเข้าร่วมแยกตามชนิดกีฬา เรียงจากมากไปน้อย
     * แถวแรกของผลลัพธ์ = "กีฬาที่เข้าร่วมมากที่สุด"
     */
    @Query("""
        SELECT new com.sportmate.dto.SportCount(e.post.sport.name, COUNT(e))
        FROM Event e
        WHERE e.user = :user AND e.status IN ('pending','approved')
        GROUP BY e.post.sport.name
        ORDER BY COUNT(e) DESC, e.post.sport.name ASC
    """)
    List<SportCount> countJoinsBySport(@Param("user") User user);
}
