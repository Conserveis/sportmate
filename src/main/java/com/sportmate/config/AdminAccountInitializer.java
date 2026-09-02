package com.sportmate.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.sportmate.entity.User;
import com.sportmate.entity.UserType;
import com.sportmate.repository.UserRepository;
import com.sportmate.repository.UserTypeRepository;

/**
 * สร้าง/อัปเดตบัญชีผู้ดูแลระบบจากค่าใน .env ตอนแอปสตาร์ท
 *   ADMIN_USERNAME / ADMIN_EMAIL / ADMIN_PASSWORD
 * ถ้าไม่ตั้ง ADMIN_PASSWORD ไว้ จะข้ามไปเฉย ๆ (ไม่สร้างบัญชี)
 */
@Component
@Order(2)   // ให้รันหลัง DataInitializer
public class AdminAccountInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserRepository userRepo;
    private final UserTypeRepository userTypeRepo;
    private final PasswordEncoder encoder;

    @Value("${sportmate.admin.username:admin}")
    private String adminUsername;

    @Value("${sportmate.admin.email:admin@sportmate.local}")
    private String adminEmail;

    @Value("${sportmate.admin.password:}")
    private String adminPassword;

    public AdminAccountInitializer(UserRepository userRepo, UserTypeRepository userTypeRepo,
                                   PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.userTypeRepo = userTypeRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.info("ไม่ได้ตั้ง ADMIN_PASSWORD — ข้ามการสร้างบัญชีผู้ดูแลระบบ");
            return;
        }

        UserType adminType = userTypeRepo.findByName(UserType.ADMIN).orElse(null);
        if (adminType == null) {
            log.warn("ไม่พบ UserType 'Admin' — ยังไม่ได้รัน db/init/06_admin.sql ใช่ไหม?");
            return;
        }

        User admin = userRepo.findByUserName(adminUsername).orElse(null);
        boolean isNew = (admin == null);

        if (isNew) {
            admin = new User();
            admin.setUserName(adminUsername);
            admin.setGmail(adminEmail);
            admin.setAvgScore(BigDecimal.ZERO);
            admin.setCreatedAt(LocalDateTime.now());
        }

        admin.setPassword(encoder.encode(adminPassword));   // แฮชใหม่ทุกครั้งที่สตาร์ท
        admin.setAuthProvider("local");
        admin.setUserType(adminType);
        admin.setEmailVerified(true);
        admin.setFailedLoginCount(0);
        admin.setLockUntil(null);
        userRepo.save(admin);

        log.info("{}บัญชีผู้ดูแลระบบ '{}' เรียบร้อย", isNew ? "สร้าง" : "อัปเดต", adminUsername);
    }
}