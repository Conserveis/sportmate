package com.sportmate.controller;

import com.sportmate.entity.User;
import com.sportmate.service.NotificationService;
import com.sportmate.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    private User me(HttpSession session) {
        return userService.getById((Integer) session.getAttribute("uid"));
    }

    @GetMapping("/notifications")
    public String notifications(HttpSession session, Model model) {
        User me = me(session);
        model.addAttribute("notifications", notificationService.list(me));
        // เปิดหน้านี้ = ถือว่าอ่านทั้งหมดแล้ว
        notificationService.markAllRead(me);
        return "notifications";
    }

    @PostMapping("/notifications/read")
    public String markRead(HttpSession session) {
        notificationService.markAllRead(me(session));
        return "redirect:/notifications";
    }
}
