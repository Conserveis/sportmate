package com.sportmate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sportmate.entity.User;
import com.sportmate.service.OtpSession;
import com.sportmate.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sportmate.util.ErrorMessage;
import com.sportmate.config.RememberMeService;

@Controller
public class AuthController {

    private final UserService userService;
    private final RememberMeService rememberMeService;
    private final org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrations;

    public AuthController(UserService userService,
                          org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrations,
                          RememberMeService rememberMeService) {
        this.userService = userService;
        this.rememberMeService = rememberMeService;
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
                        @RequestParam(defaultValue = "false") boolean rememberMe,
                        HttpSession session, HttpServletRequest request,
                        HttpServletResponse response, Model model) {
        try {
            User u = userService.login(username, password);
            session.setAttribute("uid", u.getId());
            if (rememberMe) {
                rememberMeService.remember(request, response, u.getId());
            } else {
                rememberMeService.clear(request, response);
            }
            return "redirect:/home";
        } catch (UserService.UnverifiedUserException ue) {
            // บัญชียังไม่ยืนยัน OTP -> พาไปหน้ายืนยัน
            session.setAttribute("otpSession", userService.startVerifyExisting(ue.getUserId()));
            session.setAttribute("pendingRememberMe", rememberMe);
            return "redirect:/verify";
        } catch (Exception e) {
            model.addAttribute("error", ErrorMessage.forUser(e));
            model.addAttribute("username", username);
            model.addAttribute("rememberMe", rememberMe);
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
            // ยังไม่สร้างบัญชี — เก็บไว้ใน session รอ OTP ผ่านก่อน
            session.setAttribute("otpSession",
                    userService.startRegistration(username, gmail, password));
            return "redirect:/verify";
        } catch (Exception e) {
            model.addAttribute("error", ErrorMessage.forUser(e));
            model.addAttribute("username", username);
            model.addAttribute("gmail", gmail);
            return "register";
        }
    }

    // ---- ยืนยัน OTP ----
    private void fillOtpModel(Model model, OtpSession s) {
        model.addAttribute("gmail", s.getGmail());
        model.addAttribute("devOtp", s.getOtpCode());          // โหมดจำลองเท่านั้น
        model.addAttribute("attemptsLeft", s.attemptsLeft());
        model.addAttribute("resendLeft", s.resendLeft());
        model.addAttribute("cooldown", s.cooldownSecondsLeft());
    }

    @GetMapping("/verify")
    public String verifyPage(HttpSession session, Model model) {
        OtpSession s = (OtpSession) session.getAttribute("otpSession");
        if (s == null) return "redirect:/register";
        fillOtpModel(model, s);
        return "verify";
    }

    @PostMapping("/verify")
    public String verify(@RequestParam String otp, HttpSession session,
                         HttpServletRequest request, HttpServletResponse response, Model model) {
        OtpSession s = (OtpSession) session.getAttribute("otpSession");
        if (s == null) return "redirect:/register";
        try {
            userService.checkOtp(s, otp);   // ผิด/หมดอายุ/เกินจำนวนครั้ง -> throw

            Integer uid = s.isNewAccount()
                    ? userService.completeRegistration(s).getId()   // สร้างบัญชีตรงนี้เท่านั้น
                    : s.getUserId();
            if (!s.isNewAccount()) userService.markVerified(uid);

            session.removeAttribute("otpSession");
            boolean rememberMe = Boolean.TRUE.equals(session.getAttribute("pendingRememberMe"));
            session.removeAttribute("pendingRememberMe");
            session.setAttribute("uid", uid);   // ยืนยันสำเร็จ -> ล็อกอินเลย
            if (rememberMe) {
                rememberMeService.remember(request, response, uid);
            } else {
                rememberMeService.clear(request, response);
            }
            return "redirect:/home";
        } catch (Exception e) {
            model.addAttribute("error", ErrorMessage.forUser(e));
            fillOtpModel(model, s);
            return "verify";
        }
    }

    @PostMapping("/verify/resend")
    public String resend(HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        OtpSession s = (OtpSession) session.getAttribute("otpSession");
        if (s == null) return "redirect:/register";
        try {
            userService.resend(s);
            ra.addFlashAttribute("msg", "ส่งรหัส OTP ใหม่แล้ว");
        } catch (Exception e) {
            ra.addFlashAttribute("error", ErrorMessage.forUser(e));
        }
        return "redirect:/verify";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        session.invalidate();
        rememberMeService.clear(request, response);
        return "redirect:/login";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/home";
    }
}
