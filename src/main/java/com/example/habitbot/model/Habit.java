package com.example.habitbot.model;

import java.time.LocalDateTime;

public class Habit {

    private Long id;
    private String title;
    private LocalDateTime startDate;
    private boolean isDoneToday;
    private int completionCount;

    public Habit(Long id, String title) {
        this.id = id;
        this.title = title;
        this.startDate = LocalDateTime.now();
        this.isDoneToday = false;
        this.completionCount = 0;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public LocalDateTime getStartDate() { return startDate; }
    public boolean isDoneToday() { return isDoneToday; }
    public int getCompletionCount() { return completionCount; }

    public void markDone() {
        if (!isDoneToday) {
            isDoneToday = true;
            completionCount++;
        }
    }

    public void resetDay() {
        isDoneToday = false;
    }
}


