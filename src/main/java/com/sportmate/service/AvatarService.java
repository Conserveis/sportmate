package com.sportmate.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sportmate.entity.User;
import com.sportmate.repository.UserRepository;

@Service
public class AvatarService {

    private static final Logger log = LoggerFactory.getLogger(AvatarService.class);
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES = 2L * 1024 * 1024;   // 2 MB

    private final Path baseDir;
    private final UserRepository userRepo;

    public AvatarService(@Value("${app.upload.dir}") String uploadDir, UserRepository userRepo) {
        this.baseDir = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
        this.userRepo = userRepo;
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("สร้างโฟลเดอร์เก็บรูปไม่สำเร็จ: " + baseDir, e);
        }
    }

    @Transactional
    public void upload(Integer userId, MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("กรุณาเลือกไฟล์รูปภาพ");
        if (file.getSize() > MAX_BYTES)
            throw new IllegalArgumentException("ไฟล์ต้องมีขนาดไม่เกิน 2 MB");

        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED_TYPES.contains(type))
            throw new IllegalArgumentException("รองรับเฉพาะไฟล์ JPG, PNG หรือ WebP");

        User u = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบผู้ใช้"));

        // ตั้งชื่อไฟล์เอง ไม่ใช้ชื่อเดิมจากผู้ใช้ (กัน path traversal เช่น ../../app.jar)
        String ext = switch (type) {
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            default           -> ".jpg";
        };
        String filename = "u" + userId + "-" + UUID.randomUUID().toString().substring(0, 8) + ext;

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, baseDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("บันทึกไฟล์ไม่สำเร็จ กรุณาลองใหม่", e);
        }

        String old = u.getAvatarPath();
        u.setAvatarPath(filename);
        userRepo.save(u);
        deleteQuietly(old);     // ลบไฟล์เก่าทิ้ง ไม่ให้ดิสก์บวม
    }

    @Transactional
    public void remove(Integer userId) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบผู้ใช้"));
        String old = u.getAvatarPath();
        u.setAvatarPath(null);
        userRepo.save(u);
        deleteQuietly(old);
    }

    private void deleteQuietly(String filename) {
        if (filename == null || filename.isBlank()) return;
        try {
            Path p = baseDir.resolve(filename).normalize();
            if (p.startsWith(baseDir)) Files.deleteIfExists(p);
        } catch (IOException e) {
            log.warn("ลบไฟล์รูปเก่าไม่สำเร็จ: {}", filename, e);
        }
    }
}