package com.sportmate.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sportmate.entity.Event;
import com.sportmate.entity.Location;
import com.sportmate.entity.Post;
import com.sportmate.entity.PostType;
import com.sportmate.entity.Sport;
import com.sportmate.entity.User;
import com.sportmate.repository.EventRepository;
import com.sportmate.repository.LocationRepository;
import com.sportmate.repository.PostRepository;
import com.sportmate.repository.PostTypeRepository;
import com.sportmate.repository.SportRepository;
import com.sportmate.repository.UserRepository;

@Service
public class PostService {

    /** ช่วงเวลาที่อนุญาตให้ตั้งกิจกรรม: 1 ม.ค. 2026 – 31 ธ.ค. 2028 */
    public static final LocalDateTime ALLOWED_FROM = LocalDateTime.of(2026, 1, 1, 0, 0);
    public static final LocalDateTime ALLOWED_TO   = LocalDateTime.of(2028, 12, 31, 23, 59, 59);
    private static final String RANGE_TEXT = "01/01/2026 ถึง 31/12/2028";

    private final PostRepository postRepo;
    private final PostTypeRepository postTypeRepo;
    private final SportRepository sportRepo;
    private final LocationRepository locationRepo;
    private final UserRepository userRepo;
    private final EventRepository eventRepo;
    private final NotificationService notificationService;

    public PostService(PostRepository postRepo, PostTypeRepository postTypeRepo,
                       SportRepository sportRepo, LocationRepository locationRepo,
                       UserRepository userRepo, EventRepository eventRepo,
                       NotificationService notificationService) {
        this.postRepo = postRepo;
        this.postTypeRepo = postTypeRepo;
        this.sportRepo = sportRepo;
        this.locationRepo = locationRepo;
        this.userRepo = userRepo;
        this.eventRepo = eventRepo;
        this.notificationService = notificationService;
    }

    /**
     * ค้นหา/กรอง โพสต์หรือทัวร์นาเมนต์ ตาม กีฬา / สถานที่ / วัน / เวลา
     * @param tournament true = ค้นในทัวร์นาเมนต์, false = ค้นในโพสต์ปกติ
     * @param date       วันที่ (อาจเป็น null)
     * @param time       เวลา (อาจเป็น null — ใช้ร่วมกับ date)
     */
    public List<Post> search(boolean tournament, Integer sportId, Integer locationId,
                             java.time.LocalDate date, java.time.LocalTime time) {
        LocalDateTime fromDt = null, toDt = null;
        if (date != null) {
            fromDt = (time != null) ? date.atTime(time) : date.atStartOfDay();
            toDt = date.atTime(23, 59, 59);
        }
        String ptype = tournament ? PostType.TOURNAMENT : PostType.POST;
        // โพสต์ปกติ = เอาเฉพาะที่ยังไม่หมดเวลา, ทัวร์นาเมนต์ = เอาทั้งหมด
        boolean onlyActive = !tournament;
        return postRepo.search(ptype, LocalDateTime.now(), onlyActive, sportId, locationId, fromDt, toDt);
    }

    /** โพสต์ที่ยัง active สำหรับหน้า Post (หมดเวลาแล้วหายไป) */
    public List<Post> activePosts() {
        return postRepo.findActivePosts(LocalDateTime.now());
    }

    /** ทัวร์นาเมนต์ทั้งหมดสำหรับหน้า Tournament (หมดเวลาแล้วไม่หาย) */
    public List<Post> tournaments() {
        return postRepo.findTournaments(LocalDateTime.now());
    }

    public Post getById(Integer id) {
        return postRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบโพสต์"));
    }

    public List<Post> ownedBy(User owner) {
        return postRepo.findByOwnerOrderByDateCreateDesc(owner);
    }

    /** โควตาโพสต์คงเหลือในสัปดาห์นี้ — Member ไม่จำกัด (คืน -1) */
    public int remainingWeeklyQuota(User owner) {
        if (owner.isMember()) return -1;
        long used = postRepo.countByOwnerSince(owner, LocalDateTime.now().minusWeeks(1));
        return (int) Math.max(0, 3 - used);
    }

        /** ตรวจว่าวันเวลานัด/เวลาเผยแพร่ อยู่ในช่วงปี 2026–2028 และไม่ใช่อดีต */
    private void validateSchedule(LocalDateTime datePlay, LocalDateTime publishAt) {
        if (datePlay == null)
            throw new IllegalArgumentException("กรุณาระบุวันและเวลานัด");
        if (datePlay.isBefore(ALLOWED_FROM) || datePlay.isAfter(ALLOWED_TO))
            throw new IllegalArgumentException("วันและเวลานัดต้องอยู่ระหว่าง " + RANGE_TEXT);
        if (datePlay.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("ไม่สามารถตั้งวันและเวลานัดย้อนหลังได้");

        if (publishAt != null) {
            if (publishAt.isBefore(ALLOWED_FROM) || publishAt.isAfter(ALLOWED_TO))
                throw new IllegalArgumentException("วันเวลาเผยแพร่ต้องอยู่ระหว่าง " + RANGE_TEXT);
            if (!publishAt.isBefore(datePlay))
                throw new IllegalArgumentException("วันเวลาเผยแพร่ต้องมาก่อนวันและเวลานัด");
        }
    }

    /**
     * สร้างโพสต์หรือทัวร์นาเมนต์
     * - Tournament: เฉพาะ Member 
     * - Post Normal User จำกัด 3 โพสต์/สัปดาห์, Member ไม่จำกัด
     */
    @Transactional
    public Post create(User owner, String type, Integer sportId, Integer locationId,
                       String postName, String description, LocalDateTime datePlay,
                       Integer maxPlayer, Integer minPlayer, boolean isPublic,
                       LocalDateTime publishAt) {

        validateSchedule(datePlay, publishAt);
        boolean tournament = PostType.TOURNAMENT.equals(type);

        if (tournament && !owner.isMember()) {
            throw new IllegalStateException("การสร้างทัวร์นาเมนต์จำกัดเฉพาะสมาชิกรายเดือนเท่านั้น");
        }
        if (!tournament && !owner.isMember()) {
            long since = postRepo.countByOwnerSince(owner, LocalDateTime.now().minusWeeks(1));
            if (since >= 3) {
                throw new IllegalStateException("ผู้ใช้ทั่วไปสร้างโพสต์ได้สูงสุด 3 ครั้งต่อสัปดาห์ (สมัครสมาชิกเพื่อโพสต์ไม่จำกัด)");
            }
        }
        if (minPlayer == null || minPlayer < 3) {
            throw new IllegalArgumentException("จำนวนคนขั้นต่ำต้องตั้งแต่ 3 คนขึ้นไป");
        }
        if (maxPlayer == null || maxPlayer < minPlayer) {
            throw new IllegalArgumentException("จำนวนคนสูงสุดต้องไม่น้อยกว่าจำนวนขั้นต่ำ");
        }

        PostType pt = postTypeRepo.findByName(tournament ? PostType.TOURNAMENT : PostType.POST)
                .orElseThrow(() -> new IllegalStateException("ไม่พบ PostType"));
        Sport sport = sportRepo.findById(sportId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบกีฬา"));
        Location location = locationRepo.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบสถานที่"));

        Post p = new Post();
        p.setOwner(owner);
        p.setPostType(pt);
        p.setSport(sport);
        p.setLocation(location);
        p.setPostName(postName);
        p.setDescription(description);
        p.setDatePlay(datePlay);
        p.setDateCreate(LocalDateTime.now());
        p.setPublishAt(publishAt);   // null = เผยแพร่ทันที
        p.setMaxPlayer(maxPlayer);
        p.setMinPlayer(minPlayer);
        p.setPublic(isPublic);
        p.setStatus("open");
        Post saved = postRepo.save(p);

        // แจ้งเตือนผู้ที่ติดตามหมวดกีฬานี้ว่ามีกิจกรรมใหม่ 
        if (publishAt == null) {
            String label = tournament ? "ทัวร์นาเมนต์" : "กิจกรรม";
            for (User follower : userRepo.findFollowersOfSport(sportId)) {
                if (!follower.getId().equals(owner.getId())) {
                    notificationService.push(follower,
                            label + sport.getName() + "ใหม่: \"" + postName + "\"",
                            "/posts/" + saved.getId(), "new_post");
                }
            }
        }
            return saved;
    }

    /**
     * แก้ไขรายละเอียดโพสต์ — เฉพาะเจ้าของเท่านั้น
     * แก้ไม่ได้ถ้า: ถูกยกเลิกแล้ว / เลยวันจัดกิจกรรมแล้ว
     * เปลี่ยนประเภท (Post <-> Tournament) ไม่ได้
     */
    @Transactional
    public Post update(Integer postId, User requester, Integer sportId, Integer locationId,
                       String postName, String description, LocalDateTime datePlay,
                       Integer maxPlayer, Integer minPlayer, boolean isPublic,
                       LocalDateTime publishAt) {

        Post p = getById(postId);

        if (!p.getOwner().getId().equals(requester.getId()))
            throw new IllegalStateException("เฉพาะเจ้าของโพสต์เท่านั้นที่แก้ไขได้");
        if ("cancelled".equals(p.getStatus()))
            throw new IllegalStateException("กิจกรรมนี้ถูกยกเลิกแล้ว ไม่สามารถแก้ไขได้");
        if (p.isExpired())
            throw new IllegalStateException("กิจกรรมนี้ผ่านไปแล้ว ไม่สามารถแก้ไขได้");

        validateSchedule(datePlay, publishAt);

        if (minPlayer == null || minPlayer < 3)
            throw new IllegalArgumentException("จำนวนคนขั้นต่ำต้องตั้งแต่ 3 คนขึ้นไป");
        if (maxPlayer == null || maxPlayer < minPlayer)
            throw new IllegalArgumentException("จำนวนคนสูงสุดต้องไม่น้อยกว่าจำนวนขั้นต่ำ");

        long joined = eventRepo.countApprovedJoins(p);
        if (maxPlayer < joined)
            throw new IllegalStateException(
                    "ลดจำนวนสูงสุดเหลือ " + maxPlayer + " ไม่ได้ เพราะมีผู้เข้าร่วมแล้ว " + joined + " คน");

        Sport sport = sportRepo.findById(sportId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบกีฬา"));
        Location location = locationRepo.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบสถานที่"));

        // เก็บค่าเดิมไว้เทียบ เพื่อแจ้งเตือนเฉพาะตอนที่ข้อมูลสำคัญเปลี่ยนจริง
        LocalDateTime oldDatePlay = p.getDatePlay();
        Integer oldLocationId = p.getLocation().getId();

        p.setSport(sport);
        p.setLocation(location);
        p.setPostName(postName);
        p.setDescription(description);
        p.setDatePlay(datePlay);
        p.setMaxPlayer(maxPlayer);
        p.setMinPlayer(minPlayer);
        p.setPublic(isPublic);
        // แก้เวลาเผยแพร่ได้เฉพาะตอนที่ยังไม่ถูกเผยแพร่
        if (p.getPublishAt() != null && p.getPublishAt().isAfter(LocalDateTime.now())) {
            p.setPublishAt(publishAt);
        }
        Post saved = postRepo.save(p);

        boolean important = !java.util.Objects.equals(oldDatePlay, datePlay)
                || !java.util.Objects.equals(oldLocationId, location.getId());
        if (important) {
            for (Event ev : eventRepo.findParticipants(saved)) {
                notificationService.push(ev.getUser(),
                        "ผู้จัดแก้ไขรายละเอียดกิจกรรม \"" + saved.getPostName() + "\" (วัน/เวลา หรือสถานที่เปลี่ยน)",
                        "/posts/" + saved.getId(), "post_updated");
            }
        }
        return saved;
    }

    @Transactional
    public void cancel(Integer postId, User requester) {
        Post p = getById(postId);
        if (!p.getOwner().getId().equals(requester.getId())) {
            throw new IllegalStateException("เฉพาะเจ้าของโพสต์เท่านั้นที่ยกเลิกได้");
        }
        if ("cancelled".equals(p.getStatus())) {
            throw new IllegalStateException("กิจกรรมนี้ถูกยกเลิกไปแล้ว");
        }
        if (p.isCancelLocked()) {
            throw new IllegalStateException(
                    "ไม่สามารถยกเลิกโพสต์ได้ ต้องยกเลิกก่อนวันจัดกิจกรรมอย่างน้อย 1 วัน");
        }
        p.setStatus("cancelled");
        postRepo.save(p);

        // แจ้งเตือนผู้เข้าร่วมทุกคนว่ากิจกรรมถูกยกเลิก
        for (Event ev : eventRepo.findParticipants(p)) {
            notificationService.push(ev.getUser(),
                    "กิจกรรม \"" + p.getPostName() + "\" ถูกยกเลิกโดยผู้จัด",
                    "/posts/" + p.getId(), "post_cancelled");
        }
    }
}
