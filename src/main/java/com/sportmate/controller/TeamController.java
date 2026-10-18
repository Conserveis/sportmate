package com.sportmate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sportmate.entity.Post;
import com.sportmate.entity.Team;
import com.sportmate.entity.TeamMember;
import com.sportmate.entity.TournamentMatch;
import com.sportmate.entity.User;
import com.sportmate.repository.TeamMemberRepository;
import com.sportmate.repository.TournamentMatchRepository;
import com.sportmate.service.BracketService;
import com.sportmate.service.ChatService;
import com.sportmate.service.PostService;
import com.sportmate.service.TeamService;
import com.sportmate.service.UserService;
import com.sportmate.util.ErrorMessage;

import jakarta.servlet.http.HttpSession;

/** ทีม + สายการแข่งขัน + แชททีม ของทัวร์นาเมนต์ */
@Controller
public class TeamController {

    private final TeamService teamService;
    private final BracketService bracketService;
    private final ChatService chatService;
    private final PostService postService;
    private final UserService userService;
    private final TeamMemberRepository memberRepo;
    private final TournamentMatchRepository matchRepo;

    public TeamController(TeamService teamService, BracketService bracketService,
                          ChatService chatService, PostService postService, UserService userService,
                          TeamMemberRepository memberRepo, TournamentMatchRepository matchRepo) {
        this.teamService = teamService;
        this.bracketService = bracketService;
        this.chatService = chatService;
        this.postService = postService;
        this.userService = userService;
        this.memberRepo = memberRepo;
        this.matchRepo = matchRepo;
    }

    private User me(HttpSession session) {
        return userService.getById((Integer) session.getAttribute("uid"));
    }

    private static String teams(Integer postId) { return "redirect:/posts/" + postId + "#teams"; }

    // ---------------- หัวหน้าทีม: ขอสร้างทีม / ยกเลิกทีม ----------------

    @PostMapping("/posts/{id}/teams")
    public String requestTeam(@PathVariable Integer id, @RequestParam String teamName,
                              @RequestParam(required = false) String note,
                              HttpSession session, RedirectAttributes ra) {
        try {
            teamService.requestTeam(me(session), postService.getById(id), teamName, note);
            ra.addFlashAttribute("msg", "ส่งคำขอสร้างทีมแล้ว รอเจ้าของทัวร์นาเมนต์อนุมัติ");
        } catch (Exception e) {
            ra.addFlashAttribute("error", ErrorMessage.forUser(e));
        }
        return teams(id);
    }

    @PostMapping("/teams/{teamId}/cancel")
    public String cancelTeam(@PathVariable Integer teamId, HttpSession session, RedirectAttributes ra) {
        Team t = teamService.getById(teamId);
        try {
            teamService.cancelTeam(me(session), t);
            ra.addFlashAttribute("msg", "ยกเลิกทีมแล้ว");
        } catch (Exception e) {
            ra.addFlashAttribute("error", ErrorMessage.forUser(e));
        }
        return teams(t.getPost().getId());
    }

    // ---------------- เจ้าของทัวร์นาเมนต์: อนุมัติ / ปฏิเสธทีม ----------------

    @PostMapping("/teams/{teamId}/approve")
    public String approveTeam(@PathVariable Integer teamId, HttpSession session, RedirectAttributes ra) {
        Team t = teamService.getById(teamId);
        try {
            teamService.approveTeam(me(session), t);
            ra.addFlashAttribute("msg", "อนุมัติทีม \"" + t.getName() + "\" แล้ว");
        } catch (Exception e) {
            ra.addFlashAttribute("error", ErrorMessage.forUser(e));
        }
        return teams(t.getPost().getId());
    }

    @PostMapping("/teams/{teamId}/reject")
    public String rejectTeam(@PathVariable Integer teamId, HttpSession session, RedirectAttributes ra) {
        Team t = teamService.getById(teamId);
        try {
            teamService.rejectTeam(me(session), t);
            ra.addFlashAttribute("msg", "ปฏิเสธคำขอสร้างทีมแล้ว");
        } catch (Exception e) {
            ra.addFlashAttribute("error", ErrorMessage.forUser(e));
        }
        return teams(t.getPost().getId());
    }

    // ---------------- ผู้เล่น: ขอเข้าร่วมทีม / ออกจากทีม ----------------

    @PostMapping("/teams/{teamId}/join")
    public String join(@PathVariable Integer teamId, @RequestParam(required = false) String message,
                       HttpSession session, RedirectAttributes ra) {
        Team t = teamService.getById(teamId);
        try {
            teamService.requestJoin(me(session), t, message);
            ra.addFlashAttribute("msg", "ส่งคำขอเข้าร่วมทีม \"" + t.getName() + "\" แล้ว รอหัวหน้าทีมอนุมัติ");
        } catch (Exception e) {
            ra.addFlashAttribute("error", ErrorMessage.forUser(e));
        }
        return teams(t.getPost().getId());
    }

    @PostMapping("/teams/{teamId}/leave")
    public String leave(@PathVariable Integer teamId, HttpSession session, RedirectAttributes ra) {
        Team t = teamService.getById(teamId);
        try {
            teamService.leave(me(session), t);
            ra.addFlashAttribute("msg", "ออกจากทีมแล้ว");
        } catch (Exception e) {
            ra.addFlashAttribute("error", ErrorMessage.forUser(e));
        }
        return teams(t.getPost().getId());
    }

    // ---------------- หัวหน้าทีม: จัดการสมาชิก ----------------

    @PostMapping("/team-members/{memberId}/approve")
    public String approveMember(@PathVariable Integer memberId, HttpSession session, RedirectAttributes ra) {
        return memberAction(memberId, session, ra, "approve");
    }

    @PostMapping("/team-members/{memberId}/reject")
    public String rejectMember(@PathVariable Integer memberId, HttpSession session, RedirectAttributes ra) {
        return memberAction(memberId, session, ra, "reject");
    }

    @PostMapping("/team-members/{memberId}/remove")
    public String removeMember(@PathVariable Integer memberId, HttpSession session, RedirectAttributes ra) {
        return memberAction(memberId, session, ra, "remove");
    }

    private String memberAction(Integer memberId, HttpSession session, RedirectAttributes ra, String action) {
        TeamMember m = memberRepo.findById(memberId).orElse(null);
        if (m == null) {
            ra.addFlashAttribute("error", "ไม่พบคำขอนี้");
            return "redirect:/tournaments";
        }
        try {
            switch (action) {
                case "approve" -> {
                    teamService.approveMember(me(session), memberId);
                    ra.addFlashAttribute("msg", "อนุมัติ " + m.getUser().getUserName() + " เข้าทีมแล้ว");
                }
                case "reject" -> {
                    teamService.rejectMember(me(session), memberId);
                    ra.addFlashAttribute("msg", "ปฏิเสธคำขอแล้ว");
                }
                default -> {
                    teamService.removeMember(me(session), memberId);
                    ra.addFlashAttribute("msg", "นำ " + m.getUser().getUserName() + " ออกจากทีมแล้ว");
                }
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", ErrorMessage.forUser(e));
        }
        return teams(m.getTeam().getPost().getId());
    }

    // ---------------- แชททีม ----------------

    @PostMapping("/teams/{teamId}/chat")
    public String teamChat(@PathVariable Integer teamId, @RequestParam String text,
                           HttpSession session, RedirectAttributes ra) {
        Team t = teamService.getById(teamId);
        try {
            chatService.addTeamComment(me(session), t, text);
        } catch (Exception e) {
            ra.addFlashAttribute("error", ErrorMessage.forUser(e));
        }
        return "redirect:/posts/" + t.getPost().getId() + "?chat=team#chat";
    }

    // ---------------- สายการแข่งขัน ----------------

    @GetMapping("/posts/{id}/bracket")
    public String bracket(@PathVariable Integer id, HttpSession session, Model model) {
        Post p = postService.getById(id);
        if (!p.isTournament()) return "redirect:/posts/" + id;
        User me = me(session);
        boolean drawn = bracketService.isDrawn(p);
        model.addAttribute("post", p);
        model.addAttribute("isOwner", p.getOwner().getId().equals(me.getId()));
        model.addAttribute("drawn", drawn);
        if (drawn) {
            model.addAttribute("rounds", bracketService.rounds(p));
            model.addAttribute("summary", bracketService.summary(p));
            model.addAttribute("champion", bracketService.champion(p));
        }
        return "tournament-bracket";
    }

    @PostMapping("/posts/{id}/bracket/draw")
    public String draw(@PathVariable Integer id, @RequestParam(defaultValue = "random") String method,
                       HttpSession session, RedirectAttributes ra) {
        try {
            bracketService.draw(me(session), postService.getById(id), method);
            ra.addFlashAttribute("msg", "จับสายเรียบร้อยแล้ว");
            return "redirect:/posts/" + id + "/bracket";
        } catch (Exception e) {
            ra.addFlashAttribute("error", ErrorMessage.forUser(e));
            return teams(id);
        }
    }

    @PostMapping("/matches/{matchId}/result")
    public String result(@PathVariable Integer matchId,
                         @RequestParam(required = false) Integer scoreA,
                         @RequestParam(required = false) Integer scoreB,
                         @RequestParam(required = false) Integer winnerTeamId,
                         HttpSession session, RedirectAttributes ra) {
        TournamentMatch m = matchRepo.findById(matchId).orElse(null);
        if (m == null) {
            ra.addFlashAttribute("error", "ไม่พบคู่การแข่งขันนี้");
            return "redirect:/tournaments";
        }
        try {
            bracketService.recordResult(me(session), matchId, scoreA, scoreB, winnerTeamId);
            ra.addFlashAttribute("msg", "บันทึกผลแล้ว");
        } catch (Exception e) {
            ra.addFlashAttribute("error", ErrorMessage.forUser(e));
        }
        return "redirect:/posts/" + m.getPost().getId() + "/bracket#m" + matchId;
    }
}
