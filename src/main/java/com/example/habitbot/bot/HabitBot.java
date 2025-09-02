package com.example.habitbot.bot;

import com.example.habitbot.model.Habit;
import com.example.habitbot.service.HabitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class HabitBot extends TelegramLongPollingBot {

    private final HabitService habitService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    public HabitBot(HabitService habitService) {
        this.habitService = habitService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            String text = message.getText();
            long chatId = message.getChatId();

            String response;

            if (text.startsWith("/addhabit")) {
                String title = text.replace("/addhabit", "").trim();
                if (!title.isEmpty()) {
                    Habit habit = habitService.addHabit(title);
                    response = "Привычка добавлена: " + habit.getTitle();
                } else {
                    response = "Пожалуйста, укажите название привычки после команды.";
                }
            } else if (text.equals("/listhabits")) {
                var habits = habitService.listHabits();
                if (habits.isEmpty()) {
                    response = "Список привычек пуст.";
                } else {
                    StringBuilder sb = new StringBuilder("Ваши привычки:\n");
                    for (Habit h : habits) {
                        sb.append(h.getId()).append(". ").append(h.getTitle())
                                .append(" (выполнено раз: ").append(h.getCompletionCount()).append(")\n");
                    }
                    response = sb.toString();
                }
            } else {
                response = "Неизвестная команда. Доступные команды: /addhabit, /listhabits";
            }

            sendMessage(chatId, response);
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
