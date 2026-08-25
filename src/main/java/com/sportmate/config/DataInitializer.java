package com.sportmate.config;

import com.sportmate.entity.*;
import com.sportmate.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final UserTypeRepository userTypeRepo;
    private final SportRepository sportRepo;
    private final LocationRepository locationRepo;
    private final PostTypeRepository postTypeRepo;
    private final PostRepository postRepo;
    private final PasswordEncoder encoder;
        private final JdbcTemplate jdbcTemplate;

    public DataInitializer(UserRepository userRepo, UserTypeRepository userTypeRepo,
                           SportRepository sportRepo, LocationRepository locationRepo,
                           PostTypeRepository postTypeRepo, PostRepository postRepo,
                           PasswordEncoder encoder, JdbcTemplate jdbcTemplate) {
        this.userRepo = userRepo;
        this.userTypeRepo = userTypeRepo;
        this.sportRepo = sportRepo;
        this.locationRepo = locationRepo;
        this.postTypeRepo = postTypeRepo;
        this.postRepo = postRepo;
        this.encoder = encoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
                ensureOAuthColumns();
        if (userRepo.count() > 0) return;   // seed เฉพาะครั้งแรก

        UserType normal = userTypeRepo.findByName(UserType.NORMAL).orElseThrow();
        UserType member = userTypeRepo.findByName(UserType.MEMBER).orElseThrow();

        User demo = new User();
        demo.setUserName("demo");
        demo.setGmail("demo@example.com");
        demo.setPassword(encoder.encode("password123"));
        demo.setUserType(normal);
        demo.setEmailVerified(true);
        demo.setAvgScore(BigDecimal.ZERO);
        demo.setCreatedAt(LocalDateTime.now());
        userRepo.save(demo);

        User memberUser = new User();
        memberUser.setUserName("member");
        memberUser.setGmail("member@example.com");
        memberUser.setPassword(encoder.encode("password123"));
        memberUser.setUserType(member);
        memberUser.setMembershipExpireAt(LocalDateTime.now().plusMonths(1));
        memberUser.setEmailVerified(true);
        memberUser.setAvgScore(BigDecimal.ZERO);
        memberUser.setCreatedAt(LocalDateTime.now());
        userRepo.save(memberUser);

        var sports = sportRepo.findAll();
        var locations = locationRepo.findAll();
        if (sports.isEmpty() || locations.isEmpty()) return;

        PostType postType = postTypeRepo.findByName(PostType.POST).orElseThrow();
        PostType tourType = postTypeRepo.findByName(PostType.TOURNAMENT).orElseThrow();

        // โพสต์ที่ยัง active
        savePost(memberUser, postType, sports.get(0), locations.get(0),
                "หาเพื่อนเตะบอล 5v5 เย็นนี้", "มาสนุกกันครับ ขาดอีก 4 คน",
                LocalDateTime.now().plusDays(1), 10, 6, true);
        savePost(demo, postType, sports.get(2), locations.get(2),
                "แบดมินตันคู่ผสม", "ระดับกลาง สนุก ๆ",
                LocalDateTime.now().plusDays(2), 6, 4, true);
        // โพสต์ที่หมดเวลาแล้ว (จะไม่โผล่ในหน้า Post)
        savePost(demo, postType, sports.get(3), locations.get(5),
                "วิ่งเช้าเมื่อวาน (หมดเวลาแล้ว)", "โพสต์นี้หมดเวลา ควรหายจากหน้า Post",
                LocalDateTime.now().minusDays(1), 8, 3, true);

        // ทัวร์นาเมนต์ (สมาชิกเท่านั้นที่สร้างได้ — ไม่หายแม้หมดเวลา)
        savePost(memberUser, tourType, sports.get(1), locations.get(0),
                "บาสเกตบอลทัวร์นาเมนต์ประจำเดือน", "ทีมละ 5 คน ชิงถ้วยรางวัล",
                LocalDateTime.now().plusDays(7), 40, 10, true);
    }

        private void ensureOAuthColumns() {
                addColumnIfMissing("AuthProvider", "VARCHAR(20) NOT NULL DEFAULT 'local'");
                addColumnIfMissing("ProviderId", "VARCHAR(255) NULL");
                jdbcTemplate.execute("ALTER TABLE `User` MODIFY COLUMN Password VARCHAR(255) NULL");
        }

        private void addColumnIfMissing(String columnName, String definition) {
                Integer count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM information_schema.columns "
                                                + "WHERE table_schema = DATABASE() AND table_name = 'User' AND column_name = ?",
                                Integer.class, columnName);
                if (count != null && count == 0) {
                        jdbcTemplate.execute("ALTER TABLE `User` ADD COLUMN " + columnName + " " + definition);
                }
        }

    private void savePost(User owner, PostType type, Sport sport, Location loc,
                          String name, String desc, LocalDateTime datePlay,
                          int max, int min, boolean isPublic) {
        Post p = new Post();
        p.setOwner(owner);
        p.setPostType(type);
        p.setSport(sport);
        p.setLocation(loc);
        p.setPostName(name);
        p.setDescription(desc);
        p.setDatePlay(datePlay);
        p.setDateCreate(LocalDateTime.now());
        p.setMaxPlayer(max);
        p.setMinPlayer(min);
        p.setPublic(isPublic);
        p.setStatus("open");
        postRepo.save(p);
    }
}
