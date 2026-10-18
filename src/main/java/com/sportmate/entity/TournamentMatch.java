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
 * คู่การแข่งขัน 1 คู่ในสาย
 * round 1 = รอบแรก, slot = ลำดับคู่ในรอบ (เริ่ม 0)
 * ผู้ชนะของ (round, slot) ไปอยู่ที่ (round + 1, slot / 2) ฝั่ง A ถ้า slot เป็นเลขคู่ ฝั่ง B ถ้าเลขคี่
 */
@Entity
@Table(name = "TournamentMatch")
public class TournamentMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MatchID")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PostID", nullable = false)
    private Post post;

    @Column(name = "Round", nullable = false)
    private Integer round;

    @Column(name = "Slot", nullable = false)
    private Integer slot;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "TeamAID")
    private Team teamA;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "TeamBID")
    private Team teamB;

    @Column(name = "ScoreA")
    private Integer scoreA;

    @Column(name = "ScoreB")
    private Integer scoreB;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "WinnerTeamID")
    private Team winner;

    @Column(name = "Status", nullable = false)
    private String status = "waiting";   // waiting / ready / done / bye

    @Column(name = "PlayedAt")
    private LocalDateTime playedAt;

    public boolean isDone() { return "done".equals(status); }
    public boolean isBye() { return "bye".equals(status); }
    public boolean isReady() { return "ready".equals(status); }
    public boolean isWaiting() { return "waiting".equals(status); }

    public boolean isWinnerA() { return winner != null && teamA != null && winner.getId().equals(teamA.getId()); }
    public boolean isWinnerB() { return winner != null && teamB != null && winner.getId().equals(teamB.getId()); }

    // ---- getters / setters ----
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public Integer getRound() { return round; }
    public void setRound(Integer round) { this.round = round; }
    public Integer getSlot() { return slot; }
    public void setSlot(Integer slot) { this.slot = slot; }
    public Team getTeamA() { return teamA; }
    public void setTeamA(Team teamA) { this.teamA = teamA; }
    public Team getTeamB() { return teamB; }
    public void setTeamB(Team teamB) { this.teamB = teamB; }
    public Integer getScoreA() { return scoreA; }
    public void setScoreA(Integer scoreA) { this.scoreA = scoreA; }
    public Integer getScoreB() { return scoreB; }
    public void setScoreB(Integer scoreB) { this.scoreB = scoreB; }
    public Team getWinner() { return winner; }
    public void setWinner(Team winner) { this.winner = winner; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getPlayedAt() { return playedAt; }
    public void setPlayedAt(LocalDateTime playedAt) { this.playedAt = playedAt; }
}
