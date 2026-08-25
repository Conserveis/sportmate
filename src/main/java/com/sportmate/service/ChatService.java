package com.sportmate.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sportmate.entity.Chat;
import com.sportmate.entity.Post;
import com.sportmate.entity.User;
import com.sportmate.repository.ChatRepository;

@Service
public class ChatService {

    private final ChatRepository chatRepo;
    private final EventService eventService;

    public ChatService(ChatRepository chatRepo, EventService eventService) {
        this.chatRepo = chatRepo;
        this.eventService = eventService;
    }

    public List<Chat> comments(Post post) {
        return chatRepo.findByPostOrderByTimeAsc(post);
    }

    /**
     * คอมเมนต์ในโพสต์ (UC-... FR28): ทำได้เฉพาะผู้ที่เข้าร่วมแล้ว หรือเจ้าของโพสต์
     */
    @Transactional
    public Chat addComment(User user, Post post, String text) {
        boolean owner = post.getOwner().getId().equals(user.getId());
        if (!owner && !eventService.hasJoined(user, post)) {
            throw new IllegalStateException("ต้องเข้าร่วมกิจกรรมก่อนจึงจะคอมเมนต์ได้");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("ข้อความว่างเปล่า");
        }
        Chat c = new Chat();
        c.setPost(post);
        c.setUser(user);
        c.setText(text.trim());
        c.setState("active");
        c.setTime(LocalDateTime.now());
        return chatRepo.save(c);
    }
}
