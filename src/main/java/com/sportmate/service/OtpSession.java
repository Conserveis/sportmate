package com.sportmate.service;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * สถานะ OTP ที่เก็บไว้ใน HttpSession (ไม่ลง DB)
 * ใช้ 2 กรณี:
 *   1) สมัครใหม่  -> ถือ userName/gmail/passwordHash ไว้ ยังไม่สร้าง row จนกว่า OTP จะผ่าน
 *   2) บัญชีเก่าที่ยังไม่ยืนยัน -> ถือแค่ userId
 */
public class OtpSession implements Serializable {

    /** กรอก OTP ผิดได้สูงสุดกี่ครั้งต่อ 1 รหัส */
    public static final int MAX_ATTEMPTS = 5;
    /** อายุของรหัส OTP (นาที) */
    public static final int TTL_MINUTES = 10;
    /** ต้องรอกี่วินาทีก่อนกดขอรหัสใหม่ได้อีก */
    public static final int RESEND_COOLDOWN_SECONDS = 60;
    /** ขอรหัสใหม่ได้สูงสุดกี่ครั้งต่อ 1 รอบการสมัคร */
    public static final int MAX_RESEND = 3;

    // --- กรณีสมัครใหม่ ---
    private String userName;
    private String gmail;
    private String passwordHash;
    // --- กรณีบัญชีเก่า ---
    private Integer userId;

    private String otpCode;
    private LocalDateTime otpExpireAt;
    private LocalDateTime lastSentAt;
    private int attempts;
    private int resendCount;

    /** สร้างรหัสใหม่ + รีเซ็ตตัวนับการกรอกผิด */
    public void issueNewCode() {
        this.otpCode = String.format("%06d", new java.util.Random().nextInt(1_000_000));
        this.otpExpireAt = LocalDateTime.now().plusMinutes(TTL_MINUTES);
        this.lastSentAt = LocalDateTime.now();
        this.attempts = 0;
        // จำลองการส่งอีเมล — ระบบจริงจะส่งผ่าน mail service
        System.out.println("[OTP] สำหรับ " + (gmail != null ? gmail : "userId=" + userId)
                + " = " + otpCode + " (หมดอายุใน " + TTL_MINUTES + " นาที)");
    }

    public boolean isExpired() {
        return otpExpireAt == null || otpExpireAt.isBefore(LocalDateTime.now());
    }

    public int attemptsLeft() {
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }

    public int resendLeft() {
        return Math.max(0, MAX_RESEND - resendCount);
    }

    /** เหลืออีกกี่วินาทีถึงจะกดขอรหัสใหม่ได้ (0 = กดได้เลย) */
    public long cooldownSecondsLeft() {
        if (lastSentAt == null) return 0;
        long passed = Duration.between(lastSentAt, LocalDateTime.now()).getSeconds();
        return Math.max(0, RESEND_COOLDOWN_SECONDS - passed);
    }

    public void countAttempt() { this.attempts++; }
    public void countResend() { this.resendCount++; }
    public boolean isNewAccount() { return userId == null; }

    // ---- getters / setters ----
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getGmail() { return gmail; }
    public void setGmail(String gmail) { this.gmail = gmail; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getOtpCode() { return otpCode; }
    public LocalDateTime getOtpExpireAt() { return otpExpireAt; }
    public int getAttempts() { return attempts; }
    public int getResendCount() { return resendCount; }
}