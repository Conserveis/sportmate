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


     //คอมเมนต์ในโพสต์
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
        /** แก้ไขคอมเมนต์ — เจ้าของคอมเมนต์เท่านั้น */
    @Transactional
    public void editComment(User user, Integer commentId, Integer postId, String text) {
        Chat c = chatRepo.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบความคิดเห็นนี้"));
        if (!c.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("ความคิดเห็นไม่ตรงกับโพสต์");
        }
        if (!c.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("แก้ไขได้เฉพาะความคิดเห็นของตัวเองเท่านั้น");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("ข้อความว่างเปล่า");
        }
        if (text.trim().length() > 1000) {
            throw new IllegalArgumentException("ความคิดเห็นยาวเกิน 1000 ตัวอักษร");
        }
        c.setText(text.trim());
        chatRepo.save(c);
    }

    /** ลบคอมเมนต์ — เจ้าของคอมเมนต์ หรือเจ้าของโพสต์ (ลบข้อความไม่เหมาะสมได้) */
    @Transactional
    public void deleteComment(User user, Integer commentId, Integer postId) {
        Chat c = chatRepo.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบความคิดเห็นนี้"));
        if (!c.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("ความคิดเห็นไม่ตรงกับโพสต์");
        }
        boolean isAuthor = c.getUser().getId().equals(user.getId());
        boolean isPostOwner = c.getPost().getOwner().getId().equals(user.getId());
        if (!isAuthor && !isPostOwner) {
            throw new IllegalStateException("ไม่มีสิทธิ์ลบความคิดเห็นนี้");
        }
        chatRepo.delete(c);
    }
}
