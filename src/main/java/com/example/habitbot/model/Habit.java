package com.example.habitbot.model;

import jakarta.persistence.*;
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

    public Habit() {
        this.startDate = LocalDateTime.now();
        this.isDoneToday = false;
        this.completionCount = 0;
    }


    public void markDone() {
        if (!isDoneToday) {
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
}


