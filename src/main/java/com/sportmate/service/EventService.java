package com.sportmate.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sportmate.entity.Event;
import com.sportmate.entity.Post;
import com.sportmate.entity.User;
import com.sportmate.repository.EventRepository;

@Service
public class EventService {

    private final EventRepository eventRepo;
    private final NotificationService notificationService;

    public EventService(EventRepository eventRepo, NotificationService notificationService) {
        this.eventRepo = eventRepo;
        this.notificationService = notificationService;
    }

    public long countJoins(Post post) { return eventRepo.countApprovedJoins(post); }

    public List<Event> participants(Post post) { return eventRepo.findParticipants(post); }

    /** ผู้ใช้นี้ได้รับการอนุมัติเข้าร่วมแล้วหรือไม่ */
    public boolean hasJoined(User user, Post post) {
        return eventRepo.findByUserAndPost(user, post)
                .map(e -> "approved".equals(e.getStatus()))
                .orElse(false);
    }

    /** ผู้ใช้นี้กำลังรอการอนุมัติอยู่หรือไม่ */
    public boolean isPending(User user, Post post) {
        return eventRepo.findByUserAndPost(user, post)
                .map(e -> "pending".equals(e.getStatus()))
                .orElse(false);
    }

    /** เข้าร่วมกิจกรรม */
    @Transactional
    public void join(User user, Post post) {
        if ("cancelled".equals(post.getStatus()))
            throw new IllegalStateException("กิจกรรมนี้ถูกยกเลิกแล้ว");
        if (post.isExpired())
            throw new IllegalStateException("กิจกรรมนี้หมดเวลารับสมัครแล้ว");
        if (post.getOwner().getId().equals(user.getId()))
            throw new IllegalStateException("คุณเป็นเจ้าของกิจกรรมนี้อยู่แล้ว");

        long currentApproved = eventRepo.countApprovedJoins(post);
        if (currentApproved >= post.getMaxPlayer())
            throw new IllegalStateException("จำนวนผู้เข้าร่วมเต็มแล้ว");

        Optional<Event> existing = eventRepo.findByUserAndPost(user, post);
        if (existing.isPresent()) {
            Event prev = existing.get();
            if ("approved".equals(prev.getStatus())) {
                throw new IllegalStateException("คุณเข้าร่วมกิจกรรมนี้แล้ว");
            }
            if ("pending".equals(prev.getStatus())) {
                throw new IllegalStateException("คุณได้ส่งคำขอเข้าร่วมไปแล้ว กรุณารอการอนุมัติจากผู้จัด");
            }
        }

        Event e = existing.orElseGet(Event::new);
        e.setUser(user);
        e.setPost(post);
        e.setEventName(post.getPostName());
        e.setJoinDate(LocalDateTime.now());
        e.setCancelDate(null);
        // โพสต์สาธารณะ = เข้าร่วมทันที (approved), ส่วนตัว = รออนุมัติ (pending)
        e.setStatus(post.isPublic() ? "approved" : "pending");
        eventRepo.save(e);

        if (post.isPublic()) {
            // โพสต์สาธารณะ: นับเป็นเข้าร่วมทันที และแจ้งเตือนเจ้าของ
            long joined = eventRepo.countApprovedJoins(post);
            long need = Math.max(0, post.getMinPlayer() - joined);
            String msg = user.getUserName() + " เข้าร่วม \"" + post.getPostName() + "\" (ตอนนี้ " + joined + "/" + post.getMaxPlayer()
                    + (need > 0 ? " ยังขาดอีก " + need + " คน)" : ")");
            notificationService.push(post.getOwner(), msg, "/posts/" + post.getId(), "join");
        } else {
            // โพสต์ส่วนตัว: ยังไม่นับจนกว่าผู้สร้างโพสต์จะอนุมัติ
            String msg = user.getUserName() + " ขอเข้าร่วม \"" + post.getPostName() + "\" (รอคุณอนุมัติ)";
            notificationService.push(post.getOwner(), msg, "/posts/" + post.getId(), "join_request");
        }
    }

    /**
     * อนุมัติคำขอเข้าร่วม — เฉพาะผู้สร้างโพสต์เท่านั้นที่มีสิทธิ์
     */
    @Transactional
    public void approve(User owner, Post post, Integer targetUserId) {
        if (!post.getOwner().getId().equals(owner.getId())) {
            throw new IllegalStateException("เฉพาะผู้สร้างโพสต์เท่านั้นที่สามารถอนุมัติได้");
        }
        User targetUser = new User();
        targetUser.setId(targetUserId);
        Event e = eventRepo.findByUserAndPost(targetUser, post)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบคำขอเข้าร่วมของผู้ใช้นี้"));

        if ("approved".equals(e.getStatus())) {
            throw new IllegalStateException("ผู้ใช้นี้ได้รับการอนุมัติไปแล้ว");
        }
        if (!"pending".equals(e.getStatus())) {
            throw new IllegalStateException("สถานะคำขอไม่ถูกต้อง");
        }

        long currentApproved = eventRepo.countApprovedJoins(post);
        if (currentApproved >= post.getMaxPlayer()) {
            throw new IllegalStateException("จำนวนผู้เข้าร่วมเต็มแล้ว ไม่สามารถอนุมัติเพิ่มได้");
        }

        e.setStatus("approved");
        eventRepo.save(e);

        // แจ้งเตือนผู้ขอเข้าร่วมว่าได้รับการอนุมัติแล้ว
        String msg = "ผู้จัดกิจกรรมได้อนุมัติให้คุณเข้าร่วม \"" + post.getPostName() + "\" แล้ว";
        notificationService.push(e.getUser(), msg, "/posts/" + post.getId(), "join_approved");
    }

    /**
     * ปฏิเสธคำขอเข้าร่วม — เฉพาะผู้สร้างโพสต์เท่านั้นที่มีสิทธิ์
     */
    @Transactional
    public void reject(User owner, Post post, Integer targetUserId) {
        if (!post.getOwner().getId().equals(owner.getId())) {
            throw new IllegalStateException("เฉพาะผู้สร้างโพสต์เท่านั้นที่สามารถปฏิเสธได้");
        }
        User targetUser = new User();
        targetUser.setId(targetUserId);
        Event e = eventRepo.findByUserAndPost(targetUser, post)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบคำขอเข้าร่วมของผู้ใช้นี้"));

        if (!"pending".equals(e.getStatus())) {
            throw new IllegalStateException("สถานะคำขอไม่ถูกต้อง");
        }

        e.setStatus("rejected");
        e.setCancelDate(LocalDateTime.now());
        eventRepo.save(e);

        // แจ้งเตือนผู้ขอเข้าร่วมว่าไม่ได้รับการอนุมัติ
        String msg = "คำขอเข้าร่วมกิจกรรม \"" + post.getPostName() + "\" ไม่ได้รับการอนุมัติ";
        notificationService.push(e.getUser(), msg, "/posts/" + post.getId(), "join_rejected");
    }

    /** ยกเลิกการเข้าร่วม / ยกเลิกคำขอ */
    @Transactional
    public void cancelJoin(User user, Post post) {
        Event e = eventRepo.findByUserAndPost(user, post)
                .orElseThrow(() -> new IllegalStateException("คุณยังไม่ได้เข้าร่วมกิจกรรมนี้"));
        boolean wasApproved = "approved".equals(e.getStatus());
        e.setStatus("cancelled");
        e.setCancelDate(LocalDateTime.now());
        eventRepo.save(e);

        if (wasApproved) {
            // แจ้งเตือนเจ้าของว่ามีคนยกเลิก + จำนวนล่าสุด
            long joined = eventRepo.countApprovedJoins(post);
            long need = Math.max(0, post.getMinPlayer() - joined);
            String msg = user.getUserName() + " ยกเลิกการเข้าร่วม \"" + post.getPostName() + "\" (ตอนนี้ "
                    + joined + "/" + post.getMaxPlayer()
                    + (need > 0 ? " ยังขาดอีก " + need + " คน)" : ")");
            notificationService.push(post.getOwner(), msg, "/posts/" + post.getId(), "cancel_join");
        } else {
            String msg = user.getUserName() + " ยกเลิกคำขอเข้าร่วม \"" + post.getPostName() + "\"";
            notificationService.push(post.getOwner(), msg, "/posts/" + post.getId(), "cancel_join");
        }
    }

    public List<Event> joinedByUser(User user) {
        return eventRepo.findJoinedByUser(user);
    }

    public List<Event> upcomingByUser(User user) {
        return eventRepo.findUpcomingByUser(user, LocalDateTime.now());
    }

    /** หน้าจัดเก็บกิจกรรม: โพสต์ที่เข้าร่วมแล้วและหมดเวลาเข้าร่วม */
    public List<Event> archivedByUser(User user) {
        return eventRepo.findArchivedByUser(user, LocalDateTime.now());
    }
}
