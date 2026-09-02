package com.sportmate.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sportmate.repository.AdminStatsRepository;

/** รวบรวมสถิติทั้งหมดสำหรับหน้าแดชบอร์ดผู้ดูแลระบบ */
@Service
public class AdminService {

    private static final String[] TH_MONTH_SHORT = {
            "ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.",
            "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค."
    };

    private final AdminStatsRepository repo;

    public AdminService(AdminStatsRepository repo) {
        this.repo = repo;
    }

    public Map<String, Object> summary() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("users", repo.countUsers());
        m.put("newUsers30d", repo.countNewUsers30d());
        m.put("posts", repo.countPosts());
        m.put("tournaments", repo.countTournaments());
        m.put("activePosts", repo.countActivePosts());
        m.put("cancelledPosts", repo.countCancelledPosts());
        m.put("joins", repo.countApprovedJoins());
        m.put("pendingJoins", repo.countPendingJoins());
        m.put("reviews", repo.countReviews());
        m.put("avgScore", round(nz(repo.avgReviewScore()), 2));
        m.put("fillRate", (int) Math.round(nz(repo.avgFillRate())));
        return m;
    }

    public List<Map<String, Object>> monthlyTrend() {
        Map<String, Long> posts = toMap(repo.postsPerMonth());
        Map<String, Long> joins = toMap(repo.joinsPerMonth());

        YearMonth start = YearMonth.from(LocalDate.now()).minusMonths(5);
        long max = 1;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            YearMonth ym = start.plusMonths(i);
            String key = String.format("%04d-%02d", ym.getYear(), ym.getMonthValue());
            long p = posts.getOrDefault(key, 0L);
            long j = joins.getOrDefault(key, 0L);
            max = Math.max(max, Math.max(p, j));

            Map<String, Object> row = new HashMap<>();
            row.put("label", TH_MONTH_SHORT[ym.getMonthValue() - 1]);
            row.put("posts", p);
            row.put("joins", j);
            rows.add(row);
        }
        for (Map<String, Object> row : rows) {
            row.put("postsPct", pct((Long) row.get("posts"), max));
            row.put("joinsPct", pct((Long) row.get("joins"), max));
        }
        return rows;
    }

    public List<Map<String, Object>> postStatusDonut() {
        Map<String, String> labels = Map.of(
                "open", "เปิดรับอยู่",
                "closed", "ปิดรับแล้ว",
                "finished", "จบแล้ว",
                "cancelled", "ยกเลิก");
        Map<String, String> colors = Map.of(
                "open", "#FF6A00",       // Orange Burst
                "closed", "#0071FF",     // Blue Raspberry
                "finished", "#C8FF40",   // Lime Charge
                "cancelled", "#FF007A"); // Berry Vibe

        List<Object[]> raw = repo.postStatusCounts();
        long total = 0;
        for (Object[] r : raw) total += num(r[1]);
        if (total == 0) total = 1;

        double circumference = 2 * Math.PI * 54;   // r = 54
        double offset = 0;
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : raw) {
            String status = String.valueOf(r[0]);
            long count = num(r[1]);
            double share = (double) count / total;

            Map<String, Object> seg = new HashMap<>();
            seg.put("status", status);
            seg.put("label", labels.getOrDefault(status, status));
            seg.put("color", colors.getOrDefault(status, "#B9C6BD"));
            seg.put("count", count);
            seg.put("pct", (int) Math.round(share * 100));
            seg.put("dash", round(share * circumference, 2));
            seg.put("gap", round(circumference - share * circumference, 2));
            seg.put("offset", round(-offset, 2));
            out.add(seg);
            offset += share * circumference;
        }
        return out;
    }

    public List<Map<String, Object>> bySport() {
        List<Object[]> raw = repo.statsBySport();
        long max = 1;
        for (Object[] r : raw) max = Math.max(max, num(r[1]));

        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : raw) {
            Map<String, Object> m = new HashMap<>();
            m.put("sport", String.valueOf(r[0]));
            m.put("posts", num(r[1]));
            m.put("joins", num(r[2]));
            m.put("pct", pct(num(r[1]), max));
            out.add(m);
        }
        return out;
    }

    public List<Map<String, Object>> recentPosts() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : repo.recentPosts()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", num(r[0]));
            m.put("name", String.valueOf(r[1]));
            m.put("sport", String.valueOf(r[2]));
            m.put("owner", String.valueOf(r[3]));
            m.put("datePlay", r[4] == null ? "-" : r[4].toString().substring(0, 16).replace('T', ' '));
            m.put("status", String.valueOf(r[5]));
            m.put("joined", num(r[6]));
            m.put("max", num(r[7]));
            out.add(m);
        }
        return out;
    }

    public List<Map<String, Object>> topOrganizers() {
        List<Map<String, Object>> out = new ArrayList<>();
        List<Object[]> raw = repo.topOrganizers();
        long max = 1;
        for (Object[] r : raw) max = Math.max(max, num(r[2]));
        for (Object[] r : raw) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", num(r[0]));
            m.put("name", String.valueOf(r[1]));
            m.put("posts", num(r[2]));
            m.put("score", r[3] == null ? "0.00" : r[3].toString());
            m.put("pct", pct(num(r[2]), max));
            out.add(m);
        }
        return out;
    }

    public List<Map<String, Object>> recentUsers() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : repo.recentUsers()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", num(r[0]));
            m.put("name", String.valueOf(r[1]));
            m.put("gmail", String.valueOf(r[2]));
            m.put("provider", String.valueOf(r[3]));
            m.put("createdAt", r[4] == null ? "-" : r[4].toString().substring(0, 10));
            out.add(m);
        }
        return out;
    }

    // ---------- helper ----------
    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> m = new HashMap<>();
        for (Object[] r : rows) m.put(String.valueOf(r[0]), num(r[1]));
        return m;
    }

    private static long num(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private static double nz(Double d) {
        return d == null ? 0d : d;
    }

    private static int pct(long value, long max) {
        if (max <= 0) return 0;
        int p = (int) Math.round(value * 100.0 / max);
        return value > 0 ? Math.max(p, 4) : 0;   // ให้แท่งเล็กสุดยังมองเห็น
    }

    private static double round(double v, int scale) {
        double f = Math.pow(10, scale);
        return Math.round(v * f) / f;
    }
}