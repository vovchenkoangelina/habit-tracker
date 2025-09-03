package com.example.habitbot.repository;

import com.example.habitbot.model.Habit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    List<Habit> findByChatId(Long chatId);
    List<Habit> findByRemindersEnabledTrue();

}
