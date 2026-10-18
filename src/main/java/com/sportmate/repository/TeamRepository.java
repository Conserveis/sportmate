package com.sportmate.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sportmate.entity.Post;
import com.sportmate.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Integer> {

    List<Team> findByPostAndStatusOrderByCreatedAtAsc(Post post, String status);

    long countByPostAndStatus(Post post, String status);

    /** ชื่อทีมซ้ำในทัวร์นาเมนต์เดียวกันไหม (ไม่นับทีมที่ถูกปฏิเสธ/ยกเลิก) */
    @Query("""
        SELECT COUNT(t) > 0 FROM Team t
        WHERE t.post = :post AND LOWER(t.name) = LOWER(:name)
          AND t.status IN ('pending','approved')
    """)
    boolean existsActiveName(@Param("post") Post post, @Param("name") String name);
}
