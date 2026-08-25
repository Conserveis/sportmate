package com.sportmate.controller;

import com.sportmate.entity.Event;
import com.sportmate.entity.User;
import com.sportmate.service.EventService;
import com.sportmate.service.ReviewService;
import com.sportmate.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ArchiveController {

    private final EventService eventService;
    private final ReviewService reviewService;
    private final UserService userService;

    public ArchiveController(EventService eventService, ReviewService reviewService,
                             UserService userService) {
        this.eventService = eventService;
        this.reviewService = reviewService;
        this.userService = userService;
    }

    private User me(HttpSession session) {
        return userService.getById((Integer) session.getAttribute("uid"));
    }

    @GetMapping("/archive")
    public String archive(HttpSession session, Model model) {
        User me = me(session);
        List<Event> archived = eventService.archivedByUser(me);
        Map<Integer, Boolean> reviewed = new HashMap<>();
        for (Event e : archived) reviewed.put(e.getId(), reviewService.hasReviewed(e.getId()));
        model.addAttribute("events", archived);
        model.addAttribute("reviewed", reviewed);
        return "archive";
    }

    // รีวิวผู้จัด (จากหน้าจัดเก็บกิจกรรม)
    @PostMapping("/archive/review")
    public String review(@RequestParam Integer eventId,
                         @RequestParam int score,
                         @RequestParam(required = false) String comment,
                         HttpSession session, RedirectAttributes ra) {
        try {
            reviewService.review(me(session), eventId, score, comment);
            ra.addFlashAttribute("msg", "ส่งรีวิวสำเร็จ");
        } catch (Exception e) {
            ra.addFlashAttribute("error", ErrorMessage.forUser(e));
        }
        return "redirect:/archive";
    }
}
