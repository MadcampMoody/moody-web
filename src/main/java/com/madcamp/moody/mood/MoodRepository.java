package com.madcamp.moody.mood;

import java.time.LocalDate;

import com.madcamp.moody.user.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MoodRepository extends JpaRepository<Mood, Long> {
    Mood findByUserAndDate(User user, LocalDate date);
}
