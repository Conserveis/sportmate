package com.sportmate.repository;

import com.sportmate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUserName(String userName);
    Optional<User> findByGmail(String gmail);
    boolean existsByUserName(String userName);
    boolean existsByGmail(String gmail);

    // ผู้ใช้ที่กดสนใจ/ติดตามกีฬานี้ (ใช้ส่งแจ้งเตือนกิจกรรมใหม่ตามหมวดหมู่ UC-4 FR18)
    @org.springframework.data.jpa.repository.Query(
        "SELECT u FROM User u JOIN u.interestedSports s WHERE s.id = :sportId")
    java.util.List<User> findFollowersOfSport(@org.springframework.data.repository.query.Param("sportId") Integer sportId);
}
