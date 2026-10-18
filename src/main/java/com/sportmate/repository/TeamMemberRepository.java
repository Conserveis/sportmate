package com.sportmate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sportmate.entity.Post;
import com.sportmate.entity.Team;
import com.sportmate.entity.TeamMember;
import com.sportmate.entity.User;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Integer> {

    Optional<TeamMember> findByTeamAndUser(Team team, User user);

    long countByTeamAndStatus(Team team, String status);

    /** สมาชิกที่อนุมัติแล้ว — หัวหน้าทีมขึ้นก่อน */
    @Query("""
        SELECT m FROM TeamMember m
        WHERE m.team = :team AND m.status = 'approved'
        ORDER BY CASE WHEN m.user = m.team.captain THEN 0 ELSE 1 END, m.joinDate ASC
    """)
    List<TeamMember> findApproved(@Param("team") Team team);

    List<TeamMember> findByTeamAndStatusOrderByJoinDateAsc(Team team, String status);

    /** แถวที่ยัง "ใช้งานอยู่" ของผู้ใช้ในทัวร์นาเมนต์นี้ (ขอเข้าทีม/อยู่ในทีม ของทีมที่ยังไม่ถูกปฏิเสธ/ยกเลิก) */
    @Query("""
        SELECT m FROM TeamMember m
        WHERE m.user = :user AND m.team.post = :post
          AND m.status IN ('pending','approved')
          AND m.team.status IN ('pending','approved')
    """)
    List<TeamMember> findActiveInPost(@Param("user") User user, @Param("post") Post post);

    /** จำนวนสมาชิกที่อนุมัติแล้วของทุกทีมในโพสต์ (TeamID, count) */
    @Query("""
        SELECT m.team.id, COUNT(m) FROM TeamMember m
        WHERE m.team.post = :post AND m.status = 'approved'
        GROUP BY m.team.id
    """)
    List<Object[]> countApprovedByTeam(@Param("post") Post post);
}
