package com.sportmate.dto;

import java.math.BigDecimal;
import java.util.List;

import com.sportmate.entity.Event;
import com.sportmate.entity.Post;
import com.sportmate.entity.Review;
import com.sportmate.entity.User;

public class PublicProfile {

    private final User user;

    // ---- ฝั่งผู้จัดกิจกรรม (organizer) ----
    private final long organizedCount;          // จำนวนครั้งการจัดกิจกรรมทั้งหมด (ไม่นับที่ยกเลิก)
    private final long organizedFinishedCount;  // จัดจนจบแล้วกี่ครั้ง
    private final long organizedCancelledCount; // ยกเลิกไปกี่ครั้ง
    private final long reviewCount;             // จำนวนรีวิวที่ได้รับ
    private final BigDecimal avgScore;          // คะแนนเฉลี่ย (จาก User.AvgScore ที่ trigger อัปเดตให้)
    private final List<Post> organizedPosts;    // รายการกิจกรรมที่เคยจัด
    private final List<Review> reviews;         // รีวิวที่ได้รับ (ใหม่ก่อน)

    // ---- ฝั่งผู้เข้าร่วม (participant) ----
    private final long joinCount;               // จำนวนครั้งการเข้าร่วมทั้งหมด
    private final long attendedCount;           // เข้าร่วมและกิจกรรมจบไปแล้วกี่ครั้ง
    private final long upcomingCount;           // ที่กำลังจะถึง
    private final List<SportCount> sportCounts; // สัดส่วนกีฬาที่เข้าร่วม (มาก -> น้อย)
    private final List<Event> recentJoined;     // กิจกรรมที่เข้าร่วมล่าสุด

    public PublicProfile(User user,
                         long organizedCount, long organizedFinishedCount, long organizedCancelledCount,
                         long reviewCount, BigDecimal avgScore,
                         List<Post> organizedPosts, List<Review> reviews,
                         long joinCount, long attendedCount, long upcomingCount,
                         List<SportCount> sportCounts, List<Event> recentJoined) {
        this.user = user;
        this.organizedCount = organizedCount;
        this.organizedFinishedCount = organizedFinishedCount;
        this.organizedCancelledCount = organizedCancelledCount;
        this.reviewCount = reviewCount;
        this.avgScore = avgScore;
        this.organizedPosts = organizedPosts;
        this.reviews = reviews;
        this.joinCount = joinCount;
        this.attendedCount = attendedCount;
        this.upcomingCount = upcomingCount;
        this.sportCounts = sportCounts;
        this.recentJoined = recentJoined;
    }

    /** กีฬาที่เข้าร่วมมากที่สุด — null ถ้ายังไม่เคยเข้าร่วมเลย */
    public SportCount getTopSport() {
        return sportCounts.isEmpty() ? null : sportCounts.get(0);
    }

    /** เคยจัดกิจกรรมหรือไม่ (ใช้ตัดสินว่าจะโชว์การ์ดฝั่งผู้จัดไหม) */
    public boolean isOrganizer() { return organizedCount > 0; }

    public User getUser() { return user; }
    public long getOrganizedCount() { return organizedCount; }
    public long getOrganizedFinishedCount() { return organizedFinishedCount; }
    public long getOrganizedCancelledCount() { return organizedCancelledCount; }
    public long getReviewCount() { return reviewCount; }
    public BigDecimal getAvgScore() { return avgScore; }
    public List<Post> getOrganizedPosts() { return organizedPosts; }
    public List<Review> getReviews() { return reviews; }
    public long getJoinCount() { return joinCount; }
    public long getAttendedCount() { return attendedCount; }
    public long getUpcomingCount() { return upcomingCount; }
    public List<SportCount> getSportCounts() { return sportCounts; }
    public List<Event> getRecentJoined() { return recentJoined; }
}
