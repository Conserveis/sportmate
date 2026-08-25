package com.sportmate.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final RememberMeService rememberMeService;

    public AuthInterceptor(RememberMeService rememberMeService) {
        this.rememberMeService = rememberMeService;
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
        return true;
    }
}
