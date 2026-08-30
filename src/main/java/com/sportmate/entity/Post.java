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
import jakarta.persistence.Transient;

@Entity
@Table(name = "Post")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PostID")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "OwnerUserID", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PostTypeID", nullable = false)
    private PostType postType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "SportID", nullable = false)
    private Sport sport;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "LocationID", nullable = false)
    private Location location;

    @Column(name = "PostName", nullable = false)
    private String postName;

    @Column(name = "Description")
    private String description;

    @Column(name = "DatePlay", nullable = false)
    private LocalDateTime datePlay;

    @Column(name = "DateCreate", nullable = false)
    private LocalDateTime dateCreate = LocalDateTime.now();

    @Column(name = "PublishAt")
    private LocalDateTime publishAt;   // null = เผยแพร่ทันที

    @Column(name = "MaxPlayer", nullable = false)
    private Integer maxPlayer;

    @Column(name = "MinPlayer", nullable = false)
    private Integer minPlayer;

    @Column(name = "IsPublic", nullable = false)
    private boolean isPublic = true;

    @Column(name = "Status", nullable = false)
    private String status = "open";   // open / closed / cancelled / finished

    @Transient
    public boolean isExpired() {
        return datePlay != null && datePlay.isBefore(LocalDateTime.now());
    }

    /** เส้นตายในการยกเลิก = ก่อนวันจัดกิจกรรม 1 วัน */
    @Transient
    public LocalDateTime getCancelDeadline() {
        return datePlay == null ? null : datePlay.minusDays(1);
    }

    /** true = เลยเส้นตายแล้ว ยกเลิกไม่ได้ (เหลือน้อยกว่า 1 วันก่อนวันจัด) */
    @Transient
    public boolean isCancelLocked() {
        return datePlay != null && LocalDateTime.now().isAfter(datePlay.minusDays(1));
    }


    @Transient
    public boolean isTournament() {
        return postType != null && PostType.TOURNAMENT.equals(postType.getName());
    }

    // ---- getters / setters ----
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public PostType getPostType() { return postType; }
    public void setPostType(PostType postType) { this.postType = postType; }
    public Sport getSport() { return sport; }
    public void setSport(Sport sport) { this.sport = sport; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public String getPostName() { return postName; }
    public void setPostName(String postName) { this.postName = postName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDatePlay() { return datePlay; }
    public void setDatePlay(LocalDateTime datePlay) { this.datePlay = datePlay; }
    public LocalDateTime getDateCreate() { return dateCreate; }
    public void setDateCreate(LocalDateTime dateCreate) { this.dateCreate = dateCreate; }
    public LocalDateTime getPublishAt() { return publishAt; }
    public void setPublishAt(LocalDateTime publishAt) { this.publishAt = publishAt; }
    public Integer getMaxPlayer() { return maxPlayer; }
    public void setMaxPlayer(Integer maxPlayer) { this.maxPlayer = maxPlayer; }
    public Integer getMinPlayer() { return minPlayer; }
    public void setMinPlayer(Integer minPlayer) { this.minPlayer = minPlayer; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
