package com.example.habitbot.bot;

import com.example.habitbot.model.Habit;
import com.example.habitbot.service.HabitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HabitBot extends TelegramLongPollingBot {

    private final HabitService habitService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    private final Map<Long, UserState> userStates = new HashMap<>();

    private enum UserState {
        WAITING_FOR_HABIT_NAME
    }

    public HabitBot(HabitService habitService) {
        this.habitService = habitService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallback(update);
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update.getMessage());
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(Message message) throws TelegramApiException {
        String text = message.getText();
        long chatId = message.getChatId();

        if (userStates.get(chatId) == UserState.WAITING_FOR_HABIT_NAME) {
            Habit habit = habitService.addHabit(text, chatId);
            sendMessage(chatId, "✅ Привычка добавлена: " + habit.getTitle());
            userStates.remove(chatId);
            return;
        }

        switch (text) {
            case "/start" -> {
                sendMessage(chatId, "👋 Привет! Я помогу тебе отслеживать привычки.");
                sendMenu(chatId);
            }
            case "/menu" -> sendMenu(chatId);
            case "/listhabits" -> sendHabitsList(chatId);
            default -> sendMessage(chatId, "Неизвестная команда. Используй /menu для выбора действий.");
        }
    }

    private void handleCallback(Update update) throws TelegramApiException {
        String data = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (data) {
            case "add" -> {
                sendMessage(chatId, "✏️ Введите название привычки:");
                userStates.put(chatId, UserState.WAITING_FOR_HABIT_NAME);
            }
            case "list" -> sendHabitsList(chatId);
            case "done_menu" -> sendDoneMenu(chatId);
            default -> {
                if (data.startsWith("done_")) {
                    Long habitId = Long.parseLong(data.replace("done_", ""));
                    Habit habit = habitService.markHabitDone(habitId, chatId);
                    sendMessage(chatId, "✅ Привычка отмечена: " + habit.getTitle() +
                            " (выполнено раз: " + habit.getCompletionCount() + ")");
                } else {
                    sendMessage(chatId, "Неизвестное действие");
                }
            }
        }
    }

    private void sendHabitsList(long chatId) throws TelegramApiException {
        var habits = habitService.listHabits(chatId);
        if (habits.isEmpty()) {
            sendMessage(chatId, "Список привычек пуст.");
        } else {
            StringBuilder sb = new StringBuilder("📋 Ваши привычки:\n");
            for (Habit h : habits) {
                sb.append(h.getId()).append(". ").append(h.getTitle())
                        .append(" (выполнено раз: ").append(h.getCompletionCount()).append(")\n");
            }
            sendMessage(chatId, sb.toString());
        }
    }

    private void sendMenu(long chatId) throws TelegramApiException {
        InlineKeyboardButton addBtn = new InlineKeyboardButton("➕ Добавить привычку");
        addBtn.setCallbackData("add");

        InlineKeyboardButton listBtn = new InlineKeyboardButton("📋 Список привычек");
        listBtn.setCallbackData("list");

        InlineKeyboardButton doneBtn = new InlineKeyboardButton("✅ Отметить привычку");
        doneBtn.setCallbackData("done_menu");

        List<List<InlineKeyboardButton>> rows = List.of(
                List.of(addBtn, listBtn),
                List.of(doneBtn)
        );
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите действие:");
        message.setReplyMarkup(markup);

        execute(message);
    }

    private void sendDoneMenu(long chatId) throws TelegramApiException {
        var habits = habitService.listHabits(chatId);
        if (habits.isEmpty()) {
            sendMessage(chatId, "Список привычек пуст.");
            return;
        }

        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        for (Habit h : habits) {
            InlineKeyboardButton btn = new InlineKeyboardButton(h.getTitle());
            btn.setCallbackData("done_" + h.getId());
            rows.add(List.of(btn));
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите привычку для отметки:");
        message.setReplyMarkup(markup);

        execute(message);
    }

    private void sendMessage(long chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        execute(message);
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

