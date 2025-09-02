package com.example.habitbot.service;

import com.example.habitbot.model.Habit;
import com.example.habitbot.repository.HabitRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HabitService {

    private final HabitRepository habitRepository;

    public HabitService(HabitRepository habitRepository) {
        this.habitRepository = habitRepository;
    }

    public Habit addHabit(String title) {
        Habit habit = new Habit(null, title);
        return habitRepository.save(habit);
    }

    public List<Habit> listHabits() {
        return habitRepository.findAll();
    }

    public boolean markHabitDone(Long id) {
        Optional<Habit> optionalHabit = habitRepository.findById(id);
        if (optionalHabit.isPresent()) {
            Habit habit = optionalHabit.get();
            habit.markDone();
            habitRepository.save(habit);
            return true;
        }
        return false;
    }

    public void resetDailyHabits() {
        habitRepository.findAll().forEach(Habit::resetDay);
    }
}

