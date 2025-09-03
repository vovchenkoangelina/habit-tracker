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

        if (data.startsWith("done_")) {
            Long habitId = Long.parseLong(data.replace("done_", ""));
            Habit habit = habitService.markHabitDone(habitId, chatId);
            sendMessage(chatId, "✅ Привычка отмечена: " + habit.getTitle() +
                    " (выполнено раз: " + habit.getCompletionCount() + ")");
            sendHabitsList(chatId); // обновляем список после отметки
        } else if (data.startsWith("confirm_delete_")) {
            Long habitId = Long.parseLong(data.replace("confirm_delete_", ""));
            InlineKeyboardButton yesBtn = new InlineKeyboardButton("Да");
            yesBtn.setCallbackData("delete_" + habitId);

            InlineKeyboardButton noBtn = new InlineKeyboardButton("Отмена");
            noBtn.setCallbackData("cancel");

            List<List<InlineKeyboardButton>> rows = List.of(List.of(yesBtn, noBtn));
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(rows);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("❗ Вы уверены, что хотите удалить эту привычку?");
            message.setReplyMarkup(markup);

            execute(message);
        } else if (data.startsWith("delete_")) {
            Long habitId = Long.parseLong(data.replace("delete_", ""));
            habitService.deleteHabit(habitId, chatId);
            sendMessage(chatId, "❌ Привычка удалена");
        } else if (data.equals("cancel")) {
            sendHabitsList(chatId);
        } else if (data.equals("add")) {
            sendMessage(chatId, "✏️ Введите название привычки:");
            userStates.put(chatId, UserState.WAITING_FOR_HABIT_NAME);
        } else if (data.equals("list")) {
            sendHabitsList(chatId);
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

            List<List<InlineKeyboardButton>> rows = List.of(List.of(doneBtn, deleteBtn));
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

        List<List<InlineKeyboardButton>> rows = List.of(
                List.of(addBtn, listBtn)
        );
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите действие:");
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

