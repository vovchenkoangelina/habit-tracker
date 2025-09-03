package com.example.habitbot.scheduler;

import com.example.habitbot.service.HabitService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyResetScheduler {

    private final HabitService habitService;

    public DailyResetScheduler(HabitService habitService) {
        this.habitService = habitService;
    }

    @Scheduled(cron = "0 0 0 * * ?") // в 00:00:00 каждый день
    public void resetDailyHabits() {
        habitService.resetDailyHabits();
    }
}
