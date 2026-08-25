package com.sportmate.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class RememberMeService {

    private static final String COOKIE_NAME = "sportmate_remember_me";
    private static final long MAX_AGE_SECONDS = 30L * 24 * 60 * 60;

    private final byte[] secret;

    public RememberMeService(
            @Value("${app.remember-me-secret:${REMEMBER_ME_SECRET:change-this-sportmate-secret-in-production}}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public void remember(HttpServletRequest request, HttpServletResponse response, Integer userId) {
        long expiresAt = Instant.now().getEpochSecond() + MAX_AGE_SECONDS;
        String payload = userId + "." + expiresAt;
        String token = payload + "." + sign(payload);
        addCookie(response, request, token, MAX_AGE_SECONDS);
    }

    public boolean restoreSession(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        Cookie cookie = findCookie(request);
        if (cookie == null) return false;

        String[] parts = cookie.getValue().split("\\.", -1);
        if (parts.length != 3 || !isValidSignature(parts[0] + "." + parts[1], parts[2])) {
            clear(request, response);
            return false;
        }

        try {
            int userId = Integer.parseInt(parts[0]);
            long expiresAt = Long.parseLong(parts[1]);
            if (expiresAt <= Instant.now().getEpochSecond()) {
                clear(request, response);
                return false;
            }
            session.setAttribute("uid", userId);
            return true;
        } catch (NumberFormatException e) {
            clear(request, response);
            return false;
        }
    }

    public void clear(HttpServletRequest request, HttpServletResponse response) {
        addCookie(response, request, "", 0);
    }

    private Cookie findCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) return cookie;
        }
        return null;
    }

    private boolean isValidSignature(String payload, String signature) {
        return MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII));
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create remember-me token", e);
        }
    }

    private void addCookie(HttpServletResponse response, HttpServletRequest request,
                           String value, long maxAge) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(request.isSecure())
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}