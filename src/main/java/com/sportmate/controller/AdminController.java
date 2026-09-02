package com.sportmate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sportmate.entity.User;
import com.sportmate.service.AdminService;
import com.sportmate.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    public AdminController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    @GetMapping("/admin")
    public String dashboard(HttpSession session, Model model, RedirectAttributes ra) {
        User me = userService.getById((Integer) session.getAttribute("uid"));
        if (me == null || !me.isAdmin()) {
            ra.addFlashAttribute("error", "หน้านี้สำหรับผู้ดูแลระบบเท่านั้น");
            return "redirect:/home";
        }

        model.addAttribute("s", adminService.summary());
        model.addAttribute("trend", adminService.monthlyTrend());
        model.addAttribute("statusDonut", adminService.postStatusDonut());
        model.addAttribute("bySport", adminService.bySport());
        model.addAttribute("recentPosts", adminService.recentPosts());
        model.addAttribute("topOrganizers", adminService.topOrganizers());
        model.addAttribute("recentUsers", adminService.recentUsers());
        return "admin";
    }
}