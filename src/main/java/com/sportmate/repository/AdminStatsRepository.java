package com.sportmate.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.sportmate.entity.Post;

/** Query สถิติสำหรับแดชบอร์ดผู้ดูแลระบบ (อ่านอย่างเดียว ใช้ native SQL) */
public interface AdminStatsRepository extends Repository<Post, Integer> {

    /**
     * สถานะจริงของโพสต์ (alias p): ยกเลิก / เลยเวลาเล่นแล้ว = เสร็จสิ้น / นอกนั้น = เปิดรับอยู่
     * คอลัมน์ Post.Status ไม่ถูกเปลี่ยนเมื่อเลยวันเล่น จึงต้องคำนวณจาก DatePlay เอง
     */
    String POST_STATE_SQL = """
        CASE WHEN p.Status = 'cancelled' THEN 'cancelled'
             WHEN p.DatePlay < NOW()     THEN 'finished'
             ELSE 'open' END""";

    /** โพสต์ทั้งหมดพร้อมสถานะจริง สำหรับหน้า /admin/posts (alias x) — กรองกีฬา / คำค้นได้ */
    String ADMIN_POSTS_SQL = """
        SELECT p.PostID, p.PostName, s.SportName, u.UserName, p.DatePlay,
               """ + POST_STATE_SQL + """
                AS st,
               (SELECT COUNT(*) FROM Event e WHERE e.PostID = p.PostID AND e.Status = 'approved') AS joined,
               p.MaxPlayer, p.MinPlayer, t.PtypeName, u.UserID, p.DateCreate
        FROM Post p
        JOIN Sport    s ON s.SportID    = p.SportID
        JOIN `User`   u ON u.UserID     = p.OwnerUserID
        JOIN PostType t ON t.PostTypeID = p.PostTypeID
        WHERE (:sportId IS NULL OR p.SportID = :sportId)
          AND (:q IS NULL OR p.PostName LIKE CONCAT('%', :q, '%') OR u.UserName LIKE CONCAT('%', :q, '%'))
        """;

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
        SELECT p.PostID, p.PostName, s.SportName, u.UserName, p.DatePlay,
               """ + POST_STATE_SQL + """
               ,
               (SELECT COUNT(*) FROM Event e WHERE e.PostID = p.PostID AND e.Status = 'approved'),
               p.MaxPlayer
        FROM Post p
        JOIN Sport  s ON s.SportID = p.SportID
        JOIN `User` u ON u.UserID  = p.OwnerUserID
        ORDER BY p.DateCreate DESC
        LIMIT 8
        """, nativeQuery = true)
    List<Object[]> recentPosts();

    // ---------- หน้ารายการโพสต์ทั้งหมด (/admin/posts) ----------
    @Query(value = "SELECT * FROM (" + ADMIN_POSTS_SQL + ") x"
            + " WHERE (:status IS NULL OR x.st = :status)"
            + " ORDER BY x.DateCreate DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Object[]> adminPosts(@Param("sportId") Integer sportId, @Param("status") String status,
                              @Param("q") String q, @Param("limit") int limit, @Param("offset") int offset);

    /** จำนวนโพสต์ต่อสถานะ ตามตัวกรองกีฬา / คำค้น (ใช้แสดงตัวเลขบนแท็บสถานะ) */
    @Query(value = "SELECT x.st, COUNT(*) FROM (" + ADMIN_POSTS_SQL + ") x GROUP BY x.st", nativeQuery = true)
    List<Object[]> adminPostStateCounts(@Param("sportId") Integer sportId, @Param("q") String q);

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

    // ---------- รายได้จากการสมัครสมาชิก (ตาราง Receipt) ----------
    @Query(value = "SELECT COALESCE(SUM(PlaymentAmount),0) FROM Receipt", nativeQuery = true)
    BigDecimal sumRevenue();

    @Query(value = """
        SELECT COALESCE(SUM(PlaymentAmount),0) FROM Receipt
        WHERE DatePlayment >= DATE_FORMAT(CURDATE(),'%Y-%m-01')
        """, nativeQuery = true)
    BigDecimal sumRevenueThisMonth();

    @Query(value = """
        SELECT COALESCE(SUM(PlaymentAmount),0) FROM Receipt
        WHERE DatePlayment >= DATE_SUB(DATE_FORMAT(CURDATE(),'%Y-%m-01'), INTERVAL 1 MONTH)
          AND DatePlayment <  DATE_FORMAT(CURDATE(),'%Y-%m-01')
        """, nativeQuery = true)
    BigDecimal sumRevenueLastMonth();

    @Query(value = "SELECT COUNT(*) FROM Receipt", nativeQuery = true)
    long countPayments();

    @Query(value = "SELECT COUNT(*) FROM `User` WHERE MembershipExpireAt > NOW()", nativeQuery = true)
    long countActiveMembers();

    @Query(value = """
        SELECT DATE_FORMAT(DatePlayment,'%Y-%m') AS ym, SUM(PlaymentAmount) AS total, COUNT(*) AS c
        FROM Receipt
        WHERE DatePlayment >= DATE_SUB(DATE_FORMAT(CURDATE(),'%Y-%m-01'), INTERVAL 5 MONTH)
        GROUP BY ym ORDER BY ym
        """, nativeQuery = true)
    List<Object[]> revenuePerMonth();

    @Query(value = """
        SELECT r.PlaymentID, u.UserName, r.PlaymentAmount, r.DatePlayment, u.UserID
        FROM Receipt r
        JOIN `User` u ON u.UserID = r.UserID
        ORDER BY r.DatePlayment DESC
        LIMIT 6
        """, nativeQuery = true)
    List<Object[]> recentPayments();

    // ---------- สถิติตามช่วงเวลา (1 / 3 / 6 เดือน) ----------
    // นับเป็นรายวันตั้งแต่ :start แล้วให้ AdminService รวมเป็นสัปดาห์ / 2 สัปดาห์ / เดือน
    @Query(value = """
        SELECT DATE(DateCreate) AS d, COUNT(*) AS c
        FROM Post
        WHERE DateCreate >= :start
        GROUP BY d ORDER BY d
        """, nativeQuery = true)
    List<Object[]> postsPerDaySince(@Param("start") LocalDate start);

    @Query(value = """
        SELECT DATE(JoinDate) AS d, COUNT(*) AS c
        FROM Event
        WHERE Status = 'approved' AND JoinDate >= :start
        GROUP BY d ORDER BY d
        """, nativeQuery = true)
    List<Object[]> joinsPerDaySince(@Param("start") LocalDate start);

    @Query(value = """
        SELECT DATE(DatePlayment) AS d, SUM(PlaymentAmount) AS total, COUNT(*) AS c
        FROM Receipt
        WHERE DatePlayment >= :start
        GROUP BY d ORDER BY d
        """, nativeQuery = true)
    List<Object[]> revenuePerDaySince(@Param("start") LocalDate start);

    @Query(value = """
        SELECT x.st, COUNT(*) FROM (
            SELECT """ + POST_STATE_SQL + """
                 AS st
            FROM Post p
            WHERE p.DateCreate >= :start
        ) x
        GROUP BY x.st
        ORDER BY FIELD(x.st, 'open', 'finished', 'cancelled')
        """, nativeQuery = true)
    List<Object[]> postStatusCountsSince(@Param("start") LocalDate start);

    /**
     * โพสต์ที่ "ไม่ถึงขั้นต่ำ" แยกตามกีฬา (เฉพาะโพสต์ทั่วไป ไม่รวมทัวร์นาเมนต์และโพสต์ที่ยกเลิก)
     * ไม่ถึงขั้นต่ำ = ผู้เข้าร่วมที่ approved น้อยกว่า MinPlayer
     * คอลัมน์: กีฬา, โพสต์ทั้งหมด, ไม่ถึงขั้นต่ำ, ไม่ถึงขั้นต่ำและเลยเวลาเล่นแล้ว
     */
    @Query(value = """
        SELECT s.SportName, COUNT(*) AS posts,
               SUM(x.joined < x.MinPlayer) AS under_min,
               SUM(x.joined < x.MinPlayer AND x.DatePlay < NOW()) AS under_ended
        FROM (
            SELECT p.SportID, p.MinPlayer, p.DatePlay,
                   (SELECT COUNT(*) FROM Event e
                    WHERE e.PostID = p.PostID AND e.Status = 'approved') AS joined
            FROM Post p
            JOIN PostType t ON t.PostTypeID = p.PostTypeID
            WHERE p.Status <> 'cancelled' AND t.PtypeName = 'Post' AND p.DateCreate >= :start
        ) x
        JOIN Sport s ON s.SportID = x.SportID
        GROUP BY s.SportID, s.SportName
        ORDER BY SUM(x.joined < x.MinPlayer) / COUNT(*) DESC, under_min DESC, posts DESC
        """, nativeQuery = true)
    List<Object[]> underMinBySportSince(@Param("start") LocalDate start);

    /**
     * สนามยอดนิยม: จำนวนโพสต์และผู้เข้าร่วม (approved) ต่อคู่ สถานที่ × กีฬา (ไม่รวมโพสต์ที่ยกเลิก)
     * คอลัมน์: LocationID, ชื่อสถานที่, กีฬา, โพสต์, ผู้เข้าร่วม, SportID
     */
    @Query(value = """
        SELECT l.LocationID, l.LocationName, s.SportName, COUNT(*) AS posts,
               COALESCE(SUM(j.c), 0) AS joins, s.SportID
        FROM Post p
        JOIN Location l ON l.LocationID = p.LocationID
        JOIN Sport s    ON s.SportID    = p.SportID
        LEFT JOIN (SELECT PostID, COUNT(*) AS c FROM Event
                   WHERE Status = 'approved' GROUP BY PostID) j ON j.PostID = p.PostID
        WHERE p.Status <> 'cancelled' AND p.DateCreate >= :start
        GROUP BY l.LocationID, l.LocationName, s.SportID, s.SportName
        ORDER BY posts DESC, joins DESC
        """, nativeQuery = true)
    List<Object[]> venueSportStatsSince(@Param("start") LocalDate start);

    // ---------- การใช้งาน (LastActivityAt) ----------
    /** [ทั้งหมด, active 7 วัน, active 30 วัน, หายไป > 30 วัน, ยังไม่มีข้อมูล] */
    @Query(value = """
        SELECT COUNT(*),
               COALESCE(SUM(LastActivityAt >= NOW() - INTERVAL 7 DAY), 0),
               COALESCE(SUM(LastActivityAt >= NOW() - INTERVAL 30 DAY), 0),
               COALESCE(SUM(LastActivityAt <  NOW() - INTERVAL 30 DAY), 0),
               COALESCE(SUM(LastActivityAt IS NULL), 0)
        FROM `User`
        """, nativeQuery = true)
    List<Object[]> activityCounts();

    /** ผู้ใช้ที่ไม่ได้เข้ามาเกิน 30 วัน — หายไปนานสุดก่อน */
    @Query(value = """
        SELECT UserID, UserName, LastActivityAt, DATEDIFF(NOW(), LastActivityAt) AS days
        FROM `User`
        WHERE LastActivityAt < NOW() - INTERVAL 30 DAY
        ORDER BY LastActivityAt ASC
        LIMIT 6
        """, nativeQuery = true)
    List<Object[]> dormantUsers();

    @Query(value = "SELECT COUNT(*) FROM `User` WHERE CreatedAt >= :start", nativeQuery = true)
    long countNewUsersSince(@Param("start") LocalDate start);
}