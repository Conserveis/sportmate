package com.sportmate.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import com.sportmate.entity.Post;

/** Query สถิติสำหรับแดชบอร์ดผู้ดูแลระบบ (อ่านอย่างเดียว ใช้ native SQL) */
public interface AdminStatsRepository extends Repository<Post, Integer> {

    // ---------- ตัวเลขสรุป ----------
    @Query(value = "SELECT COUNT(*) FROM `User`", nativeQuery = true)
    long countUsers();

    @Query(value = "SELECT COUNT(*) FROM `User` WHERE CreatedAt >= DATE_SUB(NOW(), INTERVAL 30 DAY)",
           nativeQuery = true)
    long countNewUsers30d();

    @Query(value = "SELECT COUNT(*) FROM Post", nativeQuery = true)
    long countPosts();

    @Query(value = """
        SELECT COUNT(*) FROM Post p
        JOIN PostType t ON t.PostTypeID = p.PostTypeID
        WHERE t.PtypeName = 'Tournament'
        """, nativeQuery = true)
    long countTournaments();

    @Query(value = """
        SELECT COUNT(*) FROM Post
        WHERE Status = 'open' AND DatePlay >= NOW()
        """, nativeQuery = true)
    long countActivePosts();

    @Query(value = "SELECT COUNT(*) FROM Post WHERE Status = 'cancelled'", nativeQuery = true)
    long countCancelledPosts();

    @Query(value = "SELECT COUNT(*) FROM Event WHERE Status = 'approved'", nativeQuery = true)
    long countApprovedJoins();

    @Query(value = "SELECT COUNT(*) FROM Event WHERE Status = 'pending'", nativeQuery = true)
    long countPendingJoins();

    @Query(value = "SELECT COUNT(*) FROM Review", nativeQuery = true)
    long countReviews();

    @Query(value = "SELECT COALESCE(AVG(ReviewScore),0) FROM Review", nativeQuery = true)
    Double avgReviewScore();

    /** อัตราการเติมเต็มเฉลี่ย (ผู้เข้าร่วมจริง / ที่รับได้) เป็น % */
    @Query(value = """
        SELECT COALESCE(AVG(rate),0) * 100 FROM (
            SELECT COUNT(e.EventID) / p.MaxPlayer AS rate
            FROM Post p
            LEFT JOIN Event e ON e.PostID = p.PostID AND e.Status = 'approved'
            WHERE p.Status <> 'cancelled'
            GROUP BY p.PostID, p.MaxPlayer
        ) x
        """, nativeQuery = true)
    Double avgFillRate();

    // ---------- กราฟ ----------
    @Query(value = """
        SELECT DATE_FORMAT(DateCreate,'%Y-%m') AS ym, COUNT(*) AS c
        FROM Post
        WHERE DateCreate >= DATE_SUB(DATE_FORMAT(CURDATE(),'%Y-%m-01'), INTERVAL 5 MONTH)
        GROUP BY ym ORDER BY ym
        """, nativeQuery = true)
    List<Object[]> postsPerMonth();

    @Query(value = """
        SELECT DATE_FORMAT(JoinDate,'%Y-%m') AS ym, COUNT(*) AS c
        FROM Event
        WHERE Status = 'approved'
          AND JoinDate >= DATE_SUB(DATE_FORMAT(CURDATE(),'%Y-%m-01'), INTERVAL 5 MONTH)
        GROUP BY ym ORDER BY ym
        """, nativeQuery = true)
    List<Object[]> joinsPerMonth();

    @Query(value = "SELECT Status, COUNT(*) FROM Post GROUP BY Status", nativeQuery = true)
    List<Object[]> postStatusCounts();

    @Query(value = """
        SELECT s.SportName,
               COUNT(DISTINCT p.PostID) AS posts,
               COUNT(DISTINCT CASE WHEN e.Status = 'approved' THEN e.EventID END) AS joins
        FROM Sport s
        LEFT JOIN Post  p ON p.SportID = s.SportID
        LEFT JOIN Event e ON e.PostID  = p.PostID
        GROUP BY s.SportID, s.SportName
        HAVING posts > 0
        ORDER BY posts DESC, joins DESC
        LIMIT 8
        """, nativeQuery = true)
    List<Object[]> statsBySport();

    // ---------- ตาราง ----------
    @Query(value = """
        SELECT p.PostID, p.PostName, s.SportName, u.UserName, p.DatePlay, p.Status,
               (SELECT COUNT(*) FROM Event e WHERE e.PostID = p.PostID AND e.Status = 'approved'),
               p.MaxPlayer
        FROM Post p
        JOIN Sport  s ON s.SportID = p.SportID
        JOIN `User` u ON u.UserID  = p.OwnerUserID
        ORDER BY p.DateCreate DESC
        LIMIT 8
        """, nativeQuery = true)
    List<Object[]> recentPosts();

    @Query(value = """
        SELECT u.UserID, u.UserName, COUNT(p.PostID) AS c, ROUND(u.AvgScore, 2)
        FROM `User` u
        JOIN Post p ON p.OwnerUserID = u.UserID
        GROUP BY u.UserID, u.UserName, u.AvgScore
        ORDER BY c DESC
        LIMIT 5
        """, nativeQuery = true)
    List<Object[]> topOrganizers();

    @Query(value = """
        SELECT u.UserID, u.UserName, u.Gmail, u.AuthProvider, u.CreatedAt
        FROM `User` u
        ORDER BY u.CreatedAt DESC
        LIMIT 6
        """, nativeQuery = true)
    List<Object[]> recentUsers();
}