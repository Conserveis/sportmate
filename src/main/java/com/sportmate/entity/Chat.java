package com.sportmate.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Chat")
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ChatID")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PostID", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;

    /** null = แชทรวมของโพสต์, มีค่า = แชทเฉพาะทีมนั้น (ทัวร์นาเมนต์แบบทีม) */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "TeamID")
    private Team team;

    @Column(name = "State", nullable = false)
    private String state = "active";   // active / ended

    @Column(name = "Text", nullable = false)
    private String text;

    @Column(name = "Time", nullable = false)
    private LocalDateTime time = LocalDateTime.now();

    // ---- getters / setters ----
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }
}
