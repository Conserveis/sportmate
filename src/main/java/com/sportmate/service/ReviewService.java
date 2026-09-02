package com.sportmate.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sportmate.entity.Event;
import com.sportmate.entity.Review;
import com.sportmate.entity.User;
import com.sportmate.repository.EventRepository;
import com.sportmate.repository.ReviewRepository;

@Service
public class ReviewService {

    private static final String TO_OWNER = "to_owner";
    private static final String TO_PARTICIPANT = "to_participant";

    private final ReviewRepository reviewRepo;
    private final EventRepository eventRepo;

    public ReviewService(ReviewRepository reviewRepo, EventRepository eventRepo) {
        this.reviewRepo = reviewRepo;
        this.eventRepo = eventRepo;
    }

    /** ผู้เข้าร่วมรีวิวผู้จัดไปแล้วหรือยัง */
    public boolean hasReviewed(Integer eventId) {
        return reviewRepo.findByEvent_IdAndDirection(eventId, TO_OWNER).isPresent();
    }

    /** ผู้จัดรีวิวผู้เข้าร่วมคนนี้ไปแล้วหรือยัง */
    public boolean hasReviewedParticipant(Integer eventId) {
        return reviewRepo.findByEvent_IdAndDirection(eventId, TO_PARTICIPANT).isPresent();
    }

    /** ผู้เข้าร่วม → รีวิวผู้จัด */
    @Transactional
    public void review(User reviewer, Integer eventId, int score, String comment) {
        Event e = loadFinishedEvent(eventId);
        if (!e.getUser().getId().equals(reviewer.getId()))
            throw new IllegalStateException("ไม่มีสิทธิ์รีวิวการเข้าร่วมนี้");
        if (!"approved".equals(e.getStatus()))
            throw new IllegalStateException("เฉพาะผู้ที่ได้รับการอนุมัติเข้าร่วมเท่านั้นที่สามารถรีวิวได้");
        validateScore(score);
        if (hasReviewed(eventId))
            throw new IllegalStateException("คุณรีวิวกิจกรรมนี้ไปแล้ว");

        save(e, score, comment, TO_OWNER);
    }

    /** ผู้จัด → รีวิวผู้เข้าร่วม */
    @Transactional
    public void reviewParticipant(User owner, Integer postId, Integer eventId, int score, String comment) {
        Event e = loadFinishedEvent(eventId);
        if (!e.getPost().getId().equals(postId))
            throw new IllegalArgumentException("ข้อมูลผู้เข้าร่วมไม่ตรงกับกิจกรรม");
        if (!e.getPost().getOwner().getId().equals(owner.getId()))
            throw new IllegalStateException("เฉพาะผู้จัดกิจกรรมเท่านั้นที่รีวิวผู้เข้าร่วมได้");
        if (!"approved".equals(e.getStatus()))
            throw new IllegalStateException("รีวิวได้เฉพาะผู้เข้าร่วมที่ได้รับการอนุมัติแล้ว");
        validateScore(score);
        if (hasReviewedParticipant(eventId))
            throw new IllegalStateException("คุณรีวิวผู้เข้าร่วมคนนี้ไปแล้ว");

        save(e, score, comment, TO_PARTICIPANT);
    }

    // ---- สถิติฝั่งผู้เข้าร่วม (ใช้แสดงบนโปรไฟล์) ----
    public List<Review> reviewsForParticipant(User user) {
        return reviewRepo.findReviewsForParticipant(user);
    }

    public long countReviewsForParticipant(User user) {
        return reviewRepo.countReviewsForParticipant(user);
    }

    public BigDecimal avgScoreForParticipant(User user) {
        Double avg = reviewRepo.avgScoreForParticipant(user);
        return avg == null ? BigDecimal.ZERO
                : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
    }

    // ---- helper ----
        private Event loadFinishedEvent(Integer eventId) {
        Event e = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบข้อมูลการเข้าร่วม"));
        if ("cancelled".equals(e.getPost().getStatus()))
            throw new IllegalStateException("กิจกรรมนี้ถูกยกเลิกโดยผู้จัด ไม่สามารถให้คะแนนได้");
        if ("cancelled".equals(e.getStatus()))
            throw new IllegalStateException("คุณยกเลิกการเข้าร่วมกิจกรรมนี้ไปแล้ว ไม่สามารถให้คะแนนได้");
        if (!e.getPost().getDatePlay().isBefore(LocalDateTime.now()))
            throw new IllegalStateException("กิจกรรมยังไม่สิ้นสุด ไม่สามารถรีวิวได้");
        return e;
    }

    private void validateScore(int score) {
        if (score < 1 || score > 5)
            throw new IllegalArgumentException("คะแนนต้องอยู่ระหว่าง 1-5");
    }

    private void save(Event e, int score, String comment, String direction) {
        Review r = new Review();
        r.setEvent(e);
        r.setReviewScore(score);
        r.setComment(comment == null || comment.isBlank() ? null : comment.trim());
        r.setReviewDate(LocalDateTime.now());
        r.setDirection(direction);
        reviewRepo.save(r);
    }
}