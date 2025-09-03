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

    public Habit addHabit(String title, Long chatId) {
        Habit habit = new Habit();
        habit.setTitle(title);
        habit.setChatId(chatId);
        return habitRepository.save(habit);
    }

    public List<Habit> listHabits(Long chatId) {
        return habitRepository.findByChatId(chatId);
    }

    public Habit markHabitDone(Long id, Long chatId) {
        Optional<Habit> optionalHabit = habitRepository.findById(id);
        if (optionalHabit.isPresent()) {
            Habit habit = optionalHabit.get();
            if (!habit.getChatId().equals(chatId)) {
                throw new IllegalArgumentException("Эта привычка не принадлежит вам.");
            }
            habit.markDone();
            return habitRepository.save(habit);
        }
        throw new IllegalArgumentException("Привычка с id " + id + " не найдена");
    }

    public void deleteHabit(Long id, Long chatId) {
        Optional<Habit> optionalHabit = habitRepository.findById(id);
        if (optionalHabit.isPresent()) {
            Habit habit = optionalHabit.get();
            if (!habit.getChatId().equals(chatId)) {
                throw new IllegalArgumentException("Эта привычка не принадлежит вам.");
            }
            habitRepository.delete(habit);
        } else {
            throw new IllegalArgumentException("Привычка с id " + id + " не найдена");
        }
    }

    public void resetDailyHabits() {
        List<Habit> habits = habitRepository.findAll();
        habits.forEach(Habit::resetDay);
        habitRepository.saveAll(habits);
    }
}

