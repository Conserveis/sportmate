package com.sportmate.controller;

import com.sportmate.entity.User;
import com.sportmate.service.EventService;
import com.sportmate.service.PostService;
import com.sportmate.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ProfileController {

    private final UserService userService;
    private final EventService eventService;
    private final PostService postService;

    public ProfileController(UserService userService, EventService eventService,
                             PostService postService) {
        this.userService = userService;
        this.eventService = eventService;
        this.postService = postService;
    }

    private User me(HttpSession session) {
        return userService.getById((Integer) session.getAttribute("uid"));
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User me = me(session);
        model.addAttribute("user", me);
        model.addAttribute("upcoming", eventService.upcomingByUser(me));     // กำลังจะมาถึง
        model.addAttribute("history", eventService.joinedByUser(me));        // ประวัติการเข้าร่วม
        model.addAttribute("organized", postService.ownedBy(me));           // ประวัติการจัด
        model.addAttribute("allSports", userService.allSports());
        return "profile";
    }

    @PostMapping("/profile/sports")
    public String updateSports(@RequestParam(required = false) List<Integer> sportIds,
                               HttpSession session, RedirectAttributes ra) {
        userService.updateInterestedSports((Integer) session.getAttribute("uid"), sportIds);
        ra.addFlashAttribute("msg", "บันทึกกีฬาที่สนใจแล้ว");
        return "redirect:/profile";
    }

    @PostMapping("/profile/edit")
    public String editProfile(@RequestParam(required = false) String phone,
                              HttpSession session, RedirectAttributes ra) {
        userService.updateProfile((Integer) session.getAttribute("uid"), phone);
        ra.addFlashAttribute("msg", "อัปเดตข้อมูลส่วนตัวแล้ว");
        return "redirect:/profile";
    }

    // ---- ชำระเงินสมัครสมาชิก (UC-5) ----
    @GetMapping("/subscribe")
    public String subscribePage(HttpSession session, Model model) {
        User me = me(session);
        if (me.isMember()) {
            return "redirect:/profile";
        }
        model.addAttribute("user", me);
        return "subscribe";
    }

    @PostMapping("/profile/subscribe")
    public String subscribe(@RequestParam String cardNumber,
                            @RequestParam String cardName,
                            @RequestParam String expiry,
                            @RequestParam String cvv,
                            HttpSession session, RedirectAttributes ra, Model model) {
        // ตรวจรูปแบบบัตรแบบจำลอง (ไม่ได้ตัดเงินจริง)
        String digits = cardNumber == null ? "" : cardNumber.replaceAll("\\s+", "");
        String err = null;
        if (!digits.matches("\\d{16}")) err = "หมายเลขบัตรต้องเป็นตัวเลข 16 หลัก";
        else if (cardName == null || cardName.isBlank()) err = "กรุณากรอกชื่อบนบัตร";
        else if (expiry == null || !expiry.matches("\\d{2}/\\d{2}")) err = "วันหมดอายุต้องอยู่ในรูปแบบ MM/YY";
        else if (cvv == null || !cvv.matches("\\d{3}")) err = "CVV ต้องเป็นตัวเลข 3 หลัก";

        if (err != null) {
            model.addAttribute("user", me(session));
            model.addAttribute("error", err);
            return "subscribe";
        }
        try {
            String last4 = digits.substring(12);
            userService.subscribeMonthly((Integer) session.getAttribute("uid"), last4);
            ra.addFlashAttribute("msg", "ชำระเงินสำเร็จ! อัปเกรดเป็นสมาชิกแล้ว — ปลดล็อกโพสต์ไม่จำกัด + สร้างทัวร์นาเมนต์");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }
}
