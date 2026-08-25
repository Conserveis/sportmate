package com.sportmate.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sportmate.entity.User;
import com.sportmate.service.EventService;
import com.sportmate.service.PostService;
import com.sportmate.service.PublicProfileService;
import com.sportmate.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ProfileController {

    private final UserService userService;
    private final EventService eventService;
    private final PostService postService;
    private final PublicProfileService publicProfileService;

    public ProfileController(UserService userService, EventService eventService,
                             PostService postService, PublicProfileService publicProfileService) {
        this.userService = userService;
        this.eventService = eventService;
        this.postService = postService;
        this.publicProfileService = publicProfileService;
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
        model.addAttribute("hasPassword", userService.hasPassword(me.getId()));
        // แดชบอร์ดสรุปสถิติของตัวเอง (ใช้ service ตัวเดียวกับโปรไฟล์สาธารณะ)
        model.addAttribute("stats", publicProfileService.build(me));
        model.addAttribute("quotaLeft", postService.remainingWeeklyQuota(me));
        return "profile";
    }

    /**
     * โปรไฟล์สาธารณะของผู้ใช้คนอื่น
     *
     * ผู้เข้าร่วมเปิดดูผู้จัด  -> เห็นกิจกรรมที่เคยจัด, รีวิว, จำนวนครั้งการจัด, คะแนนเฉลี่ย
     * ผู้จัดเปิดดูผู้เข้าร่วม  -> เห็นจำนวนครั้งการเข้าร่วม, กีฬาที่เข้าร่วมมากที่สุด, กิจกรรมล่าสุด
     *
     * ถ้ากดดูโปรไฟล์ตัวเอง จะพาไปหน้า /profile ที่แก้ไขข้อมูลได้แทน
     */
    @GetMapping("/users/{id}")
    public String publicProfile(@PathVariable Integer id,
                                @RequestParam(defaultValue = "false") boolean preview,
                                HttpSession session, Model model, RedirectAttributes ra) {
        Integer myId = (Integer) session.getAttribute("uid");
        // ดูโปรไฟล์ตัวเอง -> ไปหน้า /profile ที่แก้ไขได้ ยกเว้นกด "ดูแบบที่คนอื่นเห็น"
        if (myId != null && myId.equals(id) && !preview) {
            return "redirect:/profile";
        }
        model.addAttribute("preview", myId != null && myId.equals(id));
        try {
            model.addAttribute("profile", publicProfileService.build(id));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/posts";
        }
        return "user-profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam(required = false) String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session, RedirectAttributes ra) {
        try {
            userService.setPassword((Integer) session.getAttribute("uid"),
                    currentPassword, newPassword, confirmPassword);
            ra.addFlashAttribute("msg", "ตั้งรหัสผ่านเรียบร้อยแล้ว");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
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

    // ---- ชำระเงินสมัครสมาชิก ----
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