package com.example.habitbot.repository;

import com.example.habitbot.model.Habit;

import java.util.List;
import java.util.Optional;

public interface HabitRepository {

    Habit save(Habit habit);
    Optional<Habit> findById(Long id);
    List<Habit> findAll();
    void deleteById(Long id);
}
