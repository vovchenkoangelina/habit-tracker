package com.example.habitbot;

import com.example.habitbot.bot.HabitBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class HabitbotApplication {

	public static void main(String[] args) {
		SpringApplication.run(HabitbotApplication.class, args);
	}

	@Bean
	public TelegramBotsApi telegramBotsApi(HabitBot habitBot) throws TelegramApiException {
		TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
		botsApi.registerBot(habitBot);
		return botsApi;
	}

}
