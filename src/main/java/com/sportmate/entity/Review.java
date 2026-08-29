package com.sportmate.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "Review")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReviewID")
    private Integer id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "EventID", nullable = false, unique = true)
    private Event event;

    @Column(name = "ReviewScore", nullable = false)
    private Integer reviewScore;   // 1..5

    @Column(name = "Comment")
    private String comment;

    @Column(name = "ReviewDate", nullable = false)
    private LocalDateTime reviewDate = LocalDateTime.now();

    @Column(name = "Direction", nullable = false)
    private String direction = "to_owner";   // to_owner / to_participant

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public Integer getReviewScore() { return reviewScore; }
    public void setReviewScore(Integer reviewScore) { this.reviewScore = reviewScore; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDateTime reviewDate) { this.reviewDate = reviewDate; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
}
