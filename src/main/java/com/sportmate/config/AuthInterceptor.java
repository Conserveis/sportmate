package com.sportmate.config;

import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.sportmate.repository.UserRepository;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    /** เขียน LastActivityAt ลง DB ไม่ถี่กว่านี้ต่อ session */
    private static final long TOUCH_INTERVAL_MS = 5 * 60 * 1000;
    private static final String LAST_TOUCH = "lastActivityTouch";

    private final RememberMeService rememberMeService;
    private final UserRepository userRepo;

    public AuthInterceptor(RememberMeService rememberMeService, UserRepository userRepo) {
        this.rememberMeService = rememberMeService;
        this.userRepo = userRepo;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        boolean loggedIn = session != null && session.getAttribute("uid") != null;
        if (!loggedIn && request.getCookies() != null) {
            session = request.getSession(true);
            loggedIn = rememberMeService.restoreSession(request, response, session);
        }
        if (!loggedIn) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        touchActivity(session);
        return true;
    }

    /** บันทึกเวลาใช้งานล่าสุด (สำหรับสถิติ Active 7/30 วันในหน้าแอดมิน) — พลาดก็ไม่ขวางการใช้งาน */
    private void touchActivity(HttpSession session) {
        long now = System.currentTimeMillis();
        Object last = session.getAttribute(LAST_TOUCH);
        if (last instanceof Long t && now - t < TOUCH_INTERVAL_MS) return;
        try {
            userRepo.touchActivity((Integer) session.getAttribute("uid"), LocalDateTime.now());
            session.setAttribute(LAST_TOUCH, now);
        } catch (Exception e) {
            log.warn("Could not update LastActivityAt", e);
        }
    }
}
