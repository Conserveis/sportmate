package com.sportmate.controller;

import com.sportmate.entity.User;
import com.sportmate.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session, Model model) {
        try {
            User u = userService.login(username, password);
            session.setAttribute("uid", u.getId());
            return "redirect:/posts";
        } catch (UserService.UnverifiedUserException ue) {
            // บัญชียังไม่ยืนยัน OTP -> พาไปหน้ายืนยัน
            session.setAttribute("pendingUserId", ue.getUserId());
            userService.resendOtp(ue.getUserId());
            return "redirect:/verify";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String gmail,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           HttpSession session, Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "รหัสผ่านและการยืนยันไม่ตรงกัน");
            model.addAttribute("username", username);
            model.addAttribute("gmail", gmail);
            return "register";
        }
        try {
            User u = userService.register(username, gmail, password);
            // ยังไม่ล็อกอิน — ต้องยืนยัน OTP ก่อน
            session.setAttribute("pendingUserId", u.getId());
            return "redirect:/verify";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("gmail", gmail);
            return "register";
        }
    }

    // ---- ยืนยัน OTP (UC-1 / FR02) ----
    @GetMapping("/verify")
    public String verifyPage(HttpSession session, Model model) {
        Integer pendingId = (Integer) session.getAttribute("pendingUserId");
        if (pendingId == null) return "redirect:/register";
        User u = userService.getById(pendingId);
        model.addAttribute("gmail", u.getGmail());
        // โหมดจำลอง: แสดง OTP บนหน้าจอ (ระบบจริงจะส่งทางอีเมล ไม่แสดงตรงนี้)
        model.addAttribute("devOtp", u.getOtpCode());
        return "verify";
    }

    @PostMapping("/verify")
    public String verify(@RequestParam String otp, HttpSession session, Model model) {
        Integer pendingId = (Integer) session.getAttribute("pendingUserId");
        if (pendingId == null) return "redirect:/register";
        try {
            userService.verifyOtp(pendingId, otp);
            session.removeAttribute("pendingUserId");
            session.setAttribute("uid", pendingId);   // ยืนยันสำเร็จ -> ล็อกอินเลย
            return "redirect:/posts";
        } catch (Exception e) {
            User u = userService.getById(pendingId);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("gmail", u.getGmail());
            model.addAttribute("devOtp", u.getOtpCode());
            return "verify";
        }
    }

    @PostMapping("/verify/resend")
    public String resend(HttpSession session) {
        Integer pendingId = (Integer) session.getAttribute("pendingUserId");
        if (pendingId == null) return "redirect:/register";
        userService.resendOtp(pendingId);
        return "redirect:/verify";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/posts";
    }
}
