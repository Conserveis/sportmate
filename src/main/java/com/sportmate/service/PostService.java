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

    @Transactional
    public void cancel(Integer postId, User requester) {
        Post p = getById(postId);
        if (!p.getOwner().getId().equals(requester.getId())) {
            throw new IllegalStateException("เฉพาะเจ้าของโพสต์เท่านั้นที่ยกเลิกได้");
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
