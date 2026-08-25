package com.sportmate.service;

import com.sportmate.entity.Event;
import com.sportmate.entity.Review;
import com.sportmate.entity.User;
import com.sportmate.repository.EventRepository;
import com.sportmate.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final EventRepository eventRepo;

    public ReviewService(ReviewRepository reviewRepo, EventRepository eventRepo) {
        this.reviewRepo = reviewRepo;
        this.eventRepo = eventRepo;
    }

    public boolean hasReviewed(Integer eventId) {
        return reviewRepo.findByEvent_Id(eventId).isPresent();
    }

    /**
     * รีวิวผู้จัด (UC-6): ต้องเป็นผู้เข้าร่วมกิจกรรมนั้น, กิจกรรมจบแล้ว และยังไม่เคยรีวิว
     * เมื่อบันทึก Review ทริกเกอร์ในฐานข้อมูลจะอัปเดต AvgScore ของเจ้าของให้อัตโนมัติ
     */
    @Transactional
    public void review(User reviewer, Integer eventId, int score, String comment) {
        Event e = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบข้อมูลการเข้าร่วม"));
        if (!e.getUser().getId().equals(reviewer.getId()))
            throw new IllegalStateException("ไม่มีสิทธิ์รีวิวการเข้าร่วมนี้");
        if (!"approved".equals(e.getStatus()))
            throw new IllegalStateException("เฉพาะผู้ที่ได้รับการอนุมัติเข้าร่วมเท่านั้นที่สามารถรีวิวได้");
        if (!e.getPost().getDatePlay().isBefore(LocalDateTime.now()))
            throw new IllegalStateException("กิจกรรมยังไม่สิ้นสุด ไม่สามารถรีวิวได้");
        if (score < 1 || score > 5)
            throw new IllegalArgumentException("คะแนนต้องอยู่ระหว่าง 1-5");
        if (hasReviewed(eventId))
            throw new IllegalStateException("คุณรีวิวกิจกรรมนี้ไปแล้ว");

        Review r = new Review();
        r.setEvent(e);
        r.setReviewScore(score);
        r.setComment(comment);
        r.setReviewDate(LocalDateTime.now());
        reviewRepo.save(r);
    }
}
