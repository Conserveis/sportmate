package com.sportmate.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sportmate.entity.Post;
import com.sportmate.entity.PostReminder;

public interface PostReminderRepository extends JpaRepository<PostReminder, Integer> {
    List<PostReminder> findByPost(Post post);
}