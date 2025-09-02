package com.example.habitbot.repository;

import com.example.habitbot.model.Habit;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryHabitRepository implements HabitRepository {

    private final Map<Long, Habit> habits = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public Habit save(Habit habit) {
        if (habit.getId() == null) {
            habit = new Habit(idGenerator.incrementAndGet(), habit.getTitle());
        }
        habits.put(habit.getId(), habit);
        return habit;
    }

    @Override
    public Optional<Habit> findById(Long id) {
        return Optional.ofNullable(habits.get(id));
    }

    @Override
    public List<Habit> findAll() {
        return new ArrayList<>(habits.values());
    }

    @Override
    public void deleteById(Long id) {
        habits.remove(id);
    }
}
