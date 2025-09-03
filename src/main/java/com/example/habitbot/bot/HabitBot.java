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

import java.util.*;

@Component
public class HabitBot extends TelegramLongPollingBot {

    private final HabitService habitService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    private final Map<Long, TempHabitData> tempHabitData = new HashMap<>();

    private static class TempHabitData {
        String title;
        boolean remindersEnabled;
        String reminderTime;
        boolean waitingForTimeInput;
    }

    private final Map<Long, UserState> userStates = new HashMap<>();

    private enum UserState {
        WAITING_FOR_HABIT_NAME,
        WAITING_FOR_REMINDER_CHOICE,
        WAITING_FOR_REMINDER_TIME
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

        UserState state = userStates.get(chatId);
        TempHabitData temp = tempHabitData.get(chatId);

        if (state == UserState.WAITING_FOR_REMINDER_TIME && temp != null && temp.waitingForTimeInput) {
            if (text.matches("\\d{2}:\\d{2}")) {
                temp.reminderTime = text;
                Habit habit = habitService.addHabit(temp.title, chatId, true, temp.reminderTime);
                sendMessage(chatId, "✅ Привычка добавлена с напоминанием на " + temp.reminderTime + ": " + habit.getTitle());
                tempHabitData.remove(chatId);
                userStates.remove(chatId);
            } else {
                sendMessage(chatId, "⛔ Неверный формат времени. Используйте HH:mm (например, 09:30).");
            }
            return;
        }

        if (state == UserState.WAITING_FOR_HABIT_NAME) {
            temp = new TempHabitData();
            temp.title = text;
            tempHabitData.put(chatId, temp);

            InlineKeyboardButton yesBtn = new InlineKeyboardButton("Да");
            yesBtn.setCallbackData("reminder_yes");
            InlineKeyboardButton noBtn = new InlineKeyboardButton("Нет");
            noBtn.setCallbackData("reminder_no");

            sendInlineKeyboard(chatId, "💡 Присылать напоминания для этой привычки?", List.of(List.of(yesBtn, noBtn)));

            userStates.put(chatId, UserState.WAITING_FOR_REMINDER_CHOICE);
            return;
        }

        if (text.endsWith("+1")) {
            String habitName = text.substring(0, text.length() - 2).trim();
            try {
                Habit habit = habitService.incrementHabit(habitName, chatId);
                sendMessage(chatId, "✅ Привычка \"" + habit.getTitle() + "\" выполнена сегодня. Всего раз выполнено: " + habit.getCompletionCount());
            } catch (IllegalArgumentException e) {
                sendMessage(chatId, e.getMessage());
            }
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
        UserState state = userStates.get(chatId);
        TempHabitData temp = tempHabitData.get(chatId);

        if (state == UserState.WAITING_FOR_REMINDER_CHOICE && temp != null) {
            if ("reminder_yes".equals(data)) {
                temp.remindersEnabled = true;

                InlineKeyboardButton specificBtn = new InlineKeyboardButton("Выбрать время");
                specificBtn.setCallbackData("reminder_time");
                InlineKeyboardButton randomBtn = new InlineKeyboardButton("Случайное время");
                randomBtn.setCallbackData("reminder_random");

                sendInlineKeyboard(chatId, "⏰ Выберите время напоминания:", List.of(List.of(specificBtn, randomBtn)));
                userStates.put(chatId, UserState.WAITING_FOR_REMINDER_TIME);
            } else if ("reminder_no".equals(data)) {
                temp.remindersEnabled = false;
                Habit habit = habitService.addHabit(temp.title, chatId, false, null);
                sendMessage(chatId, "✅ Привычка добавлена: " + habit.getTitle());
                tempHabitData.remove(chatId);
                userStates.remove(chatId);
            }
            return;
        }

        if (state == UserState.WAITING_FOR_REMINDER_TIME && temp != null) {
            if ("reminder_random".equals(data)) {
                Habit habit = habitService.addHabit(temp.title, chatId, true, "random");
                sendMessage(chatId, "✅ Привычка добавлена с напоминаниями в случайное время: " + habit.getTitle());
                tempHabitData.remove(chatId);
                userStates.remove(chatId);
            } else if ("reminder_time".equals(data)) {
                sendMessage(chatId, "Введите время в формате HH:mm, например 10:30");
                temp.waitingForTimeInput = true;
            }
            return;
        }

        if (data.startsWith("done_")) {
            Long habitId = Long.parseLong(data.replace("done_", ""));
            Habit habit = habitService.markHabitDone(habitId, chatId);
            sendMessage(chatId, "✅ Привычка отмечена: " + habit.getTitle() +
                    " (выполнено раз: " + habit.getCompletionCount() + ")");
            sendHabitsList(chatId);
        } else if (data.startsWith("confirm_delete_")) {
            Long habitId = Long.parseLong(data.replace("confirm_delete_", ""));
            InlineKeyboardButton yesBtn = new InlineKeyboardButton("Да");
            yesBtn.setCallbackData("delete_" + habitId);
            InlineKeyboardButton noBtn = new InlineKeyboardButton("Отмена");
            noBtn.setCallbackData("cancel");
            sendInlineKeyboard(chatId, "❗ Вы уверены, что хотите удалить эту привычку?", List.of(List.of(yesBtn, noBtn)));
        } else if (data.startsWith("delete_")) {
            Long habitId = Long.parseLong(data.replace("delete_", ""));
            habitService.deleteHabit(habitId, chatId);
            sendMessage(chatId, "❌ Привычка удалена");
            sendHabitsList(chatId);
        } else if ("cancel".equals(data)) {
            sendHabitsList(chatId);
        } else if ("add".equals(data)) {
            sendMessage(chatId, "✏️ Введите название привычки:");
            userStates.put(chatId, UserState.WAITING_FOR_HABIT_NAME);
        } else if ("list".equals(data)) {
            sendHabitsList(chatId);
        } else if (data.startsWith("stats_")) {
            Long habitId = Long.parseLong(data.replace("stats_", ""));
            Habit habit = habitService.getHabitByIdAndChatId(habitId, chatId);
            sendMessage(chatId, "📊 Статистика по привычке \"" + habit.getTitle() + "\":\n" +
                    "Всего выполнено: " + habit.getCompletionCount() + "\n" +
                    "Текущий стрик: " + habit.getCurrentStreak() + "\n" +
                    "Максимальный стрик: " + habit.getMaxStreak());
        } else {
            sendMessage(chatId, "Неизвестное действие");
        }
    }

    private void sendHabitsList(long chatId) throws TelegramApiException {
        var habits = habitService.listHabits(chatId);
        if (habits.isEmpty()) {
            sendMessage(chatId, "Список привычек пуст.");
            return;
        }

        for (Habit h : habits) {
            InlineKeyboardButton doneBtn = new InlineKeyboardButton("✅");
            doneBtn.setCallbackData("done_" + h.getId());

            InlineKeyboardButton deleteBtn = new InlineKeyboardButton("🗑️");
            deleteBtn.setCallbackData("confirm_delete_" + h.getId());

            InlineKeyboardButton statsBtn = new InlineKeyboardButton("📊");
            statsBtn.setCallbackData("stats_" + h.getId());

            List<List<InlineKeyboardButton>> rows = List.of(
                    List.of(doneBtn, deleteBtn, statsBtn)
            );

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(rows);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(h.getTitle() +
                    " (выполнено раз: " + h.getCompletionCount() + ")");
            message.setReplyMarkup(markup);

            execute(message);
        }
    }


    private void sendMenu(long chatId) throws TelegramApiException {
        InlineKeyboardButton addBtn = new InlineKeyboardButton("➕ Добавить привычку");
        addBtn.setCallbackData("add");
        InlineKeyboardButton listBtn = new InlineKeyboardButton("📋 Список привычек");
        listBtn.setCallbackData("list");

        sendInlineKeyboard(chatId, "Выберите действие:", List.of(List.of(addBtn, listBtn)));
    }

    private void sendInlineKeyboard(long chatId, String text, List<List<InlineKeyboardButton>> buttons) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(buttons);
        message.setReplyMarkup(markup);

        execute(message);
    }

    private void sendMessage(long chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        execute(message);
    }

    public void sendReminder(Long chatId, String habitTitle) {
        try {
            sendMessage(chatId, "⏰ Напоминание: не забудьте выполнить привычку '" + habitTitle + "'");
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


