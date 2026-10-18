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
 * สมาชิกทีม — หัวหน้าทีมก็มีแถวของตัวเอง (approved ตั้งแต่ขอสร้างทีม)
 * จำนวนสมาชิกที่แสดง = จำนวนแถว approved จึงนับรวมหัวหน้าทีมแล้ว
 */
@Entity
@Table(name = "TeamMember")
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TeamMemberID")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "TeamID", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;

    @Column(name = "Message", length = 300)
    private String message;

    @Column(name = "Status", nullable = false)
    private String status = "pending";   // pending / approved / rejected / cancelled

    @Column(name = "JoinDate", nullable = false)
    private LocalDateTime joinDate = LocalDateTime.now();

    @Column(name = "CancelDate")
    private LocalDateTime cancelDate;

    public boolean isCaptain() {
        return team != null && team.getCaptain() != null && user != null
                && team.getCaptain().getId().equals(user.getId());
    }

    // ---- getters / setters ----
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDateTime joinDate) { this.joinDate = joinDate; }
    public LocalDateTime getCancelDate() { return cancelDate; }
    public void setCancelDate(LocalDateTime cancelDate) { this.cancelDate = cancelDate; }
}
