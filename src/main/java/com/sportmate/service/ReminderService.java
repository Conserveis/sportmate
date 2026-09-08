package com.sportmate.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sportmate.entity.Event;
import com.sportmate.entity.Post;
import com.sportmate.entity.PostReminder;
import com.sportmate.entity.User;
import com.sportmate.repository.EventRepository;
import com.sportmate.repository.PostReminderRepository;
import com.sportmate.repository.PostRepository;

@Service
public class ReminderService {

    /** รอบแจ้งเตือนล่วงหน้า (ชั่วโมง) — เรียงมากไปน้อย */
    private static final int[] STAGES = {24, 12, 1};

    private final PostRepository postRepo;
    private final EventRepository eventRepo;
    private final PostReminderRepository reminderRepo;
    private final NotificationService notificationService;

    public ReminderService(PostRepository postRepo, EventRepository eventRepo,
                           PostReminderRepository reminderRepo,
                           NotificationService notificationService) {
        this.postRepo = postRepo;
        this.eventRepo = eventRepo;
        this.reminderRepo = reminderRepo;
        this.notificationService = notificationService;
    }

    /**
     * หากิจกรรมที่ใกล้ถึงกำหนดแล้วส่งแจ้งเตือนให้ผู้จัด + ผู้เข้าร่วมที่อนุมัติแล้ว
     * @return จำนวนโพสต์ที่ส่งแจ้งเตือนในรอบนี้
     */
    @Transactional
    public int sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Post> upcoming = postRepo.findUpcomingForReminder(now, now.plusHours(STAGES[0]));

        int count = 0;
        for (Post p : upcoming) {
            Set<Integer> alreadySent = new HashSet<>();
            for (PostReminder r : reminderRepo.findByPost(p)) {
                alreadySent.add(r.getHoursBefore());
            }

            long minutesLeft = Duration.between(now, p.getDatePlay()).toMinutes();

            // รอบที่ถึงกำหนดแล้วและยังไม่เคยส่ง
            List<Integer> due = new ArrayList<>();
            for (int h : STAGES) {
                if (minutesLeft <= h * 60L && !alreadySent.contains(h)) due.add(h);
            }
            if (due.isEmpty()) continue;

            // ส่งเฉพาะรอบที่ใกล้ที่สุดรอบเดียว แล้วปิดรอบที่เหลือไปด้วย
            // (กันกรณีโพสต์ถูกสร้างก่อนเวลานัดไม่ถึง 24 ชม. แล้วยิงรวด 3 ข้อความ)
            int stage = Collections.min(due);
            notifyAttendees(p, stage);

            for (int h : due) {
                PostReminder r = new PostReminder();
                r.setPost(p);
                r.setHoursBefore(h);
                r.setSentAt(now);
                reminderRepo.save(r);
            }
            count++;
        }
        return count;
    }

    /** ส่งข้อความเตือนให้ผู้จัดและผู้เข้าร่วมที่อนุมัติแล้ว (ไม่ซ้ำคน) */
    private void notifyAttendees(Post p, int hoursBefore) {
        String when = hoursBefore == 1 ? "อีก 1 ชั่วโมง" : "อีก " + hoursBefore + " ชั่วโมง";
        String msg = when + " จะถึงเวลา \"" + p.getPostName() + "\" ที่ "
                   + p.getLocation().getName();
        String link = "/posts/" + p.getId();

        Set<Integer> sentTo = new HashSet<>();

        User owner = p.getOwner();
        if (owner != null) {
            notificationService.push(owner, msg, link, "reminder");
            sentTo.add(owner.getId());
        }

        for (Event e : eventRepo.findParticipants(p)) {
            if (!"approved".equals(e.getStatus())) continue;   // ยังไม่อนุมัติ ไม่ต้องเตือน
            User u = e.getUser();
            if (u != null && sentTo.add(u.getId())) {
                notificationService.push(u, msg, link, "reminder");
            }
        }
    }
}