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

        /** ดูความคิดเห็นได้เฉพาะเจ้าของโพสต์ และผู้เข้าร่วมที่ได้รับอนุมัติแล้วเท่านั้น */
    public boolean canAccess(User user, Post post) {
        if (user == null || post == null) return false;
        if (post.getOwner().getId().equals(user.getId())) return true;
        return eventService.hasJoined(user, post);   // approved เท่านั้น (pending = false)
    }

    /** ส่งความคิดเห็นได้ก็ต่อเมื่อเห็นความคิดเห็นได้ + กิจกรรมยังไม่ถูกยกเลิกและยังไม่หมดเวลา */
    public boolean canComment(User user, Post post) {
        return canAccess(user, post)
                && !"cancelled".equals(post.getStatus())
                && !post.isExpired();
    }

    public List<Chat> comments(User user, Post post) {
        if (!canAccess(user, post)) return List.of();
        return chatRepo.findByPostOrderByTimeAsc(post);
    }


     //คอมเมนต์ในโพสต์
    @Transactional
    public Chat addComment(User user, Post post, String text) {
        if (!canAccess(user, post)) {
            throw new IllegalStateException("ต้องเข้าร่วมกิจกรรมและได้รับการอนุมัติก่อนจึงจะคอมเมนต์ได้");
        }
        if ("cancelled".equals(post.getStatus())) {
            throw new IllegalStateException("กิจกรรมนี้ถูกยกเลิกแล้ว ไม่สามารถแสดงความคิดเห็นได้");
        }
        if (post.isExpired()) {
            throw new IllegalStateException("กิจกรรมนี้จบไปแล้ว ไม่สามารถแสดงความคิดเห็นได้");
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
        if (c.getPost().isExpired() || "cancelled".equals(c.getPost().getStatus())) {
            throw new IllegalStateException("กิจกรรมนี้จบแล้ว ไม่สามารถแก้ไขความคิดเห็นได้");
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
