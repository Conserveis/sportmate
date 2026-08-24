package com.sportmate.controller;

import com.sportmate.entity.User;
import com.sportmate.service.NotificationService;
import com.sportmate.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserService userService;
    private final NotificationService notificationService;

    public GlobalControllerAdvice(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @ModelAttribute("currentUser")
    public User currentUser(HttpSession session) {
        Object uid = session.getAttribute("uid");
        if (uid == null) return null;
        try {
            return userService.getById((Integer) uid);
        } catch (Exception e) {
            return null;
        }
    }

    /** จำนวนการแจ้งเตือนที่ยังไม่อ่าน — ใช้แสดง badge บนกระดิ่งใน nav */
    @ModelAttribute("unreadCount")
    public long unreadCount(HttpSession session) {
        Object uid = session.getAttribute("uid");
        if (uid == null) return 0;
        try {
            return notificationService.unreadCount(userService.getById((Integer) uid));
        } catch (Exception e) {
            return 0;
        }
    }
}
