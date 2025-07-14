package com.madcamp.moody.diary;

import com.madcamp.moody.user.User;
import com.madcamp.moody.mood.Mood;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    Diary findByUserAndMood(User user, Mood mood);
    // 또는 날짜로 찾고 싶으면
    // Diary findByUserAndDate(User user, LocalDate date);
}
