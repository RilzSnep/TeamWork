package com.example.demo.config;

import com.example.demo.controller.TelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Конфигурационный класс для инициализации и регистрации Telegram бота.
 */
@Configuration
public class BotInitializer {

    private final TelegramBot telegramBot;

    @Autowired
    public BotInitializer(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    /**
     * Регистрирует бота в Telegram API при запуске приложения.
     *
     * @return TelegramBotsApi instance
     * @throws TelegramApiException если регистрация не удалась
     */
    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            botsApi.registerBot(telegramBot);
            System.out.println("✅ Бот успешно зарегистрирован в Telegram API!");
        } catch (TelegramApiException e) {
            System.err.println("❌ Ошибка при регистрации бота: " + e.getMessage());
            throw e;
        }
        return botsApi;
    }
}