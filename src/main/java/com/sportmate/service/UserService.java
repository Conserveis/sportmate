package com.sportmate.service;

import java.math.BigDecimal;
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
     * สมัครสมาชิก: สร้างบัญชีแบบยังไม่ยืนยัน แล้วออก OTP ให้ยืนยัน
     * หมายเหตุ: ไม่มี mail server จริง จึงไม่ได้ "ส่ง" OTP ทางอีเมล แต่จะแสดง OTP
     * บนหน้าจอยืนยัน (โหมดจำลอง) และพิมพ์ลง log ให้กรอกต่อได้
     */
    @Transactional
    public User register(String userName, String gmail, String rawPassword) {
        if (userRepo.existsByUserName(userName))
            throw new IllegalArgumentException("ชื่อผู้ใช้งานนี้มีผู้ใช้แล้ว");
        if (userRepo.existsByGmail(gmail))
            throw new IllegalArgumentException("อีเมลนี้มีผู้ใช้แล้ว");
        if (rawPassword == null || rawPassword.length() < 8)
            throw new IllegalArgumentException("รหัสผ่านต้องยาวอย่างน้อย 8 ตัวอักษร");

        UserType normal = userTypeRepo.findByName(UserType.NORMAL)
                .orElseThrow(() -> new IllegalStateException("ไม่พบ UserType 'Normal'"));

        User u = new User();
        u.setUserName(userName);
        u.setGmail(gmail);
        u.setPassword(encoder.encode(rawPassword));
        u.setUserType(normal);
        u.setEmailVerified(false);   // ต้องยืนยัน OTP ก่อน
        u.setAvgScore(BigDecimal.ZERO);
        u.setCreatedAt(LocalDateTime.now());
        userRepo.save(u);
        generateOtp(u);
        return u;
    }

    /** สร้าง OTP 6 หลัก ตั้งอายุ 10 นาที และคืนค่า OTP */
    @Transactional
    public String generateOtp(User u) {
        String code = String.format("%06d", new java.util.Random().nextInt(1_000_000));
        u.setOtpCode(code);
        u.setOtpExpireAt(LocalDateTime.now().plusMinutes(10));
        userRepo.save(u);
        // จำลองการส่งอีเมล — ในระบบจริงจะส่งผ่าน mail service
        System.out.println("[OTP] สำหรับ " + u.getGmail() + " = " + code + " (หมดอายุใน 10 นาที)");
        return code;
    }

    /** ขอ OTP ใหม่ */
    @Transactional
    public String resendOtp(Integer userId) {
        return generateOtp(getById(userId));
    }

    /** ตรวจสอบ OTP : ถูกต้อง + ยังไม่หมดอายุ -> เปิดใช้งานบัญชี */
    @Transactional
    public void verifyOtp(Integer userId, String code) {
        User u = getById(userId);
        if (u.isEmailVerified()) return;
        if (u.getOtpCode() == null || u.getOtpExpireAt() == null)
            throw new IllegalStateException("ไม่พบรหัส OTP กรุณากดขอรหัสใหม่");
        if (u.getOtpExpireAt().isBefore(LocalDateTime.now()))
            throw new IllegalStateException("รหัส OTP หมดอายุแล้ว กรุณากดขอรหัสใหม่");
        if (!u.getOtpCode().equals(code == null ? null : code.trim()))
            throw new IllegalArgumentException("รหัส OTP ไม่ถูกต้อง");
        u.setEmailVerified(true);
        u.setOtpCode(null);
        u.setOtpExpireAt(null);
        userRepo.save(u);
    }

    /** ล็อกอินAuthen */
    public User login(String userNameOrEmail, String rawPassword) {
    User u = userRepo.findByUserName(userNameOrEmail)
            .or(() -> userRepo.findByGmail(userNameOrEmail))
            .orElseThrow(() -> new IllegalArgumentException("ชื่อผู้ใช้/อีเมล หรือรหัสผ่านไม่ถูกต้อง"));
    // บล็อกเฉพาะบัญชีที่ "ยังไม่มีรหัสผ่าน" เท่านั้น
    // บัญชีที่เคยตั้งรหัสผ่านไว้แล้วผูกกับ Google/ThaiD ทีหลัง ยังเข้าด้วยรหัสผ่านได้ตามปกติ
    if (u.getPassword() == null || u.getPassword().isBlank())
        throw new IllegalArgumentException(
                "บัญชีนี้สมัครผ่าน " + providerLabel(u.getAuthProvider())
                + " จึงยังไม่มีรหัสผ่าน กรุณากดปุ่มด้านล่างเพื่อเข้าสู่ระบบ "
                + "แล้วไปตั้งรหัสผ่านได้ที่หน้าโปรไฟล์");
    if (!encoder.matches(rawPassword, u.getPassword()))
        throw new IllegalArgumentException("ชื่อผู้ใช้/อีเมล หรือรหัสผ่านไม่ถูกต้อง");
    if (!u.isEmailVerified())
        throw new UnverifiedUserException(u.getId());
    return u;
}

/** ตั้ง/เปลี่ยนรหัสผ่าน — ใช้ได้ทั้งบัญชี local และบัญชีที่มาจาก Google/ThaiD */
@Transactional
public void setPassword(Integer userId, String currentPassword, String newPassword, String confirmPassword) {
    User u = getById(userId);
    boolean hasPassword = u.getPassword() != null && !u.getPassword().isBlank();

    // บัญชีที่มีรหัสผ่านอยู่แล้ว ต้องยืนยันรหัสเดิมก่อน
    if (hasPassword && !encoder.matches(currentPassword, u.getPassword()))
        throw new IllegalArgumentException("รหัสผ่านเดิมไม่ถูกต้อง");
    if (newPassword == null || newPassword.length() < 8)
        throw new IllegalArgumentException("รหัสผ่านต้องยาวอย่างน้อย 8 ตัวอักษร");
    if (!newPassword.equals(confirmPassword))
        throw new IllegalArgumentException("รหัสผ่านและการยืนยันไม่ตรงกัน");

    u.setPassword(encoder.encode(newPassword));
    userRepo.save(u);
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

    /** ใช้บอก controller ว่าบัญชีนี้ยังไม่ยืนยัน OTP เพื่อพาไปหน้ายืนยัน */
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
    @Transactional
    public void updateProfile(Integer userId, String phone) {
        User u = getById(userId);
        u.setPhoneNumber(phone);
        userRepo.save(u);
    }

    /**
     * สมัคร Paid Member รายเดือน
     * หมายเหตุ: ไม่ได้ต่อ Payment Gateway จริง — ตรวจรูปแบบบัตรแบบจำลองแล้วถือว่าชำระสำเร็จ
     * -> เปลี่ยน UserType เป็น 'Member', ตั้งวันหมดอายุ +1 เดือน, ออกใบเสร็จ (เก็บเลขบัตร 4 ตัวท้าย)
     */
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