package com.sportmate.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    // ---------- ช่วงเวลา (1 / 3 / 6 เดือน) ----------
    /** ช่วงย่อย 1 แท่งในกราฟ: from..to (นับรวมทั้งสองวัน) */
    public record Bucket(LocalDate from, LocalDate to, String label, String sub) {}

    /** รับได้แค่ 1, 3, 6 — ค่าอื่นหรือไม่ส่งมา = 6 */
    public static int normalizeRange(Integer range) {
        return (range != null && (range == 1 || range == 3)) ? range : 6;
    }

    /**
     * แบ่งช่วงให้กราฟสมดุล:
     * 1 เดือน = 4 สัปดาห์ (ช่วงละ 7 วัน), 3 เดือน = 6 ช่วงครึ่งเดือน (1–15, 16–สิ้นเดือน), 6 เดือน = 6 เดือนปฏิทิน
     */
    public List<Bucket> buckets(int range) {
        LocalDate today = LocalDate.now();
        List<Bucket> out = new ArrayList<>();
        if (range == 6) {
            YearMonth start = YearMonth.from(today).minusMonths(5);
            for (int i = 0; i < 6; i++) {
                YearMonth ym = start.plusMonths(i);
                LocalDate to = (i == 5) ? today : ym.atEndOfMonth();
                out.add(new Bucket(ym.atDay(1), to,
                        TH_MONTH_SHORT[ym.getMonthValue() - 1],
                        String.valueOf(ym.getYear() + 543)));
            }
            return out;
        }
        if (range == 3) {
            // ครึ่งเดือน: 1–15 และ 16–วันสุดท้ายของเดือน ย้อนหลัง 6 ช่วง (ช่วงสุดท้ายคือครึ่งเดือนปัจจุบัน)
            LocalDate from = today.withDayOfMonth(today.getDayOfMonth() <= 15 ? 1 : 16);
            for (int i = 0; i < 5; i++) {
                from = (from.getDayOfMonth() == 16)
                        ? from.withDayOfMonth(1)
                        : from.minusMonths(1).withDayOfMonth(16);
            }
            for (int i = 0; i < 6; i++) {
                LocalDate halfEnd = (from.getDayOfMonth() == 1)
                        ? from.withDayOfMonth(15)
                        : YearMonth.from(from).atEndOfMonth();
                LocalDate to = (i == 5) ? today : halfEnd;
                out.add(new Bucket(from, to, thSpan(from, halfEnd), ""));
                from = halfEnd.plusDays(1);
            }
            return out;
        }
        // 1 เดือน: 4 สัปดาห์ ช่วงละ 7 วัน สิ้นสุดวันนี้
        LocalDate start = today.minusDays(4 * 7 - 1);
        for (int i = 0; i < 4; i++) {
            LocalDate from = start.plusDays(i * 7L);
            LocalDate to = from.plusDays(6);
            out.add(new Bucket(from, to, thSpan(from, to), ""));
        }
        return out;
    }

    /** ข้อมูลแถบเลือกช่วง: ช่วงวันที่ + ยอดสรุปของช่วงนั้น */
    public Map<String, Object> period(int range) {
        List<Bucket> bs = buckets(range);
        LocalDate from = bs.get(0).from();
        LocalDate to = bs.get(bs.size() - 1).to();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("range", range);
        m.put("text", thDay(from)
                + (from.getYear() != to.getYear() ? " " + (from.getYear() + 543) : "")
                + " – " + thDay(to) + " " + (to.getYear() + 543));
        m.put("unit", switch (range) {
            case 1 -> "แบ่งรายสัปดาห์ · 4 สัปดาห์";
            case 3 -> "แบ่งครึ่งเดือน (1–15, 16–สิ้นเดือน) · 6 ช่วง";
            default -> "แบ่งรายเดือน · 6 เดือน";
        });
        m.put("posts", sumAll(toDayMap(repo.postsPerDaySince(from))));
        m.put("joins", sumAll(toDayMap(repo.joinsPerDaySince(from))));
        m.put("newUsers", repo.countNewUsersSince(from));
        m.put("fillRate", (int) Math.round(nz(repo.avgFillRateSince(from))));
        return m;
    }

    public List<Map<String, Object>> trend(int range) {
        List<Bucket> bs = buckets(range);
        LocalDate start = bs.get(0).from();
        Map<LocalDate, Long> posts = toDayMap(repo.postsPerDaySince(start));
        Map<LocalDate, Long> joins = toDayMap(repo.joinsPerDaySince(start));

        long max = 1;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Bucket b : bs) {
            long p = sumIn(posts, b);
            long j = sumIn(joins, b);
            max = Math.max(max, Math.max(p, j));

            Map<String, Object> row = new HashMap<>();
            row.put("label", b.label());
            row.put("sub", b.sub());
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

    public List<Map<String, Object>> postStatusDonut(int range) {
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

        List<Object[]> raw = repo.postStatusCountsSince(buckets(range).get(0).from());
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

    // ---------- รายได้จากการสมัครสมาชิก ----------
    public Map<String, Object> revenue() {
        BigDecimal thisMonth = nzb(repo.sumRevenueThisMonth());
        BigDecimal lastMonth = nzb(repo.sumRevenueLastMonth());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", nzb(repo.sumRevenue()));
        m.put("thisMonth", thisMonth);
        m.put("lastMonth", lastMonth);
        m.put("payments", repo.countPayments());
        m.put("activeMembers", repo.countActiveMembers());
        // % เปลี่ยนแปลงเทียบเดือนก่อน (null = เดือนก่อนไม่มีรายได้ เทียบไม่ได้)
        m.put("growthPct", lastMonth.signum() == 0 ? null
                : thisMonth.subtract(lastMonth).multiply(BigDecimal.valueOf(100))
                           .divide(lastMonth, 0, RoundingMode.HALF_UP).intValue());
        return m;
    }

    public List<Map<String, Object>> revenueTrend(int range) {
        List<Bucket> bs = buckets(range);
        Map<LocalDate, BigDecimal> totals = new HashMap<>();
        Map<LocalDate, Long> counts = new HashMap<>();
        for (Object[] r : repo.revenuePerDaySince(bs.get(0).from())) {
            LocalDate d = toLocalDate(r[0]);
            totals.put(d, new BigDecimal(r[1].toString()));
            counts.put(d, num(r[2]));
        }

        long max = 1;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Bucket b : bs) {
            BigDecimal amt = BigDecimal.ZERO;
            long c = 0;
            for (LocalDate d = b.from(); !d.isAfter(b.to()); d = d.plusDays(1)) {
                amt = amt.add(totals.getOrDefault(d, BigDecimal.ZERO));
                c += counts.getOrDefault(d, 0L);
            }
            max = Math.max(max, amt.longValue());
            Map<String, Object> row = new HashMap<>();
            row.put("label", b.label());
            row.put("sub", b.sub());
            row.put("amount", amt);
            row.put("count", c);
            rows.add(row);
        }
        for (Map<String, Object> row : rows) {
            row.put("pct", pct(((BigDecimal) row.get("amount")).longValue(), max));
        }
        return rows;
    }

    public List<Map<String, Object>> recentPayments() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : repo.recentPayments()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", num(r[0]));
            m.put("user", String.valueOf(r[1]));
            m.put("userId", num(r[4]));
            m.put("amount", new BigDecimal(r[2].toString()));
            m.put("date", r[3] == null ? "-" : r[3].toString().substring(0, 16).replace('T', ' '));
            out.add(m);
        }
        return out;
    }

    private static BigDecimal nzb(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }

    // ---------- helper ----------
    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> m = new HashMap<>();
        for (Object[] r : rows) m.put(String.valueOf(r[0]), num(r[1]));
        return m;
    }

    /** แปลงผล DATE(...) ของ native query (อาจเป็น java.sql.Date / LocalDate / String) */
    private static LocalDate toLocalDate(Object o) {
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        return LocalDate.parse(o.toString().substring(0, 10));
    }

    private static Map<LocalDate, Long> toDayMap(List<Object[]> rows) {
        Map<LocalDate, Long> m = new HashMap<>();
        for (Object[] r : rows) m.put(toLocalDate(r[0]), num(r[1]));
        return m;
    }

    /** รวมค่ารายวันที่อยู่ในช่วง b */
    private static long sumIn(Map<LocalDate, Long> perDay, Bucket b) {
        long sum = 0;
        for (LocalDate d = b.from(); !d.isAfter(b.to()); d = d.plusDays(1)) {
            sum += perDay.getOrDefault(d, 0L);
        }
        return sum;
    }

    private static long sumAll(Map<LocalDate, Long> perDay) {
        long sum = 0;
        for (long v : perDay.values()) sum += v;
        return sum;
    }

    /** 21 ก.ย. */
    private static String thDay(LocalDate d) {
        return d.getDayOfMonth() + " " + TH_MONTH_SHORT[d.getMonthValue() - 1];
    }

    /** 21–27 ก.ย. หรือ 28 ก.ย.–4 ต.ค. */
    private static String thSpan(LocalDate from, LocalDate to) {
        return from.getMonthValue() == to.getMonthValue()
                ? from.getDayOfMonth() + "–" + thDay(to)
                : thDay(from) + "–" + thDay(to);
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