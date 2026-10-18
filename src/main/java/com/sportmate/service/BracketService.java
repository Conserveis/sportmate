package com.sportmate.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sportmate.entity.Event;
import com.sportmate.entity.Post;
import com.sportmate.entity.Team;
import com.sportmate.entity.TeamMember;
import com.sportmate.entity.TournamentMatch;
import com.sportmate.entity.User;
import com.sportmate.repository.EventRepository;
import com.sportmate.repository.TeamMemberRepository;
import com.sportmate.repository.TeamRepository;
import com.sportmate.repository.TournamentMatchRepository;

/**
 * สายการแข่งขันแบบแพ้คัดออก (single elimination)
 *
 * จับสาย: ปัดจำนวนผู้เข้าแข่ง N ขึ้นเป็นกำลังของ 2 (size) → ช่องที่เหลือ (size - N) คือ "บาย"
 * ทีมที่ได้บายเข้ารอบ 2 อัตโนมัติ เพราะ N > size/2 เสมอ บายจึงมีแค่รอบแรก ไม่มีใครได้บายซ้ำ
 *
 * กีฬาเดี่ยว: สร้างทีม 1 คนให้ผู้เข้าร่วมแต่ละคนตอนจับสาย แล้วใช้ตารางเดียวกัน
 */
@Service
public class BracketService {

    private final TournamentMatchRepository matchRepo;
    private final TeamRepository teamRepo;
    private final TeamMemberRepository memberRepo;
    private final EventRepository eventRepo;
    private final NotificationService notificationService;

    public BracketService(TournamentMatchRepository matchRepo, TeamRepository teamRepo,
                          TeamMemberRepository memberRepo, EventRepository eventRepo,
                          NotificationService notificationService) {
        this.matchRepo = matchRepo;
        this.teamRepo = teamRepo;
        this.memberRepo = memberRepo;
        this.eventRepo = eventRepo;
        this.notificationService = notificationService;
    }

    public boolean isDrawn(Post post) { return matchRepo.existsByPost(post); }

    public List<TournamentMatch> matches(Post post) { return matchRepo.findByPostOrderByRoundAscSlotAsc(post); }

    // =====================================================================
    // จับสาย
    // =====================================================================

    /** @param method "random" = สุ่ม, "order" = ตามลำดับที่สมัคร (สมัครก่อนได้บายก่อน) */
    @Transactional
    public void draw(User owner, Post post, String method) {
        if (!post.isTournament()) throw new IllegalStateException("โพสต์นี้ไม่ใช่ทัวร์นาเมนต์");
        if (!post.getOwner().getId().equals(owner.getId()))
            throw new IllegalStateException("เฉพาะเจ้าของทัวร์นาเมนต์เท่านั้นที่จับสายได้");
        if ("cancelled".equals(post.getStatus())) throw new IllegalStateException("ทัวร์นาเมนต์นี้ถูกยกเลิกแล้ว");
        if (isDrawn(post)) throw new IllegalStateException("จับสายไปแล้ว");

        List<Team> entrants = post.isTeamTournament()
                ? new ArrayList<>(teamRepo.findByPostAndStatusOrderByCreatedAtAsc(post, "approved"))
                : soloEntrants(post);
        if (entrants.size() < 2) {
            throw new IllegalStateException("ต้องมีผู้เข้าแข่งอย่างน้อย 2 " + (post.isTeamTournament() ? "ทีม" : "คน") + " จึงจะจับสายได้");
        }
        if ("random".equals(method)) Collections.shuffle(entrants);

        List<TournamentMatch> all = buildBracket(post, entrants);
        matchRepo.saveAll(all);

        String msg = "จับสาย \"" + post.getPostName() + "\" แล้ว ดูคู่แข่งของคุณได้เลย";
        for (Event e : eventRepo.findParticipants(post)) {
            if ("approved".equals(e.getStatus())) {
                notificationService.push(e.getUser(), msg, "/posts/" + post.getId() + "/bracket", "bracket_drawn");
            }
        }
    }

    /**
     * สร้างคู่ทั้งสาย (แยกออกมาให้ทดสอบได้โดยไม่ต้องมี DB)
     * รอบแรก: คู่ที่ 0..byes-1 เป็นบาย, ที่เหลือจับคู่กันตามลำดับ
     */
    static List<TournamentMatch> buildBracket(Post post, List<Team> entrants) {
        int n = entrants.size();
        int size = 1;
        while (size < n) size *= 2;
        int rounds = Integer.numberOfTrailingZeros(size);
        int byes = size - n;

        Map<String, TournamentMatch> byKey = new LinkedHashMap<>();
        for (int r = 1; r <= rounds; r++) {
            int count = size >> r;
            for (int s = 0; s < count; s++) {
                TournamentMatch m = new TournamentMatch();
                m.setPost(post);
                m.setRound(r);
                m.setSlot(s);
                m.setStatus("waiting");
                byKey.put(r + ":" + s, m);
            }
        }

        int idx = 0;
        int firstRoundMatches = size / 2;
        for (int s = 0; s < firstRoundMatches; s++) {
            TournamentMatch m = byKey.get("1:" + s);
            if (s < byes) {
                m.setTeamA(entrants.get(idx++));
                m.setStatus("bye");
                m.setWinner(m.getTeamA());
            } else {
                m.setTeamA(entrants.get(idx++));
                m.setTeamB(entrants.get(idx++));
                m.setStatus("ready");
            }
        }
        // ส่งผู้ชนะจากบายขึ้นรอบถัดไป
        for (int s = 0; s < byes; s++) {
            advance(byKey, byKey.get("1:" + s), rounds);
        }
        return new ArrayList<>(byKey.values());
    }

    /** วางผู้ชนะของคู่ m ลงคู่ถัดไป แล้วอัปเดตสถานะคู่นั้น */
    private static void advance(Map<String, TournamentMatch> byKey, TournamentMatch m, int rounds) {
        if (m.getRound() >= rounds) return;
        TournamentMatch next = byKey.get((m.getRound() + 1) + ":" + (m.getSlot() / 2));
        placeWinner(m, next);
    }

    private static void placeWinner(TournamentMatch from, TournamentMatch next) {
        if (from.getSlot() % 2 == 0) next.setTeamA(from.getWinner());
        else next.setTeamB(from.getWinner());
        next.setStatus(next.getTeamA() != null && next.getTeamB() != null ? "ready" : "waiting");
    }

    /** กีฬาเดี่ยว: ผู้เข้าร่วมที่อนุมัติแล้วแต่ละคน = ทีม 1 คน */
    private List<Team> soloEntrants(Post post) {
        List<Team> out = new ArrayList<>();
        for (Event e : eventRepo.findParticipants(post)) {
            if (!"approved".equals(e.getStatus())) continue;
            Team t = new Team();
            t.setPost(post);
            t.setCaptain(e.getUser());
            t.setName(e.getUser().getUserName());
            t.setStatus("approved");
            t.setCreatedAt(e.getJoinDate() == null ? LocalDateTime.now() : e.getJoinDate());
            teamRepo.save(t);

            TeamMember m = new TeamMember();
            m.setTeam(t);
            m.setUser(e.getUser());
            m.setStatus("approved");
            memberRepo.save(m);
            out.add(t);
        }
        return out;
    }

    // =====================================================================
    // กรอกผล
    // =====================================================================

    /**
     * บันทึกผล 1 คู่ แล้วส่งผู้ชนะขึ้นรอบถัดไป
     * @param winnerTeamId ใช้เมื่อสกอร์เสมอ (เช่นตัดสินด้วยจุดโทษ)
     */
    @Transactional
    public void recordResult(User owner, Integer matchId, Integer scoreA, Integer scoreB, Integer winnerTeamId) {
        TournamentMatch m = matchRepo.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบคู่การแข่งขันนี้"));
        Post post = m.getPost();
        if (!post.getOwner().getId().equals(owner.getId()))
            throw new IllegalStateException("เฉพาะเจ้าของทัวร์นาเมนต์เท่านั้นที่กรอกผลได้");
        if (m.isBye()) throw new IllegalStateException("คู่นี้เป็นบาย ไม่ต้องกรอกผล");
        if (m.getTeamA() == null || m.getTeamB() == null)
            throw new IllegalStateException("คู่นี้ยังรอผู้ชนะจากรอบก่อน");
        if (scoreA == null || scoreB == null || scoreA < 0 || scoreB < 0)
            throw new IllegalArgumentException("กรุณากรอกสกอร์เป็นตัวเลขตั้งแต่ 0 ขึ้นไป");

        int rounds = maxRound(post);
        TournamentMatch next = m.getRound() < rounds
                ? matchRepo.findByPostAndRoundAndSlot(post, m.getRound() + 1, m.getSlot() / 2).orElse(null)
                : null;
        if (next != null && next.isDone())
            throw new IllegalStateException("แก้ผลคู่นี้ไม่ได้ เพราะรอบถัดไปบันทึกผลไปแล้ว");

        Team winner;
        if (scoreA > scoreB) winner = m.getTeamA();
        else if (scoreB > scoreA) winner = m.getTeamB();
        else {
            if (winnerTeamId == null)
                throw new IllegalArgumentException("ผลเสมอ กรุณาเลือกทีมที่ชนะ");
            if (winnerTeamId.equals(m.getTeamA().getId())) winner = m.getTeamA();
            else if (winnerTeamId.equals(m.getTeamB().getId())) winner = m.getTeamB();
            else throw new IllegalArgumentException("ทีมที่เลือกไม่ได้อยู่ในคู่นี้");
        }

        m.setScoreA(scoreA);
        m.setScoreB(scoreB);
        m.setWinner(winner);
        m.setStatus("done");
        m.setPlayedAt(LocalDateTime.now());
        matchRepo.save(m);

        if (next != null) {
            placeWinner(m, next);
            matchRepo.save(next);
        } else {
            for (TeamMember tm : memberRepo.findApproved(winner)) {
                notificationService.push(tm.getUser(),
                        "ยินดีด้วย! " + winner.getName() + " เป็นแชมป์ \"" + post.getPostName() + "\"",
                        "/posts/" + post.getId() + "/bracket", "champion");
            }
        }
    }

    // =====================================================================
    // ข้อมูลสำหรับหน้าแสดงสาย
    // =====================================================================

    /** แยกคู่ตามรอบ พร้อมชื่อรอบ: [{title, sub, matches}] */
    public List<Map<String, Object>> rounds(Post post) {
        List<TournamentMatch> all = matches(post);
        int rounds = 0;
        for (TournamentMatch m : all) rounds = Math.max(rounds, m.getRound());
        Map<Integer, List<TournamentMatch>> byRound = new HashMap<>();
        for (TournamentMatch m : all) byRound.computeIfAbsent(m.getRound(), k -> new ArrayList<>()).add(m);

        String unit = post.isTeamTournament() ? "ทีม" : "คน";
        List<Map<String, Object>> out = new ArrayList<>();
        for (int r = 1; r <= rounds; r++) {
            List<TournamentMatch> list = byRound.getOrDefault(r, List.of());
            int left = list.size() * 2;
            Map<String, Object> row = new HashMap<>();
            row.put("round", r);
            row.put("title", roundTitle(r, rounds));
            row.put("sub", left + " " + unit + " · " + list.size() + " คู่");
            row.put("matches", list);
            out.add(row);
        }
        return out;
    }

    /** แชมป์ (ผู้ชนะคู่ชิง) — ยังไม่จบ = null */
    public Team champion(Post post) {
        TournamentMatch fin = null;
        for (TournamentMatch m : matches(post)) {
            if (fin == null || m.getRound() > fin.getRound()) fin = m;
        }
        return (fin != null && fin.isDone()) ? fin.getWinner() : null;
    }

    public Map<String, Object> summary(Post post) {
        List<TournamentMatch> all = matches(post);
        int byes = 0, done = 0, total = 0, rounds = 0;
        for (TournamentMatch m : all) {
            rounds = Math.max(rounds, m.getRound());
            if (m.isBye()) byes++;
            else {
                total++;
                if (m.isDone()) done++;
            }
        }
        Map<String, Object> s = new HashMap<>();
        s.put("entrants", (1 << rounds) - byes);
        s.put("size", 1 << rounds);
        s.put("byes", byes);
        s.put("rounds", rounds);
        s.put("done", done);
        s.put("total", total);
        return s;
    }

    /** ตัวเลขพรีวิวก่อนจับสาย: จำนวนผู้เข้าแข่ง N → ขนาดสาย / บาย / คู่ในรอบแรก */
    public static Map<String, Integer> preview(int n) {
        Map<String, Integer> p = new HashMap<>();
        int size = 1;
        while (size < Math.max(n, 2)) size *= 2;
        p.put("entrants", n);
        p.put("size", size);
        p.put("rounds", Integer.numberOfTrailingZeros(size));
        p.put("byes", n < 2 ? 0 : size - n);
        p.put("firstMatches", n < 2 ? 0 : n - size / 2);
        return p;
    }

    private int maxRound(Post post) {
        int r = 0;
        for (TournamentMatch m : matches(post)) r = Math.max(r, m.getRound());
        return r;
    }

    private static String roundTitle(int r, int rounds) {
        int fromEnd = rounds - r;
        if (fromEnd == 0) return "ชิงชนะเลิศ";
        if (fromEnd == 1) return "รอบรองชนะเลิศ";
        if (fromEnd == 2) return "รอบก่อนรองชนะเลิศ";
        if (r == 1) return "รอบแรก";
        return "รอบ " + (1 << (fromEnd + 1)) + " ทีม";
    }
}
