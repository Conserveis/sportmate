package com.sportmate.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sportmate.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUserName(String userName);
    Optional<User> findByGmail(String gmail);
    boolean existsByUserName(String userName);
    boolean existsByGmail(String gmail);
    Optional<User> findByAuthProviderAndProviderId(String authProvider, String providerId); //authenเพิ่ม

    // ผู้ใช้ที่กดสนใจ/ติดตามกีฬานี้ (ส่งแจ้งเตือนกิจกรรมใหม่)
    @org.springframework.data.jpa.repository.Query(
        "SELECT u FROM User u JOIN u.interestedSports s WHERE s.id = :sportId")
    java.util.List<User> findFollowersOfSport(@org.springframework.data.repository.query.Param("sportId") Integer sportId);
}
