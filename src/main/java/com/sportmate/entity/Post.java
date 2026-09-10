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

    /** จำนวนครั้งสูงสุดที่แก้ไขโพสต์ได้ */
    public static final int MAX_EDIT = 3;
    /** เลื่อนวันเวลานัดออกไปได้สูงสุดกี่ชั่วโมง (นับจากกำหนดเดิมตอนสร้าง) */
    public static final int MAX_POSTPONE_HOURS = 72;

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

    /** วันเวลานัดเดิมตอนสร้างโพสต์ — ฐานคำนวณเพดานเลื่อนเวลา 72 ชม. */
    @Column(name = "OriginalDatePlay")
    private LocalDateTime originalDatePlay;

    /** จำนวนครั้งที่แก้ไขไปแล้ว */
    @Column(name = "EditCount", nullable = false)
    private Integer editCount = 0;

    @Column(name = "DateCreate", nullable = false)
    private LocalDateTime dateCreate = LocalDateTime.now();

    @Column(name = "PublishAt")
    private LocalDateTime publishAt;   // null = เผยแพร่ทันที

    /** เวลาที่ยิงแจ้งเตือนผู้ติดตามกีฬาไปแล้ว — null = ยังไม่ได้แจ้ง (กันแจ้งซ้ำ) */
    @Column(name = "NotifiedAt")
    private LocalDateTime notifiedAt;

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

    /** เส้นตายยกเลิก "การเข้าร่วม" ของผู้เล่น = ก่อนวันนัดปัจจุบัน 1 วัน */
    @Transient
    public LocalDateTime getCancelDeadline() {
        return datePlay == null ? null : datePlay.minusDays(1);
    }

    @Transient
    public boolean isCancelLocked() {
        return datePlay != null && LocalDateTime.now().isAfter(datePlay.minusDays(1));
    }

    /** กำหนดเดิมตอนสร้างโพสต์ — โพสต์เก่าที่ยังไม่มีค่า ถือว่าค่าปัจจุบันคือกำหนดเดิม */
    @Transient
    public LocalDateTime getBaseDatePlay() {
        return originalDatePlay != null ? originalDatePlay : datePlay;
    }

    /**
     * เส้นตายยกเลิก "กิจกรรม" ของเจ้าของ = ก่อน "กำหนดเดิม" 1 วัน
     * ผูกกับกำหนดเดิมเพื่อกันเจ้าของเลื่อนวันเพื่อรีเซ็ตเส้นตายยกเลิก
     */
    @Transient
    public LocalDateTime getOwnerCancelDeadline() {
        LocalDateTime base = getBaseDatePlay();
        return base == null ? null : base.minusDays(1);
    }

    @Transient
    public boolean isOwnerCancelLocked() {
        LocalDateTime base = getBaseDatePlay();
        return base != null && LocalDateTime.now().isAfter(base.minusDays(1));
    }

    /** เลื่อนวันเวลานัดได้ช้าสุดถึงเมื่อไหร่ (กำหนดเดิม + 72 ชม.) */
    @Transient
    public LocalDateTime getPostponeDeadline() {
        LocalDateTime base = getBaseDatePlay();
        return base == null ? null : base.plusHours(MAX_POSTPONE_HOURS);
    }

    @Transient
    public int getEditsLeft() {
        return Math.max(0, MAX_EDIT - (editCount == null ? 0 : editCount));
    }

    @Transient
    public boolean isEditLocked() {
        return getEditsLeft() <= 0;
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
    public LocalDateTime getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(LocalDateTime notifiedAt) { this.notifiedAt = notifiedAt; }
    public Integer getMaxPlayer() { return maxPlayer; }
    public void setMaxPlayer(Integer maxPlayer) { this.maxPlayer = maxPlayer; }
    public Integer getMinPlayer() { return minPlayer; }
    public void setMinPlayer(Integer minPlayer) { this.minPlayer = minPlayer; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getOriginalDatePlay() { return originalDatePlay; }
    public void setOriginalDatePlay(LocalDateTime originalDatePlay) { this.originalDatePlay = originalDatePlay; }
    public Integer getEditCount() { return editCount; }
    public void setEditCount(Integer editCount) { this.editCount = editCount; }
}
