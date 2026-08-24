package com.sportmate.service;

import com.sportmate.entity.Notification;
import com.sportmate.entity.User;
import com.sportmate.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    /** สร้างการแจ้งเตือน 1 รายการให้ผู้รับ */
    @Transactional
    public void push(User recipient, String message, String link, String type) {
        if (recipient == null) return;
        Notification n = new Notification();
        n.setUser(recipient);
        n.setMessage(message);
        n.setLink(link);
        n.setType(type);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        repo.save(n);
    }

    public List<Notification> list(User user) {
        return repo.findByUserOrderByCreatedAtDesc(user);
    }

    public long unreadCount(User user) {
        return repo.countByUserAndReadFalse(user);
    }

    @Transactional
    public void markAllRead(User user) {
        repo.markAllRead(user);
    }
}
