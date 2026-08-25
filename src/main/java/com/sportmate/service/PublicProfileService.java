package com.sportmate.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sportmate.dto.PublicProfile;
import com.sportmate.dto.SportCount;
import com.sportmate.entity.Event;
import com.sportmate.entity.Post;
import com.sportmate.entity.Review;
import com.sportmate.entity.User;
import com.sportmate.repository.EventRepository;
import com.sportmate.repository.PostRepository;
import com.sportmate.repository.ReviewRepository;
import com.sportmate.repository.UserRepository;

/**
 * ประกอบข้อมูลโปรไฟล์สาธารณะของผู้ใช้ (UC: ดูโปรไฟล์ผู้จัด / ดูโปรไฟล์ผู้เข้าร่วม)
 *
 * ผู้เข้าร่วมใช้ดู "ประวัติการจัด + รีวิว + จำนวนครั้งที่จัด" ของผู้จัด เพื่อประเมินก่อนกดเข้าร่วม
 * ผู้จัดใช้ดู "จำนวนครั้งการเข้าร่วม + กีฬาที่เข้าร่วมมากที่สุด" ของผู้สมัคร เพื่อประกอบการอนุมัติ
 */
@Service
public class PublicProfileService {

    /** จำนวนรายการสูงสุดที่แสดงในแต่ละส่วน (กันหน้าโปรไฟล์ยาวเกินไป) */
    private static final int LIST_LIMIT = 10;

    private final UserRepository userRepo;
    private final PostRepository postRepo;
    private final EventRepository eventRepo;
    private final ReviewRepository reviewRepo;

    public PublicProfileService(UserRepository userRepo, PostRepository postRepo,
                                EventRepository eventRepo, ReviewRepository reviewRepo) {
        this.userRepo = userRepo;
        this.postRepo = postRepo;
        this.eventRepo = eventRepo;
        this.reviewRepo = reviewRepo;
    }

    @Transactional(readOnly = true)
    public PublicProfile build(Integer userId) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบผู้ใช้ที่ต้องการดูโปรไฟล์"));
        return build(u);
    }

    @Transactional(readOnly = true)
    public PublicProfile build(User u) {
        LocalDateTime now = LocalDateTime.now();

        // ---- ฝั่งผู้จัดกิจกรรม ----
        long organized = postRepo.countOrganizedBy(u);
        long finished = postRepo.countFinishedBy(u, now);
        long cancelled = postRepo.countCancelledBy(u);
        long reviewCount = reviewRepo.countReviewsForOwner(u);
        List<Post> organizedPosts = limit(postRepo.findPublicOrganizedBy(u, now));
        List<Review> reviews = limit(reviewRepo.findReviewsForOwner(u));

        // ---- ฝั่งผู้เข้าร่วม ----
        long joins = eventRepo.countJoinsByUser(u);
        long attended = eventRepo.countAttendedByUser(u, now);
        long upcoming = Math.max(0, joins - attended);
        List<SportCount> sportCounts = eventRepo.countJoinsBySport(u);
        List<Event> recentJoined = limit(eventRepo.findJoinedByUser(u));

        return new PublicProfile(u,
                organized, finished, cancelled, reviewCount, u.getAvgScore(),
                organizedPosts, reviews,
                joins, attended, upcoming, sportCounts, recentJoined);
    }

    private <T> List<T> limit(List<T> list) {
        return list.size() <= LIST_LIMIT ? list : List.copyOf(list.subList(0, LIST_LIMIT));
    }
}
