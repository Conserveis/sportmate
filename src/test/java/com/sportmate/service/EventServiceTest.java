package com.sportmate.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sportmate.entity.Event;
import com.sportmate.entity.Post;
import com.sportmate.entity.User;
import com.sportmate.repository.EventRepository;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepo;

    @Mock
    private NotificationService notificationService;

    private EventService eventService;

    private User owner;
    private User applicant;
    private User stranger;
    private Post publicPost;
    private Post privatePost;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepo, notificationService);

        owner = new User();
        owner.setId(1);
        owner.setUserName("ownerUser");

        applicant = new User();
        applicant.setId(2);
        applicant.setUserName("applicantUser");

        stranger = new User();
        stranger.setId(3);
        stranger.setUserName("strangerUser");

        publicPost = new Post();
        publicPost.setId(101);
        publicPost.setOwner(owner);
        publicPost.setPostName("เตะบอล 5v5");
        publicPost.setPublic(true);
        publicPost.setMaxPlayer(10);
        publicPost.setMinPlayer(4);
        publicPost.setStatus("open");
        publicPost.setDatePlay(LocalDateTime.now().plusDays(2));

        privatePost = new Post();
        privatePost.setId(102);
        privatePost.setOwner(owner);
        privatePost.setPostName("แบดมินตันส่วนตัว");
        privatePost.setPublic(false);
        privatePost.setMaxPlayer(6);
        privatePost.setMinPlayer(2);
        privatePost.setStatus("open");
        privatePost.setDatePlay(LocalDateTime.now().plusDays(2));
    }

    @Test
    @DisplayName("โพสต์ส่วนตัว (ต้องอนุมัติ): เมื่อ user ขอเข้าร่วม สถานะต้องเป็น pending และยังไม่นับจำนวนเข้าร่วม")
    void testJoinPrivatePostPending() {
        when(eventRepo.countApprovedJoins(privatePost)).thenReturn(0L);
        when(eventRepo.findByUserAndPost(applicant, privatePost)).thenReturn(Optional.empty());

        eventService.join(applicant, privatePost);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepo).save(eventCaptor.capture());
        assertEquals("pending", eventCaptor.getValue().getStatus());

        // แจ้งเตือนเจ้าของว่าขอเข้าร่วม 
        verify(notificationService).push(eq(owner), contains("ขอเข้าร่วม"), contains("/posts/102"), eq("join_request"));
    }

    @Test
    @DisplayName("โพสต์สาธารณะ: เมื่อ user เข้าร่วม สถานะต้องเป็น approved ทันที")
    void testJoinPublicPostApproved() {
        when(eventRepo.countApprovedJoins(publicPost)).thenReturn(0L).thenReturn(1L);
        when(eventRepo.findByUserAndPost(applicant, publicPost)).thenReturn(Optional.empty());

        eventService.join(applicant, publicPost);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepo).save(eventCaptor.capture());
        assertEquals("approved", eventCaptor.getValue().getStatus());

        verify(notificationService).push(eq(owner), contains("เข้าร่วม"), contains("/posts/101"), eq("join"));
    }

    @Test
    @DisplayName("เฉพาะผู้สร้างโพสต์เท่านั้นที่สามารถอนุมัติได้ (คนอื่นกดอนุมัติต้อง Error)")
    void testApproveNonOwnerThrowsException() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                eventService.approve(stranger, privatePost, applicant.getId())
        );
        assertTrue(ex.getMessage().contains("เฉพาะผู้สร้างโพสต์"));
    }

    @Test
    @DisplayName("ผู้สร้างโพสต์อนุมัติคำขอ: เปลี่ยนสถานะเป็น approved และส่งแจ้งเตือนให้ผู้ขอเข้าร่วม")
    void testApproveByOwnerSuccess() {
        Event pendingEvent = new Event();
        pendingEvent.setUser(applicant);
        pendingEvent.setPost(privatePost);
        pendingEvent.setStatus("pending");

        when(eventRepo.findByUserAndPost(any(User.class), eq(privatePost))).thenReturn(Optional.of(pendingEvent));
        when(eventRepo.countApprovedJoins(privatePost)).thenReturn(0L);

        eventService.approve(owner, privatePost, applicant.getId());

        assertEquals("approved", pendingEvent.getStatus());
        verify(eventRepo).save(pendingEvent);
        verify(notificationService).push(eq(applicant), contains("อนุมัติให้คุณเข้าร่วม"), contains("/posts/102"), eq("join_approved"));
    }

    @Test
    @DisplayName("ผู้สร้างโพสต์ปฏิเสธคำขอ: เปลี่ยนสถานะเป็น rejected และส่งแจ้งเตือนให้ผู้ขอเข้าร่วม")
    void testRejectByOwnerSuccess() {
        Event pendingEvent = new Event();
        pendingEvent.setUser(applicant);
        pendingEvent.setPost(privatePost);
        pendingEvent.setStatus("pending");

        when(eventRepo.findByUserAndPost(any(User.class), eq(privatePost))).thenReturn(Optional.of(pendingEvent));

        eventService.reject(owner, privatePost, applicant.getId());

        assertEquals("rejected", pendingEvent.getStatus());
        assertNotNull(pendingEvent.getCancelDate());
        verify(eventRepo).save(pendingEvent);
        verify(notificationService).push(eq(applicant), contains("ไม่ได้รับการอนุมัติ"), contains("/posts/102"), eq("join_rejected"));
    }

    @Test
    @DisplayName("hasJoined ต้องเป็น true เฉพาะ approved เท่านั้น (pending ต้องเป็น false)")
    void testHasJoinedAndIsPending() {
        Event approvedEvent = new Event();
        approvedEvent.setStatus("approved");

        Event pendingEvent = new Event();
        pendingEvent.setStatus("pending");

        when(eventRepo.findByUserAndPost(applicant, privatePost)).thenReturn(Optional.of(pendingEvent));
        assertFalse(eventService.hasJoined(applicant, privatePost));
        assertTrue(eventService.isPending(applicant, privatePost));

        when(eventRepo.findByUserAndPost(applicant, privatePost)).thenReturn(Optional.of(approvedEvent));
        assertTrue(eventService.hasJoined(applicant, privatePost));
        assertFalse(eventService.isPending(applicant, privatePost));
    }
}
