package com.sportmate.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** บันทึกว่าโพสต์นี้ส่งแจ้งเตือนล่วงหน้ารอบไหนไปแล้ว (24 / 12 / 1 ชม.) */
@Entity
@Table(name = "PostReminder",
       uniqueConstraints = @UniqueConstraint(columnNames = {"PostID", "HoursBefore"}))
public class PostReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReminderID")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PostID", nullable = false)
    private Post post;

    @Column(name = "HoursBefore", nullable = false)
    private int hoursBefore;

    @Column(name = "SentAt", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public int getHoursBefore() { return hoursBefore; }
    public void setHoursBefore(int hoursBefore) { this.hoursBefore = hoursBefore; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}