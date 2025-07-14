package com.madcamp.moody.mood;

import java.time.LocalDate;
import java.util.List;

import com.madcamp.moody.user.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MoodRepository extends JpaRepository<Mood, Long> {
    Mood findByUserAndDate(User user, LocalDate date);
    List<Mood> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
}
