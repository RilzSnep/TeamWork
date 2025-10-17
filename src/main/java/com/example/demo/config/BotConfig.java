package com.example.demo.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс для настроек Telegram бота.
 * Считывает настройки из application.properties
 */
@Configuration
@Data
public class BotConfig {

    @Value("${bot.token}")
    private String token;

    @Value("${bot.name}")
    private String name;
}