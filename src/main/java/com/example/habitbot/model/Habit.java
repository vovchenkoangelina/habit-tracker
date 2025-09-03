package com.example.habitbot.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "habits")
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private LocalDateTime startDate;
    private boolean isDoneToday;
    private int completionCount;

    private Long chatId;

    private boolean remindersEnabled;
    private String reminderTime;
    private String reminderTimeActual;

    private int currentStreak;
    private int maxStreak;
    private LocalDate lastCompletionDate;

    public Habit() {
        this.startDate = LocalDateTime.now();
        this.isDoneToday = false;
        this.completionCount = 0;
    }


    public void markDone() {
        LocalDate today = LocalDate.now();
        if (!isDoneToday) {
            if (lastCompletionDate != null && lastCompletionDate.equals(today.minusDays(1))) {
                currentStreak++;
            } else {
                currentStreak = 1;
            }
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak;
            }
            lastCompletionDate = today;
            isDoneToday = true;
            completionCount++;
        }
    }

    public void resetDay() {
        isDoneToday = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public boolean isDoneToday() {
        return isDoneToday;
    }

    public void setDoneToday(boolean doneToday) {
        isDoneToday = doneToday;
    }

    public int getCompletionCount() {
        return completionCount;
    }

    public void setCompletionCount(int completionCount) {
        this.completionCount = completionCount;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public boolean isRemindersEnabled() {
        return remindersEnabled;
    }

    public void setRemindersEnabled(boolean remindersEnabled) {
        this.remindersEnabled = remindersEnabled;
    }

    public String getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(String reminderTime) {
        this.reminderTime = reminderTime;
    }

    public String getReminderTimeActual() {
        return reminderTimeActual;
    }

    public void setReminderTimeActual(String reminderTimeActual) {
        this.reminderTimeActual = reminderTimeActual;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getMaxStreak() {
        return maxStreak;
    }

    public void setMaxStreak(int maxStreak) {
        this.maxStreak = maxStreak;
    }

    public LocalDate getLastCompletionDate() {
        return lastCompletionDate;
    }

    public void setLastCompletionDate(LocalDate lastCompletionDate) {
        this.lastCompletionDate = lastCompletionDate;
    }
}


