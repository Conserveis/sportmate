package com.sportmate.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sportmate.service.PostService;
import com.sportmate.service.ReminderService;

/**
 * ตรวจทุก 1 นาที ว่ามีโพสต์ที่ตั้งเวลาเผยแพร่ไว้ถึงกำหนดแล้วหรือยัง
 * ถ้าถึงแล้ว → ยิงแจ้งเตือนให้ผู้ที่สนใจหมวดกีฬานั้น
 */
@Component
public class ScheduledPostNotifier {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPostNotifier.class);

    private final PostService postService;
    private final ReminderService reminderService;

    public ScheduledPostNotifier(PostService postService, ReminderService reminderService) {
        this.postService = postService;
        this.reminderService = reminderService;
    }

    // initialDelay = รอ 20 วิหลังแอปสตาร์ท ค่อยเริ่มรอบแรก (ให้ DB พร้อมก่อน)
    @Scheduled(initialDelay = 20_000, fixedDelay = 60_000)
    public void run() {
        try {
            int n = postService.notifyDuePosts();
            if (n > 0) {
                log.info("แจ้งเตือนโพสต์ที่ตั้งเวลาไว้แล้ว {} โพสต์", n);
            }
        } catch (Exception e) {
            // อย่าให้ scheduler ตายทั้ง thread ถ้ารอบนี้พัง
            log.error("แจ้งเตือนโพสต์ตามเวลาล้มเหลว", e);
        }
    }
    /** แจ้งเตือนล่วงหน้า 24 / 12 / 1 ชม. ก่อนถึงเวลากิจกรรม */
    @Scheduled(initialDelay = 30_000, fixedDelay = 60_000)
    public void reminders() {
        try {
            int n = reminderService.sendDueReminders();
            if (n > 0) {
                log.info("[Reminder] ส่งแจ้งเตือนล่วงหน้าแล้ว {} โพสต์", n);
            }
        } catch (Exception e) {
            log.error("ส่งแจ้งเตือนล่วงหน้าล้มเหลว", e);
        }
    }
}