package com.sportmate.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sportmate.dto.TeamCard;
import com.sportmate.entity.Event;
import com.sportmate.entity.Post;
import com.sportmate.entity.Team;
import com.sportmate.entity.TeamMember;
import com.sportmate.entity.User;
import com.sportmate.repository.EventRepository;
import com.sportmate.repository.TeamMemberRepository;
import com.sportmate.repository.TeamRepository;
import com.sportmate.repository.TournamentMatchRepository;

/**
 * ระบบทีมของทัวร์นาเมนต์แบบทีม/คู่
 *
 * ขั้นตอน: หัวหน้าทีมขอสร้างทีม → เจ้าของทัวร์นาเมนต์อนุมัติ → ผู้ใช้อื่นขอเข้าร่วมทีม → หัวหน้าทีมอนุมัติ
 *
 * ทุกคนที่อยู่ในทีมที่อนุมัติแล้วจะมีแถว Event (approved) ของทัวร์นาเมนต์ด้วย
 * เพื่อให้ส่วนเดิมของระบบใช้ต่อได้ทันที: นับจำนวนผู้เข้าร่วม, แชทรวม, หน้าจัดเก็บกิจกรรม, รีวิว, สถิติแอดมิน
 */
@Service
public class TeamService {

    private final TeamRepository teamRepo;
    private final TeamMemberRepository memberRepo;
    private final EventRepository eventRepo;
    private final TournamentMatchRepository matchRepo;
    private final NotificationService notificationService;

    public TeamService(TeamRepository teamRepo, TeamMemberRepository memberRepo,
                       EventRepository eventRepo, TournamentMatchRepository matchRepo,
                       NotificationService notificationService) {
        this.teamRepo = teamRepo;
        this.memberRepo = memberRepo;
        this.eventRepo = eventRepo;
        this.matchRepo = matchRepo;
        this.notificationService = notificationService;
    }

    // =====================================================================
    // อ่านข้อมูล
    // =====================================================================

    public Team getById(Integer teamId) {
        return teamRepo.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบทีมนี้"));
    }

    /** การ์ดทีมที่อนุมัติแล้ว พร้อมจำนวนสมาชิก และสถานะของผู้ดู */
    public List<TeamCard> approvedTeamCards(Post post, User viewer) {
        Map<Integer, Long> counts = countMap(post);
        Optional<TeamMember> mine = viewer == null ? Optional.empty() : activeMembership(viewer, post);
        List<TeamCard> out = new ArrayList<>();
        for (Team t : teamRepo.findByPostAndStatusOrderByCreatedAtAsc(post, "approved")) {
            String vs = null;
            if (mine.isPresent() && mine.get().getTeam().getId().equals(t.getId())) {
                vs = "approved".equals(mine.get().getStatus()) ? "member" : "pending";
            }
            out.add(new TeamCard(t, counts.getOrDefault(t.getId(), 0L), post.getTeamSize(),
                    memberRepo.findApproved(t), vs));
        }
        return out;
    }

    /** คำขอสร้างทีมที่รอเจ้าของทัวร์นาเมนต์อนุมัติ */
    public List<Team> pendingTeams(Post post) {
        return teamRepo.findByPostAndStatusOrderByCreatedAtAsc(post, "pending");
    }

    public long countApprovedTeams(Post post) {
        return teamRepo.countByPostAndStatus(post, "approved");
    }

    /** แถวสมาชิกที่ยังใช้งานอยู่ของผู้ใช้ในทัวร์นาเมนต์นี้ (อยู่ในทีม หรือรออนุมัติ) */
    public Optional<TeamMember> activeMembership(User user, Post post) {
        if (user == null) return Optional.empty();
        List<TeamMember> list = memberRepo.findActiveInPost(user, post);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** ทีมที่ผู้ใช้เป็นหัวหน้า (รออนุมัติ หรืออนุมัติแล้ว) */
    public Optional<Team> captainTeam(User user, Post post) {
        return activeMembership(user, post)
                .map(TeamMember::getTeam)
                .filter(t -> t.getCaptain().getId().equals(user.getId()));
    }

    public List<TeamMember> members(Team team) { return memberRepo.findApproved(team); }

    public List<TeamMember> pendingMembers(Team team) {
        return memberRepo.findByTeamAndStatusOrderByJoinDateAsc(team, "pending");
    }

    public long memberCount(Team team) { return memberRepo.countByTeamAndStatus(team, "approved"); }

    /** สมาชิกที่อนุมัติแล้วของทีมนี้ไหม (ใช้กับแชททีม) */
    public boolean isApprovedMember(User user, Team team) {
        if (user == null || team == null) return false;
        return memberRepo.findByTeamAndUser(team, user)
                .map(m -> "approved".equals(m.getStatus()))
                .orElse(false);
    }

    /** จำนวนสมาชิกที่อนุมัติแล้วของแต่ละทีมในโพสต์ (TeamID -> count) */
    public Map<Integer, Long> countMap(Post post) {
        Map<Integer, Long> m = new HashMap<>();
        for (Object[] r : memberRepo.countApprovedByTeam(post)) {
            m.put(((Number) r[0]).intValue(), ((Number) r[1]).longValue());
        }
        return m;
    }

    // =====================================================================
    // หัวหน้าทีม: ขอสร้างทีม
    // =====================================================================

    @Transactional
    public Team requestTeam(User user, Post post, String name, String note) {
        checkRegistrationOpen(post);
        if (!post.isTeamTournament())
            throw new IllegalStateException("ทัวร์นาเมนต์นี้แข่งแบบเดี่ยว ไม่ต้องสร้างทีม");
        if (post.getOwner().getId().equals(user.getId()))
            throw new IllegalStateException("เจ้าของทัวร์นาเมนต์สร้างทีมในทัวร์นาเมนต์ของตัวเองไม่ได้");
        if (activeMembership(user, post).isPresent())
            throw new IllegalStateException("คุณอยู่ในทีม หรือส่งคำขอในทัวร์นาเมนต์นี้ไว้แล้ว");

        String clean = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (clean.isEmpty()) throw new IllegalArgumentException("กรุณาตั้งชื่อทีม");
        if (clean.length() > 40) throw new IllegalArgumentException("ชื่อทีมยาวได้ไม่เกิน 40 ตัวอักษร");
        if (teamRepo.existsActiveName(post, clean))
            throw new IllegalArgumentException("มีทีมชื่อ \"" + clean + "\" ในทัวร์นาเมนต์นี้แล้ว");
        if (countApprovedTeams(post) >= post.getMaxTeams())
            throw new IllegalStateException("ทัวร์นาเมนต์นี้รับทีมครบ " + post.getMaxTeams() + " ทีมแล้ว");

        Team t = new Team();
        t.setPost(post);
        t.setCaptain(user);
        t.setName(clean);
        t.setNote(trimOrNull(note, 300));
        t.setStatus("pending");
        t.setCreatedAt(LocalDateTime.now());
        teamRepo.save(t);

        // หัวหน้าทีมเป็นสมาชิกคนแรก (นับรวมในจำนวนสมาชิก)
        TeamMember m = new TeamMember();
        m.setTeam(t);
        m.setUser(user);
        m.setStatus("approved");
        m.setJoinDate(LocalDateTime.now());
        memberRepo.save(m);

        notificationService.push(post.getOwner(),
                user.getUserName() + " ขอสร้างทีม \"" + clean + "\" ใน \"" + post.getPostName() + "\" (รอคุณอนุมัติ)",
                link(post), "team_request");
        return t;
    }

    /** หัวหน้าทีมยกเลิกทีม (ทั้งตอนรออนุมัติ และหลังอนุมัติ) */
    @Transactional
    public void cancelTeam(User user, Team team) {
        Post post = team.getPost();
        if (!team.getCaptain().getId().equals(user.getId()))
            throw new IllegalStateException("เฉพาะหัวหน้าทีมเท่านั้นที่ยกเลิกทีมได้");
        if (!team.isPending() && !team.isApproved())
            throw new IllegalStateException("ทีมนี้ถูกยกเลิกหรือปฏิเสธไปแล้ว");
        if (isDrawn(post))
            throw new IllegalStateException("จับสายการแข่งขันแล้ว ยกเลิกทีมไม่ได้");
        if (team.isApproved()) checkCancelDeadline(post);

        boolean wasApproved = team.isApproved();
        team.setStatus("cancelled");
        teamRepo.save(team);

        for (TeamMember m : memberRepo.findByTeamAndStatusOrderByJoinDateAsc(team, "approved")) {
            closeMember(m, "cancelled");
            if (!m.isCaptain()) {
                notificationService.push(m.getUser(),
                        "ทีม \"" + team.getName() + "\" ถูกยกเลิกโดยหัวหน้าทีม", link(post), "team_cancelled");
            }
        }
        for (TeamMember m : memberRepo.findByTeamAndStatusOrderByJoinDateAsc(team, "pending")) {
            closeMember(m, "rejected");
        }
        if (wasApproved) {
            notificationService.push(post.getOwner(),
                    "ทีม \"" + team.getName() + "\" ยกเลิกการเข้าร่วม \"" + post.getPostName() + "\"",
                    link(post), "team_cancelled");
        }
    }

    // =====================================================================
    // เจ้าของทัวร์นาเมนต์: อนุมัติ / ปฏิเสธทีม
    // =====================================================================

    @Transactional
    public void approveTeam(User owner, Team team) {
        Post post = team.getPost();
        checkOwner(owner, post);
        if (!team.isPending()) throw new IllegalStateException("ทีมนี้ไม่ได้อยู่ในสถานะรออนุมัติ");
        checkRegistrationOpen(post);
        if (countApprovedTeams(post) >= post.getMaxTeams())
            throw new IllegalStateException("รับทีมครบ " + post.getMaxTeams() + " ทีมแล้ว อนุมัติเพิ่มไม่ได้");

        team.setStatus("approved");
        teamRepo.save(team);

        // สมาชิกที่อนุมัติแล้ว (ตอนนี้มีหัวหน้าทีมคนเดียว) ได้สิทธิ์เข้าร่วมทัวร์นาเมนต์
        for (TeamMember m : memberRepo.findApproved(team)) {
            ensureEvent(m.getUser(), post);
        }
        notificationService.push(team.getCaptain(),
                "ทีม \"" + team.getName() + "\" ได้รับอนุมัติแล้ว เปิดรับสมาชิกได้เลย", link(post), "team_approved");
    }

    @Transactional
    public void rejectTeam(User owner, Team team) {
        Post post = team.getPost();
        checkOwner(owner, post);
        if (!team.isPending()) throw new IllegalStateException("ทีมนี้ไม่ได้อยู่ในสถานะรออนุมัติ");

        team.setStatus("rejected");
        teamRepo.save(team);
        for (TeamMember m : memberRepo.findApproved(team)) closeMember(m, "rejected");

        notificationService.push(team.getCaptain(),
                "คำขอสร้างทีม \"" + team.getName() + "\" ไม่ได้รับการอนุมัติ", link(post), "team_rejected");
    }

    // =====================================================================
    // ผู้เล่น: ขอเข้าร่วมทีม / ออกจากทีม
    // =====================================================================

    @Transactional
    public void requestJoin(User user, Team team, String message) {
        Post post = team.getPost();
        checkRegistrationOpen(post);
        if (!team.isApproved()) throw new IllegalStateException("ทีมนี้ยังไม่ได้รับอนุมัติจากเจ้าของทัวร์นาเมนต์");
        if (post.getOwner().getId().equals(user.getId()))
            throw new IllegalStateException("เจ้าของทัวร์นาเมนต์เข้าร่วมทีมไม่ได้");
        if (activeMembership(user, post).isPresent())
            throw new IllegalStateException("คุณอยู่ในทีม หรือส่งคำขอในทัวร์นาเมนต์นี้ไว้แล้ว (ขอได้ครั้งละ 1 ทีม)");
        if (memberCount(team) >= post.getTeamSize())
            throw new IllegalStateException("ทีมนี้สมาชิกเต็มแล้ว");

        TeamMember m = memberRepo.findByTeamAndUser(team, user).orElseGet(TeamMember::new);
        m.setTeam(team);
        m.setUser(user);
        m.setMessage(trimOrNull(message, 300));
        m.setStatus("pending");
        m.setJoinDate(LocalDateTime.now());
        m.setCancelDate(null);
        memberRepo.save(m);

        notificationService.push(team.getCaptain(),
                user.getUserName() + " ขอเข้าร่วมทีม \"" + team.getName() + "\" (รอคุณอนุมัติ)",
                link(post), "team_join_request");
    }

    /** ผู้เล่นยกเลิกคำขอ หรือออกจากทีม (หัวหน้าทีมต้องใช้ "ยกเลิกทีม" แทน) */
    @Transactional
    public void leave(User user, Team team) {
        Post post = team.getPost();
        TeamMember m = memberRepo.findByTeamAndUser(team, user)
                .orElseThrow(() -> new IllegalStateException("คุณไม่ได้อยู่ในทีมนี้"));
        if (m.isCaptain())
            throw new IllegalStateException("หัวหน้าทีมออกจากทีมไม่ได้ ใช้ปุ่มยกเลิกทีมแทน");

        if ("pending".equals(m.getStatus())) {
            closeMember(m, "cancelled");
            return;
        }
        if (!"approved".equals(m.getStatus())) throw new IllegalStateException("คุณไม่ได้อยู่ในทีมนี้");
        if (isDrawn(post)) throw new IllegalStateException("จับสายการแข่งขันแล้ว ออกจากทีมไม่ได้");
        checkCancelDeadline(post);

        closeMember(m, "cancelled");
        notificationService.push(team.getCaptain(),
                user.getUserName() + " ออกจากทีม \"" + team.getName() + "\" (เหลือ "
                        + memberCount(team) + "/" + post.getTeamSize() + " คน)",
                link(post), "team_leave");
    }

    // =====================================================================
    // หัวหน้าทีม: อนุมัติ / ปฏิเสธ / นำสมาชิกออก
    // =====================================================================

    @Transactional
    public void approveMember(User captain, Integer memberId) {
        TeamMember m = getMember(memberId);
        Team team = m.getTeam();
        Post post = team.getPost();
        checkCaptain(captain, team);
        if (!"pending".equals(m.getStatus())) throw new IllegalStateException("คำขอนี้ไม่ได้อยู่ในสถานะรออนุมัติ");
        checkRegistrationOpen(post);
        if (memberCount(team) >= post.getTeamSize()) throw new IllegalStateException("ทีมสมาชิกเต็มแล้ว");
        // กันกรณีผู้ใช้ไปได้ทีมอื่นระหว่างรอ
        for (TeamMember other : memberRepo.findActiveInPost(m.getUser(), post)) {
            if (!other.getId().equals(m.getId()) && "approved".equals(other.getStatus()))
                throw new IllegalStateException(m.getUser().getUserName() + " อยู่ในทีมอื่นแล้ว");
        }

        m.setStatus("approved");
        memberRepo.save(m);
        ensureEvent(m.getUser(), post);

        notificationService.push(m.getUser(),
                "หัวหน้าทีมอนุมัติให้คุณเข้าร่วมทีม \"" + team.getName() + "\" แล้ว", link(post), "team_join_approved");

        // ทีมเต็ม → ปฏิเสธคำขอที่เหลืออัตโนมัติ
        if (memberCount(team) >= post.getTeamSize()) {
            for (TeamMember p : pendingMembers(team)) {
                closeMember(p, "rejected");
                notificationService.push(p.getUser(),
                        "ทีม \"" + team.getName() + "\" สมาชิกเต็มแล้ว ลองขอเข้าร่วมทีมอื่นได้", link(post), "team_join_rejected");
            }
        }
    }

    @Transactional
    public void rejectMember(User captain, Integer memberId) {
        TeamMember m = getMember(memberId);
        Team team = m.getTeam();
        checkCaptain(captain, team);
        if (!"pending".equals(m.getStatus())) throw new IllegalStateException("คำขอนี้ไม่ได้อยู่ในสถานะรออนุมัติ");

        closeMember(m, "rejected");
        notificationService.push(m.getUser(),
                "คำขอเข้าร่วมทีม \"" + team.getName() + "\" ไม่ได้รับการอนุมัติ", link(team.getPost()), "team_join_rejected");
    }

    @Transactional
    public void removeMember(User captain, Integer memberId) {
        TeamMember m = getMember(memberId);
        Team team = m.getTeam();
        Post post = team.getPost();
        checkCaptain(captain, team);
        if (m.isCaptain()) throw new IllegalStateException("นำหัวหน้าทีมออกไม่ได้");
        if (!"approved".equals(m.getStatus())) throw new IllegalStateException("ผู้ใช้นี้ไม่ได้อยู่ในทีม");
        if (isDrawn(post)) throw new IllegalStateException("จับสายการแข่งขันแล้ว นำสมาชิกออกไม่ได้");
        checkCancelDeadline(post);

        closeMember(m, "cancelled");
        notificationService.push(m.getUser(),
                "คุณถูกนำออกจากทีม \"" + team.getName() + "\"", link(post), "team_removed");
    }

    // =====================================================================
    // helper
    // =====================================================================

    public boolean isDrawn(Post post) { return matchRepo.existsByPost(post); }

    /** ปิดสถานะสมาชิก + ยกเลิกสิทธิ์เข้าร่วมทัวร์นาเมนต์ (Event) ถ้าเคยได้ */
    private void closeMember(TeamMember m, String status) {
        boolean wasApproved = "approved".equals(m.getStatus());
        m.setStatus(status);
        m.setCancelDate(LocalDateTime.now());
        memberRepo.save(m);
        if (wasApproved) cancelEvent(m.getUser(), m.getTeam().getPost());
    }

    /** ให้ผู้ใช้มีแถว Event (approved) ของทัวร์นาเมนต์ */
    private void ensureEvent(User user, Post post) {
        Event e = eventRepo.findByUserAndPost(user, post).orElseGet(Event::new);
        if ("approved".equals(e.getStatus()) && e.getId() != null) return;
        e.setUser(user);
        e.setPost(post);
        e.setEventName(post.getPostName());
        e.setStatus("approved");
        e.setJoinDate(LocalDateTime.now());
        e.setCancelDate(null);
        eventRepo.save(e);
    }

    private void cancelEvent(User user, Post post) {
        eventRepo.findByUserAndPost(user, post).ifPresent(e -> {
            if ("approved".equals(e.getStatus()) || "pending".equals(e.getStatus())) {
                e.setStatus("cancelled");
                e.setCancelDate(LocalDateTime.now());
                eventRepo.save(e);
            }
        });
    }

    private TeamMember getMember(Integer memberId) {
        return memberRepo.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบคำขอนี้"));
    }

    private void checkOwner(User user, Post post) {
        if (!post.getOwner().getId().equals(user.getId()))
            throw new IllegalStateException("เฉพาะเจ้าของทัวร์นาเมนต์เท่านั้น");
    }

    private void checkCaptain(User user, Team team) {
        if (!team.getCaptain().getId().equals(user.getId()))
            throw new IllegalStateException("เฉพาะหัวหน้าทีมเท่านั้น");
        if (!team.isApproved() && !team.isPending())
            throw new IllegalStateException("ทีมนี้ถูกยกเลิกหรือปฏิเสธไปแล้ว");
    }

    /** เปิดรับสมัครอยู่ไหม: ยังไม่ยกเลิก ยังไม่หมดเวลา และยังไม่จับสาย */
    private void checkRegistrationOpen(Post post) {
        if (!post.isTournament()) throw new IllegalStateException("โพสต์นี้ไม่ใช่ทัวร์นาเมนต์");
        if ("cancelled".equals(post.getStatus())) throw new IllegalStateException("ทัวร์นาเมนต์นี้ถูกยกเลิกแล้ว");
        if (post.isExpired()) throw new IllegalStateException("ทัวร์นาเมนต์นี้หมดเวลารับสมัครแล้ว");
        if (isDrawn(post)) throw new IllegalStateException("จับสายการแข่งขันแล้ว ปิดรับสมัคร");
    }

    /** เกณฑ์เดิม: ยกเลิกได้ถึง 24 ชม. ก่อนเวลานัด (ถ้าทัวร์นาเมนต์ถูกยกเลิก ไม่ติดเส้นตาย) */
    private void checkCancelDeadline(Post post) {
        if (!"cancelled".equals(post.getStatus()) && post.isCancelLocked()) {
            throw new IllegalStateException("ยกเลิกไม่ได้แล้ว ต้องยกเลิกก่อนเวลาแข่งอย่างน้อย 24 ชั่วโมง");
        }
    }

    private static String trimOrNull(String s, int max) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static String link(Post post) { return "/posts/" + post.getId() + "#teams"; }
}
