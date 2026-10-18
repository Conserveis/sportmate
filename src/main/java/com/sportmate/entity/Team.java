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

/**
 * ทีมในทัวร์นาเมนต์
 * - หัวหน้าทีม (captain) = คนที่กดขอสร้างทีม
 * - เจ้าของทัวร์นาเมนต์เป็นคนอนุมัติทีม (pending -> approved)
 * - กีฬาเดี่ยว: ระบบสร้างทีม 1 คนให้อัตโนมัติตอนจับสาย เพื่อให้สายใช้ตารางเดียวกัน
 */
@Entity
@Table(name = "Team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TeamID")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PostID", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "CaptainUserID", nullable = false)
    private User captain;

    @Column(name = "TeamName", nullable = false, length = 40)
    private String name;

    @Column(name = "Note", length = 300)
    private String note;

    @Column(name = "Status", nullable = false)
    private String status = "pending";   // pending / approved / rejected / cancelled

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** ตัวย่อชื่อทีม ใช้ทำไอคอน เช่น "ISE United" -> "IU" */
    public String getShortName() {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    public boolean isApproved() { return "approved".equals(status); }
    public boolean isPending() { return "pending".equals(status); }

    // ---- getters / setters ----
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public User getCaptain() { return captain; }
    public void setCaptain(User captain) { this.captain = captain; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
