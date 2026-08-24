package com.sportmate.service;

import com.sportmate.entity.Event;
import com.sportmate.entity.Post;
import com.sportmate.entity.User;
import com.sportmate.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepo;
    private final NotificationService notificationService;

    public EventService(EventRepository eventRepo, NotificationService notificationService) {
        this.eventRepo = eventRepo;
        this.notificationService = notificationService;
    }

    public long countJoins(Post post) { return eventRepo.countActiveJoins(post); }

    public List<Event> participants(Post post) { return eventRepo.findParticipants(post); }

    public boolean hasJoined(User user, Post post) {
        return eventRepo.findByUserAndPost(user, post)
                .map(e -> e.getStatus().equals("pending") || e.getStatus().equals("approved"))
                .orElse(false);
    }

    /** เข้าร่วมกิจกรรม (UC-7) */
    @Transactional
    public void join(User user, Post post) {
        if ("cancelled".equals(post.getStatus()))
            throw new IllegalStateException("กิจกรรมนี้ถูกยกเลิกแล้ว");
        if (post.isExpired())
            throw new IllegalStateException("กิจกรรมนี้หมดเวลารับสมัครแล้ว");
        if (post.getOwner().getId().equals(user.getId()))
            throw new IllegalStateException("คุณเป็นเจ้าของกิจกรรมนี้อยู่แล้ว");

        long current = eventRepo.countActiveJoins(post);
        if (current >= post.getMaxPlayer())
            throw new IllegalStateException("จำนวนผู้เข้าร่วมเต็มแล้ว");

        Optional<Event> existing = eventRepo.findByUserAndPost(user, post);
        Event e = existing.orElseGet(Event::new);
        e.setUser(user);
        e.setPost(post);
        e.setEventName(post.getPostName());
        e.setJoinDate(LocalDateTime.now());
        e.setCancelDate(null);
        // โพสต์สาธารณะ = เข้าร่วมทันที (approved), ส่วนตัว = รออนุมัติ (pending)
        e.setStatus(post.isPublic() ? "approved" : "pending");
        eventRepo.save(e);

        // แจ้งเตือนเจ้าของกิจกรรมว่ามีผู้เข้าร่วมใหม่ (UC-4 FR16/FR17)
        long joined = eventRepo.countActiveJoins(post);
        long need = Math.max(0, post.getMinPlayer() - joined);
        String msg = post.isPublic()
                ? user.getUserName() + " เข้าร่วม \"" + post.getPostName() + "\" (ตอนนี้ " + joined + "/" + post.getMaxPlayer()
                    + (need > 0 ? " ยังขาดอีก " + need + " คน)" : ")")
                : user.getUserName() + " ขอเข้าร่วม \"" + post.getPostName() + "\" — รอการอนุมัติ";
        notificationService.push(post.getOwner(), msg, "/posts/" + post.getId(), "join");
    }

    /** ยกเลิกการเข้าร่วม (UC-7 9a) */
    @Transactional
    public void cancelJoin(User user, Post post) {
        Event e = eventRepo.findByUserAndPost(user, post)
                .orElseThrow(() -> new IllegalStateException("คุณยังไม่ได้เข้าร่วมกิจกรรมนี้"));
        e.setStatus("cancelled");
        e.setCancelDate(LocalDateTime.now());
        eventRepo.save(e);

        // แจ้งเตือนเจ้าของว่ามีคนยกเลิก + จำนวนล่าสุด (UC-4 FR20)
        long joined = eventRepo.countActiveJoins(post);
        long need = Math.max(0, post.getMinPlayer() - joined);
        String msg = user.getUserName() + " ยกเลิกการเข้าร่วม \"" + post.getPostName() + "\" (ตอนนี้ "
                + joined + "/" + post.getMaxPlayer()
                + (need > 0 ? " ยังขาดอีก " + need + " คน)" : ")");
        notificationService.push(post.getOwner(), msg, "/posts/" + post.getId(), "cancel_join");
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
