package com.sportmate.repository;

import com.sportmate.entity.Chat;
import com.sportmate.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Integer> {
    List<Chat> findByPostOrderByTimeAsc(Post post);

    /** แชทรวมของโพสต์ (ไม่รวมแชททีม) */
    List<Chat> findByPostAndTeamIsNullOrderByTimeAsc(Post post);

    /** แชทของทีมหนึ่ง */
    List<Chat> findByTeamOrderByTimeAsc(com.sportmate.entity.Team team);
}
