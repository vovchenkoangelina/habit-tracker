package com.example.habitbot.scheduler;

import com.example.habitbot.model.Habit;
import com.example.habitbot.service.HabitService;
import com.example.habitbot.bot.HabitBot;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReminderScheduler {

    private final HabitService habitService;
    private final HabitBot habitBot;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public ReminderScheduler(HabitService habitService, HabitBot habitBot) {
        this.habitService = habitService;
        this.habitBot = habitBot;
    }

    @Scheduled(cron = "0 * * * * ?")
    public void sendReminders() {
        List<Habit> habits = habitService.findAllWithRemindersEnabled();
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        String nowStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        for (Habit habit : habits) {
            if (!habit.isDoneToday() && habit.isRemindersEnabled()) {
                String reminderTimeToCheck = habit.getReminderTime();
                if ("random".equals(reminderTimeToCheck)) {
                    reminderTimeToCheck = habit.getReminderTimeActual();
                }

                if (nowStr.equals(reminderTimeToCheck)) {
                    habitBot.sendReminder(habit.getChatId(), habit.getTitle());
                }
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void generateRandomTimes() {
        habitService.generateRandomReminderTimes();
    }

}
