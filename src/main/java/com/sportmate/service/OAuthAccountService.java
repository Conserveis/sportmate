package com.sportmate.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sportmate.entity.User;
import com.sportmate.entity.UserType;
import com.sportmate.repository.UserRepository;
import com.sportmate.repository.UserTypeRepository;

/**
 * แปลงข้อมูลผู้ใช้ที่ได้จาก Google / ThaiD ให้กลายเป็นบัญชี User ในระบบเรา
 *
 * หลักการจับคู่บัญชี (เรียงตามลำดับ):
 *   1. หาจาก provider + providerId ก่อน  -> เคยล็อกอินด้วยช่องทางนี้แล้ว
 *   2. ถ้าไม่เจอ ลองหาจากอีเมล          -> เคยสมัครด้วยอีเมลเดียวกันไว้ ให้ผูกบัญชีเข้าด้วยกัน
 *   3. ถ้ายังไม่เจอ                      -> สร้างบัญชีใหม่ให้อัตโนมัติ
 */
@Service
public class OAuthAccountService {

    private final UserRepository userRepo;
    private final UserTypeRepository userTypeRepo;

    public OAuthAccountService(UserRepository userRepo, UserTypeRepository userTypeRepo) {
        this.userRepo = userRepo;
        this.userTypeRepo = userTypeRepo;
    }

    @Transactional
    public User findOrCreate(String provider, OAuth2User oAuth2User) {
        Map<String, Object> attr = oAuth2User.getAttributes();

        String providerId = extractProviderId(provider, attr);
        String email = extractEmail(provider, attr, providerId);
        String displayName = extractDisplayName(provider, attr);

        // 1. เคยล็อกอินด้วยช่องทางนี้แล้ว
        var byProvider = userRepo.findByAuthProviderAndProviderId(provider, providerId);
        if (byProvider.isPresent()) {
            return byProvider.get();
        }

        // 2. เคยสมัครด้วยอีเมลเดียวกันไว้ -> ผูกบัญชีเดิมเข้ากับผู้ให้บริการนี้
        var byEmail = userRepo.findByGmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            existing.setAuthProvider(provider);
            existing.setProviderId(providerId);
            existing.setEmailVerified(true);   // ผู้ให้บริการยืนยันอีเมลให้แล้ว
            return userRepo.save(existing);
        }

        // 3. สร้างบัญชีใหม่
        UserType normal = userTypeRepo.findByName(UserType.NORMAL)
                .orElseThrow(() -> new IllegalStateException("ไม่พบ UserType 'Normal'"));

        User u = new User();
        u.setUserName(generateUniqueUserName(displayName, provider));
        u.setGmail(email);
        u.setPassword(null);              // บัญชีภายนอกไม่มีรหัสผ่านในระบบเรา
        u.setAuthProvider(provider);
        u.setProviderId(providerId);
        u.setUserType(normal);
        u.setEmailVerified(true);         // ข้ามขั้นตอน OTP เพราะผู้ให้บริการยืนยันให้แล้ว
        u.setAvgScore(BigDecimal.ZERO);
        u.setCreatedAt(LocalDateTime.now());
        return userRepo.save(u);
    }

    /**
    * สำหรับ ThaiD แบบ mock: ไม่มี OAuth2User จริง (ไม่ได้ต่อ DOPA จริง)
    * รับข้อมูลจากฟอร์มจำลองมาสร้าง/หาบัญชีตรงๆ ด้วย logic เดียวกับ findOrCreate
    */
    @Transactional
    public User findOrCreateFromThaiDMock(String pid, String fullName) {
        var byProvider = userRepo.findByAuthProviderAndProviderId("thaid", pid);
        if (byProvider.isPresent()) return byProvider.get();

        UserType normal = userTypeRepo.findByName(UserType.NORMAL)
                .orElseThrow(() -> new IllegalStateException("ไม่พบ UserType 'Normal'"));

        User u = new User();
        u.setUserName(generateUniqueUserName(fullName, "thaid"));
        u.setGmail("thaid_" + pid + "@no-email.sportmate.local");
        u.setPassword(null);
        u.setAuthProvider("thaid");
        u.setProviderId(pid);
        u.setUserType(normal);
        u.setEmailVerified(true);
        u.setAvgScore(BigDecimal.ZERO);
        u.setCreatedAt(LocalDateTime.now());
        return userRepo.save(u);
    }

    /** รหัสประจำตัวผู้ใช้ฝั่งผู้ให้บริการ — Google ใช้ "sub", ThaiD ใช้ "pid" */
    private String extractProviderId(String provider, Map<String, Object> attr) {
        Object id = "thaid".equals(provider) ? attr.getOrDefault("pid", attr.get("sub"))
                                             : attr.get("sub");
        if (id == null) {
            throw new IllegalStateException("ไม่พบรหัสผู้ใช้จากผู้ให้บริการ " + provider);
        }
        return String.valueOf(id);
    }

    /**
     * ThaiD ไม่ได้คืนอีเมลมาด้วย (คืนเลขบัตรประชาชนกับชื่อ) และคอลัมน์ Gmail
     * ในฐานข้อมูลเป็น NOT NULL UNIQUE จึงต้องสร้างอีเมลแทนไว้ก่อน
     * ผู้ใช้สามารถไปแก้อีเมลจริงได้ภายหลังในหน้าโปรไฟล์
     */
    private String extractEmail(String provider, Map<String, Object> attr, String providerId) {
        Object email = attr.get("email");
        if (email != null && !String.valueOf(email).isBlank()) {
            return String.valueOf(email);
        }
        return provider + "_" + providerId + "@no-email.sportmate.local";
    }

    private String extractDisplayName(String provider, Map<String, Object> attr) {
        for (String key : new String[]{"name", "given_name", "email"}) {
            Object v = attr.get(key);
            if (v != null && !String.valueOf(v).isBlank()) {
                return String.valueOf(v);
            }
        }
        return provider + "user";
    }

    /**
     * UserName ในระบบเป็น UNIQUE จึงต้องกันชนกัน
     * เช่น "สมชาย ใจดี" -> "somchai" ถ้าซ้ำก็ต่อท้ายเป็น "somchai2", "somchai3" ...
     */
    private String generateUniqueUserName(String displayName, String provider) {
        String base = displayName.split("@")[0]
                .replaceAll("[^a-zA-Z0-9ก-๙_]", "")
                .toLowerCase();
        if (base.isBlank()) base = provider + "user";
        if (base.length() > 40) base = base.substring(0, 40);

        String candidate = base;
        int suffix = 1;
        while (userRepo.existsByUserName(candidate)) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
    }
}
