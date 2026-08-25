package com.sportmate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sportmate.entity.User;
import com.sportmate.service.UserService;

import jakarta.servlet.http.HttpSession;

import com.sportmate.util.ErrorMessage;

@Controller
public class AuthController {

    private final UserService userService;
    private final org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrations;

    public AuthController(UserService userService,
                          org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrations) {
        this.userService = userService;
        this.clientRegistrations = clientRegistrations;
    }

    /** ข้อมูลปุ่มผู้ให้บริการที่ตั้งค่าไว้จริง — ตัวที่ยังไม่ตั้งค่าจะไม่ขึ้นปุ่ม */
    private java.util.List<java.util.Map<String, String>> availableProviders() {
    var result = new java.util.ArrayList<java.util.Map<String, String>>();
    if (clientRegistrations instanceof Iterable<?> iterable) {
        for (Object o : iterable) {
            if (o instanceof org.springframework.security.oauth2.client.registration.ClientRegistration reg) {
                if ("google".equals(reg.getRegistrationId())) {
                    result.add(java.util.Map.of("id", "google", "label", "เข้าสู่ระบบด้วย Google", "mark", "G", "url", "/oauth2/authorization/google"));
                }
            }
        }
    }
    // ThaiD เป็น mockup — แสดงปุ่มเสมอ ไม่ต้องรอ credential จริง
    result.add(java.util.Map.of("id", "thaid", "label", "เข้าสู่ระบบด้วย ThaiD", "mark", "ID", "url", "/thaid-mock/login"));
    return result;
}

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            Model model) {
        model.addAttribute("oauthProviders", availableProviders());
        model.addAttribute("oauthError", error != null);
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
            model.addAttribute("error", ErrorMessage.forUser(e));
            model.addAttribute("username", username);
            model.addAttribute("oauthProviders", availableProviders());
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
            model.addAttribute("error", ErrorMessage.forUser(e));
            model.addAttribute("username", username);
            model.addAttribute("gmail", gmail);
            return "register";
        }
    }

    // ---- ยืนยัน OTP ----
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
            model.addAttribute("error", ErrorMessage.forUser(e));
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
