package com.sportmate.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Event")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EventID")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;   // ผู้เข้าร่วม

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PostID", nullable = false)
    private Post post;

    @Column(name = "EventName")
    private String eventName;

    @Column(name = "Status", nullable = false)
    private String status = "pending";   // pending / approved / rejected / cancelled

    @Column(name = "JoinDate", nullable = false)
    private LocalDateTime joinDate = LocalDateTime.now();

    @Column(name = "CancelDate")
    private LocalDateTime cancelDate;

    // ---- getters / setters ----
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDateTime joinDate) { this.joinDate = joinDate; }
    public LocalDateTime getCancelDate() { return cancelDate; }
    public void setCancelDate(LocalDateTime cancelDate) { this.cancelDate = cancelDate; }
}
