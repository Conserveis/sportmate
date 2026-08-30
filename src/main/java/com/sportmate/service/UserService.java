package com.sportmate.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sportmate.entity.Receipt;
import com.sportmate.entity.Sport;
import com.sportmate.entity.User;
import com.sportmate.entity.UserType;
import com.sportmate.repository.ReceiptRepository;
import com.sportmate.repository.SportRepository;
import com.sportmate.repository.UserRepository;
import com.sportmate.repository.UserTypeRepository;

@Service
public class UserService {

    /** กรอกรหัสผิดเกินจำนวนนี้ -> ล็อกบัญชีชั่วคราว */
    public static final int MAX_FAILED_LOGIN = 5;
    /** ระยะเวลาล็อกบัญชี (นาที) */
    public static final int LOCK_MINUTES = 30;

    private final UserRepository userRepo;
    private final UserTypeRepository userTypeRepo;
    private final SportRepository sportRepo;
    private final ReceiptRepository receiptRepo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository userRepo, UserTypeRepository userTypeRepo,
                       SportRepository sportRepo, ReceiptRepository receiptRepo,
                       PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.userTypeRepo = userTypeRepo;
        this.sportRepo = sportRepo;
        this.receiptRepo = receiptRepo;
        this.encoder = encoder;
    }

        /**
     * ขั้นที่ 1 ของการสมัคร — ตรวจข้อมูล + ออก OTP
     * ยังไม่เขียนอะไรลง DB ทั้งสิ้น บัญชีจะถูกสร้างก็ต่อเมื่อ OTP ผ่านเท่านั้น
     */
    public OtpSession startRegistration(String userName, String gmail, String rawPassword) {
        if (userRepo.existsByUserName(userName))
            throw new IllegalArgumentException("ชื่อผู้ใช้งานนี้มีผู้ใช้แล้ว");
        if (userRepo.existsByGmail(gmail))
            throw new IllegalArgumentException("อีเมลนี้มีผู้ใช้แล้ว");
        validatePassword(rawPassword);

        OtpSession s = new OtpSession();
        s.setUserName(userName);
        s.setGmail(gmail);
        s.setPasswordHash(encoder.encode(rawPassword));
        s.issueNewCode();
        return s;
    }

    /** สร้าง OtpSession สำหรับบัญชีเก่าที่ยังไม่ยืนยันอีเมล */
    public OtpSession startVerifyExisting(Integer userId) {
        User u = getById(userId);
        OtpSession s = new OtpSession();
        s.setUserId(u.getId());
        s.setGmail(u.getGmail());
        s.issueNewCode();
        return s;
    }

    /**
     * ตรวจ OTP — หมดอายุ / กรอกผิดเกินกำหนด จะโยน exception
     * เรียกใช้ก่อน completeRegistration() หรือ markVerified() เสมอ
     */
    public void checkOtp(OtpSession s, String code) {
        if (s.getOtpCode() == null)
            throw new IllegalStateException("ไม่พบรหัส OTP กรุณากดขอรหัสใหม่");
        if (s.isExpired())
            throw new IllegalStateException("รหัส OTP หมดอายุแล้ว กรุณากดขอรหัสใหม่");
        if (s.attemptsLeft() <= 0)
            throw new IllegalStateException(
                    "กรอกรหัส OTP ผิดครบ " + OtpSession.MAX_ATTEMPTS + " ครั้ง "
                    + "รหัสนี้ใช้ไม่ได้แล้ว กรุณากดขอรหัสใหม่");

        if (!s.getOtpCode().equals(code == null ? null : code.trim())) {
            s.countAttempt();
            if (s.attemptsLeft() <= 0)
                throw new IllegalArgumentException(
                        "รหัส OTP ไม่ถูกต้อง และกรอกผิดครบ " + OtpSession.MAX_ATTEMPTS
                        + " ครั้งแล้ว กรุณากดขอรหัสใหม่");
            throw new IllegalArgumentException(
                    "รหัส OTP ไม่ถูกต้อง (เหลืออีก " + s.attemptsLeft() + " ครั้ง)");
        }
    }

    /** ขั้นที่ 2 — OTP ผ่านแล้ว ค่อยสร้างบัญชีจริงลง DB */
    @Transactional
    public User completeRegistration(OtpSession s) {
        // เช็คซ้ำอีกรอบ เผื่อมีคนสมัครชื่อ/อีเมลเดียวกันตัดหน้าระหว่างรอกรอก OTP
        if (userRepo.existsByUserName(s.getUserName()))
            throw new IllegalArgumentException("ชื่อผู้ใช้งานนี้มีผู้ใช้แล้ว กรุณาสมัครใหม่");
        if (userRepo.existsByGmail(s.getGmail()))
            throw new IllegalArgumentException("อีเมลนี้มีผู้ใช้แล้ว กรุณาสมัครใหม่");

        UserType normal = userTypeRepo.findByName(UserType.NORMAL)
                .orElseThrow(() -> new IllegalStateException("ไม่พบ UserType 'Normal'"));

        User u = new User();
        u.setUserName(s.getUserName());
        u.setGmail(s.getGmail());
        u.setPassword(s.getPasswordHash());
        u.setUserType(normal);
        u.setEmailVerified(true);   // ผ่าน OTP แล้วเท่านั้นถึงมาถึงบรรทัดนี้
        u.setAvgScore(BigDecimal.ZERO);
        u.setCreatedAt(LocalDateTime.now());
        return userRepo.save(u);
    }

    /** ยืนยันอีเมลให้บัญชีเก่าที่มี row อยู่แล้ว */
    @Transactional
    public void markVerified(Integer userId) {
        User u = getById(userId);
        u.setEmailVerified(true);
        u.setOtpCode(null);
        u.setOtpExpireAt(null);
        userRepo.save(u);
    }

    /** ขอรหัสใหม่ — จำกัดจำนวนครั้ง + คูลดาวน์ */
    public void resend(OtpSession s) {
        long wait = s.cooldownSecondsLeft();
        if (wait > 0)
            throw new IllegalStateException("กรุณารออีก " + wait + " วินาที ก่อนขอรหัสใหม่");
        if (s.resendLeft() <= 0)
            throw new IllegalStateException(
                    "ขอรหัสใหม่ได้สูงสุด " + OtpSession.MAX_RESEND
                    + " ครั้ง กรุณาเริ่มสมัครใหม่อีกครั้ง");
        s.countResend();
        s.issueNewCode();
    }

        /**
     * ล็อกอินAuthen
     * หมายเหตุ: เมธอดนี้ "ห้าม" ใส่ @Transactional เพราะเราต้อง save จำนวนครั้งที่ผิด
     * ก่อนจะ throw exception — ถ้าอยู่ใน transaction เดียวกัน exception จะ rollback การนับทิ้ง
     */
    public User login(String userNameOrEmail, String rawPassword) {
    User u = userRepo.findByUserName(userNameOrEmail)
            .or(() -> userRepo.findByGmail(userNameOrEmail))
            .orElseThrow(() -> new IllegalArgumentException("ชื่อผู้ใช้/อีเมล หรือรหัสผ่านไม่ถูกต้อง"));

    // 1) ยังอยู่ในช่วงถูกล็อกอยู่หรือไม่
    LocalDateTime now = LocalDateTime.now();
    if (u.getLockUntil() != null && u.getLockUntil().isAfter(now)) {
        long left = Duration.between(now, u.getLockUntil()).toMinutes() + 1;
        throw new IllegalArgumentException(
                "บัญชีนี้ถูกล็อกชั่วคราวเนื่องจากกรอกรหัสผ่านผิดเกิน " + MAX_FAILED_LOGIN
                + " ครั้ง กรุณาลองใหม่อีกครั้งในอีก " + left + " นาที");
    }
    // เลยเวลาล็อกแล้ว -> ล้างสถานะ เริ่มนับใหม่
    if (u.getLockUntil() != null) {
        u.setLockUntil(null);
        u.setFailedLoginCount(0);
        userRepo.save(u);
    }

    // บล็อกเฉพาะบัญชีที่ "ยังไม่มีรหัสผ่าน" เท่านั้น
    // บัญชีที่เคยตั้งรหัสผ่านไว้แล้วผูกกับ Google/ThaiD ทีหลัง ยังเข้าด้วยรหัสผ่านได้ตามปกติ
    if (u.getPassword() == null || u.getPassword().isBlank())
        throw new IllegalArgumentException(
                "บัญชีนี้สมัครผ่าน " + providerLabel(u.getAuthProvider())
                + " จึงยังไม่มีรหัสผ่าน กรุณากดปุ่มด้านล่างเพื่อเข้าสู่ระบบ "
                + "แล้วไปตั้งรหัสผ่านได้ที่หน้าโปรไฟล์");

    // 2) รหัสผ่านผิด -> นับเพิ่ม และล็อกเมื่อครบ
    if (!encoder.matches(rawPassword, u.getPassword())) {
        int failed = u.getFailedLoginCount() + 1;
        u.setFailedLoginCount(failed);

        if (failed >= MAX_FAILED_LOGIN) {
            u.setLockUntil(now.plusMinutes(LOCK_MINUTES));
            u.setFailedLoginCount(0);   // เริ่มนับใหม่หลังปลดล็อก
            userRepo.save(u);
            throw new IllegalArgumentException(
                    "กรอกรหัสผ่านผิดครบ " + MAX_FAILED_LOGIN + " ครั้ง "
                    + "บัญชีถูกล็อกชั่วคราว " + LOCK_MINUTES + " นาที");
        }

        userRepo.save(u);
        int left = MAX_FAILED_LOGIN - failed;
        throw new IllegalArgumentException(
                "ชื่อผู้ใช้/อีเมล หรือรหัสผ่านไม่ถูกต้อง (เหลืออีก " + left + " ครั้งก่อนบัญชีถูกล็อก)");
    }

    // 3) รหัสผ่านถูก -> ล้างตัวนับ
    if (u.getFailedLoginCount() != 0 || u.getLockUntil() != null) {
        u.setFailedLoginCount(0);
        u.setLockUntil(null);
        userRepo.save(u);
    }

    if (!u.isEmailVerified())
        throw new UnverifiedUserException(u.getId());
    return u;
}

/** ปลดล็อกบัญชีด้วยมือ (เผื่อใช้ตอนเดโม / ทดสอบ) */
@Transactional
public void unlock(Integer userId) {
    User u = getById(userId);
    u.setFailedLoginCount(0);
    u.setLockUntil(null);
    userRepo.save(u);
}

/** ตั้ง/เปลี่ยนรหัสผ่าน — ใช้ได้ทั้งบัญชี local และบัญชีที่มาจาก Google/ThaiD */
@Transactional
public void setPassword(Integer userId, String currentPassword, String newPassword, String confirmPassword) {
    User u = getById(userId);
    boolean hasPassword = u.getPassword() != null && !u.getPassword().isBlank();

    // บัญชีที่มีรหัสผ่านอยู่แล้ว ต้องยืนยันรหัสเดิมก่อน
    if (hasPassword && !encoder.matches(currentPassword, u.getPassword()))
        throw new IllegalArgumentException("รหัสผ่านเดิมไม่ถูกต้อง");
    validatePassword(newPassword);
    if (!newPassword.equals(confirmPassword))
        throw new IllegalArgumentException("รหัสผ่านและการยืนยันไม่ตรงกัน");

    u.setPassword(encoder.encode(newPassword));
    userRepo.save(u);
}

public static void validatePassword(String password) {
    if (password == null || password.length() < 8) {
        throw new IllegalArgumentException("รหัสผ่านต้องยาวอย่างน้อย 8 ตัวอักษร");
    }
    boolean hasUpper = password.chars().anyMatch(ch -> ch >= 'A' && ch <= 'Z');
    boolean hasLower = password.chars().anyMatch(ch -> ch >= 'a' && ch <= 'z');
    if (!hasUpper || !hasLower) {
        throw new IllegalArgumentException("รหัสผ่านต้องมีทั้งตัวอักษรพิมพ์ใหญ่ (A-Z) และพิมพ์เล็ก (a-z)");
    }
}

/** บัญชีนี้ตั้งรหัสผ่านไว้หรือยัง (ให้หน้าโปรไฟล์เลือกแสดงข้อความ) */
public boolean hasPassword(Integer userId) {
    String p = getById(userId).getPassword();
    return p != null && !p.isBlank();
}

private String providerLabel(String provider) {
    return switch (provider == null ? "" : provider) {
        case "google" -> "Google";
        case "thaid"  -> "ThaiD";
        default        -> "ผู้ให้บริการภายนอก";
    };
} //Authen

    public static class UnverifiedUserException extends RuntimeException {
        private final Integer userId;
        public UnverifiedUserException(Integer userId) { this.userId = userId; }
        public Integer getUserId() { return userId; }
    }

    public User getById(Integer id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบผู้ใช้"));
    }

    /** อัปเดตกีฬาที่สนใจ (UserSport) */
    @Transactional
    public void updateInterestedSports(Integer userId, List<Integer> sportIds) {
        User u = getById(userId);
        Set<Sport> sports = new HashSet<>();
        if (sportIds != null) {
            sports = sportIds.stream().map(id -> sportRepo.findById(id).orElse(null))
                    .filter(s -> s != null).collect(Collectors.toSet());
        }
        u.setInterestedSports(sports);
        userRepo.save(u);
    }

    /** แก้ไขข้อมูลส่วนตัว */
    /** แก้ไขข้อมูลส่วนตัว: ชื่อผู้ใช้ / อีเมล / เบอร์โทร */
    @Transactional
    public void updateProfile(Integer userId, String userName, String gmail, String phone) {
        User u = getById(userId);

        String newName  = userName == null ? "" : userName.trim();
        String newMail  = gmail == null ? "" : gmail.trim().toLowerCase();
        String newPhone = phone == null ? "" : phone.trim();

        // --- ชื่อผู้ใช้งาน ---
        if (newName.isBlank())
            throw new IllegalArgumentException("กรุณากรอกชื่อผู้ใช้งาน");
        if (newName.length() < 3 || newName.length() > 50)
            throw new IllegalArgumentException("ชื่อผู้ใช้งานต้องยาว 3–50 ตัวอักษร");
        if (!newName.equals(u.getUserName()) && userRepo.existsByUserName(newName))
            throw new IllegalArgumentException("ชื่อผู้ใช้งานนี้มีผู้ใช้แล้ว");

        // --- อีเมล ---
        if (newMail.isBlank())
            throw new IllegalArgumentException("กรุณากรอกอีเมล");
        if (!newMail.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$"))
            throw new IllegalArgumentException("รูปแบบอีเมลไม่ถูกต้อง");
        if (!newMail.equalsIgnoreCase(u.getGmail()) && userRepo.existsByGmail(newMail))
            throw new IllegalArgumentException("อีเมลนี้มีผู้ใช้แล้ว");

        // --- เบอร์โทร (ไม่บังคับกรอก) ---
        if (!newPhone.isBlank()) {
            String digits = newPhone.replaceAll("[\\s-]", "");
            if (!digits.matches("0\\d{8,9}"))
                throw new IllegalArgumentException("เบอร์โทรศัพท์ต้องเป็นตัวเลข 9–10 หลัก และขึ้นต้นด้วย 0");
            newPhone = digits;
        }

        u.setUserName(newName);
        u.setGmail(newMail);
        u.setPhoneNumber(newPhone.isBlank() ? null : newPhone);
        userRepo.save(u);
    }

    @Transactional
    public void subscribeMonthly(Integer userId, String cardLast4) {
        User u = getById(userId);
        UserType member = userTypeRepo.findByName(UserType.MEMBER)
                .orElseThrow(() -> new IllegalStateException("ไม่พบ UserType 'Member'"));
        u.setUserType(member);
        LocalDateTime base = (u.getMembershipExpireAt() != null && u.getMembershipExpireAt().isAfter(LocalDateTime.now()))
                ? u.getMembershipExpireAt() : LocalDateTime.now();
        u.setMembershipExpireAt(base.plusMonths(1));
        userRepo.save(u);

        Receipt r = new Receipt();
        r.setUser(u);
        r.setAmount(new BigDecimal("99.00"));
        r.setDatePlayment(LocalDateTime.now());
        r.setQr(cardLast4 == null ? null : "**** **** **** " + cardLast4);
        receiptRepo.save(r);
    }

    public List<Sport> allSports() { return sportRepo.findAll(); }
}