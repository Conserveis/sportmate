package com.sportmate.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sportmate.service.PostService;

/**
 * ตรวจทุก 1 นาที ว่ามีโพสต์ที่ตั้งเวลาเผยแพร่ไว้ถึงกำหนดแล้วหรือยัง
 * ถ้าถึงแล้ว → ยิงแจ้งเตือนให้ผู้ที่สนใจหมวดกีฬานั้น
 */
@Component
public class ScheduledPostNotifier {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPostNotifier.class);

    private final PostService postService;

    public ScheduledPostNotifier(PostService postService) {
        this.postService = postService;
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
}