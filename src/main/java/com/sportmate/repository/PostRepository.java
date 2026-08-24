package com.sportmate.repository;

import com.sportmate.entity.Post;
import com.sportmate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Integer> {

    // หน้า Post: เฉพาะ PostType='Post' ที่ยังไม่หมดเวลา (DatePlay >= now)
    // และเผยแพร่แล้ว (PublishAt null = ทันที, หรือ PublishAt <= now) และไม่ถูกยกเลิก
    @Query("""
        SELECT p FROM Post p
        WHERE p.postType.name = 'Post'
          AND p.status <> 'cancelled'
          AND p.datePlay >= :now
          AND (p.publishAt IS NULL OR p.publishAt <= :now)
        ORDER BY p.datePlay ASC
    """)
    List<Post> findActivePosts(@Param("now") LocalDateTime now);

    // หน้า Tournament: PostType='Tournament' ทั้งหมด (ไม่หายแม้หมดเวลา)
    @Query("""
        SELECT p FROM Post p
        WHERE p.postType.name = 'Tournament'
          AND p.status <> 'cancelled'
          AND (p.publishAt IS NULL OR p.publishAt <= :now)
        ORDER BY p.datePlay DESC
    """)
    List<Post> findTournaments(@Param("now") LocalDateTime now);

    // ประวัติการจัดกิจกรรมของผู้ใช้
    List<Post> findByOwnerOrderByDateCreateDesc(User owner);

    // นับจำนวนโพสต์ของผู้ใช้ในช่วงเวลา (ใช้ตรวจโควตา 3 โพสต์/สัปดาห์)
    @Query("""
        SELECT COUNT(p) FROM Post p
        WHERE p.owner = :owner AND p.dateCreate >= :since
    """)
    long countByOwnerSince(@Param("owner") User owner, @Param("since") LocalDateTime since);

    // ค้นหา/กรอง โพสต์หรือทัวร์นาเมนต์ ตาม กีฬา / สถานที่ / ช่วงวันเวลา
    // onlyActive = true (หน้า Post: เอาเฉพาะที่ยังไม่หมดเวลา), false (หน้า Tournament: เอาทั้งหมด)
    @Query("""
        SELECT p FROM Post p
        WHERE p.postType.name = :ptype
          AND p.status <> 'cancelled'
          AND (p.publishAt IS NULL OR p.publishAt <= :now)
          AND (:onlyActive = false OR p.datePlay >= :now)
          AND (:sportId IS NULL OR p.sport.id = :sportId)
          AND (:locationId IS NULL OR p.location.id = :locationId)
          AND (:fromDt IS NULL OR p.datePlay >= :fromDt)
          AND (:toDt IS NULL OR p.datePlay <= :toDt)
        ORDER BY p.datePlay ASC
    """)
    List<Post> search(@Param("ptype") String ptype,
                      @Param("now") LocalDateTime now,
                      @Param("onlyActive") boolean onlyActive,
                      @Param("sportId") Integer sportId,
                      @Param("locationId") Integer locationId,
                      @Param("fromDt") LocalDateTime fromDt,
                      @Param("toDt") LocalDateTime toDt);
}
