package com.example.habitbot.service;

import com.example.habitbot.model.Habit;
import com.example.habitbot.repository.HabitRepository;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class HabitService {

    private final HabitRepository habitRepository;

    public HabitService(HabitRepository habitRepository) {
        this.habitRepository = habitRepository;
    }

    public Habit addHabit(String title, Long chatId, boolean remindersEnabled, String reminderTime) {
        Habit habit = new Habit();
        habit.setTitle(title);
        habit.setChatId(chatId);
        habit.setRemindersEnabled(remindersEnabled);
        if ("random".equals(reminderTime)) {
            habit.setReminderTime("random");
            LocalTime start = LocalTime.of(9, 0);
            LocalTime end = LocalTime.of(23, 0);
            int min = start.toSecondOfDay();
            int max = end.toSecondOfDay();
            int randomSec = min + (int) (Math.random() * (max - min));
            LocalTime randomTime = LocalTime.ofSecondOfDay(randomSec);
            habit.setReminderTimeActual(randomTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        } else {
            habit.setReminderTime(reminderTime);
            habit.setReminderTimeActual(reminderTime);
        }
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

    public Habit incrementHabit(String title, Long chatId) {
        List<Habit> habits = habitRepository.findByChatId(chatId);
        for (Habit habit : habits) {
            if (habit.getTitle().equalsIgnoreCase(title)) {
                habit.markDone();
                return habitRepository.save(habit);
            }
        }
        throw new IllegalArgumentException("Привычка с названием \"" + title + "\" не найдена");
    }

    public void generateRandomReminderTimes() {
        List<Habit> habits = findAllWithRemindersEnabled();
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(23, 0);

        for (Habit habit : habits) {
            if ("random".equals(habit.getReminderTime())) {
                int min = start.toSecondOfDay();
                int max = end.toSecondOfDay();
                int randomSec = min + (int) (Math.random() * (max - min));
                LocalTime randomTime = LocalTime.ofSecondOfDay(randomSec);
                habit.setReminderTimeActual(randomTime.format(DateTimeFormatter.ofPattern("HH:mm")));
                habitRepository.save(habit);
            }
        }
    }

    public void resetDailyHabits() {
        List<Habit> habits = habitRepository.findAll();
        habits.forEach(Habit::resetDay);
        habitRepository.saveAll(habits);
    }

    public List<Habit> findAllWithRemindersEnabled() {
        return habitRepository.findByRemindersEnabledTrue();
    }
}

