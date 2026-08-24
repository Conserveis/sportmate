package com.sportmate.config.config;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sportmate.entity.User;
import com.sportmate.service.OAuthAccountService;

import jakarta.servlet.http.HttpSession;


//จำลองหน้ายืนยันตัวตนของ ThaiD 
@Controller
public class ThaiDMockController {

    private final OAuthAccountService oAuthAccountService;

    public ThaiDMockController(OAuthAccountService oAuthAccountService) {
        this.oAuthAccountService = oAuthAccountService;
    }

    @GetMapping("/thaid-mock/login")
    public String mockLoginPage() {
        return "thaid-mock";
    }

    @PostMapping("/thaid-mock/login")
    public String mockLoginSubmit(@RequestParam String pid,
                                  @RequestParam String fullName,
                                  HttpSession session, Model model) {
        if (pid == null || !pid.matches("\\d{13}")) {
            model.addAttribute("error", "เลขบัตรประชาชนต้องเป็นตัวเลข 13 หลัก (ข้อมูลจำลอง ใส่เลขอะไรก็ได้ 13 หลัก)");
            return "thaid-mock";
        }
        User user = oAuthAccountService.findOrCreateFromThaiDMock(pid, fullName);
        session.setAttribute("uid", user.getId());
        return "redirect:/posts";
    }
}
