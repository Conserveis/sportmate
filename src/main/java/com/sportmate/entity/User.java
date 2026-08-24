package com.sportmate.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "User")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Integer id;

    @Column(name = "UserName", nullable = false, unique = true)
    private String userName;

    @Column(name = "Password", nullable = false)
    private String password;   // BCrypt hash

    @Column(name = "PhoneNumber")
    private String phoneNumber;

    @Column(name = "Gmail", nullable = false, unique = true)
    private String gmail;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "UserTypeID", nullable = false)
    private UserType userType;

    @Column(name = "AvgScore", nullable = false)
    private BigDecimal avgScore = BigDecimal.ZERO;

    @Column(name = "IsEmailVerified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "OtpCode")
    private String otpCode;

    @Column(name = "OtpExpireAt")
    private LocalDateTime otpExpireAt;

    @Column(name = "FailedLoginCount", nullable = false)
    private int failedLoginCount = 0;

    @Column(name = "LockUntil")
    private LocalDateTime lockUntil;

    @Column(name = "LastActivityAt")
    private LocalDateTime lastActivityAt;

    @Column(name = "MembershipExpireAt")
    private LocalDateTime membershipExpireAt;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // กีฬาที่สนใจ (UserSport M:N)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "UserSport",
            joinColumns = @JoinColumn(name = "UserID"),
            inverseJoinColumns = @JoinColumn(name = "SportID"))
    private Set<Sport> interestedSports = new HashSet<>();

    @Transient
    public boolean isMember() {
        return userType != null && UserType.MEMBER.equals(userType.getName())
                && membershipExpireAt != null
                && membershipExpireAt.isAfter(LocalDateTime.now());
    }

    // ---- getters / setters ----
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getGmail() { return gmail; }
    public void setGmail(String gmail) { this.gmail = gmail; }
    public UserType getUserType() { return userType; }
    public void setUserType(UserType userType) { this.userType = userType; }
    public BigDecimal getAvgScore() { return avgScore; }
    public void setAvgScore(BigDecimal avgScore) { this.avgScore = avgScore; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
    public LocalDateTime getOtpExpireAt() { return otpExpireAt; }
    public void setOtpExpireAt(LocalDateTime otpExpireAt) { this.otpExpireAt = otpExpireAt; }
    public int getFailedLoginCount() { return failedLoginCount; }
    public void setFailedLoginCount(int failedLoginCount) { this.failedLoginCount = failedLoginCount; }
    public LocalDateTime getLockUntil() { return lockUntil; }
    public void setLockUntil(LocalDateTime lockUntil) { this.lockUntil = lockUntil; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public LocalDateTime getMembershipExpireAt() { return membershipExpireAt; }
    public void setMembershipExpireAt(LocalDateTime membershipExpireAt) { this.membershipExpireAt = membershipExpireAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Set<Sport> getInterestedSports() { return interestedSports; }
    public void setInterestedSports(Set<Sport> interestedSports) { this.interestedSports = interestedSports; }
}
