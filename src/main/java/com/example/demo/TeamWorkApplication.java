package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Главный класс Spring Boot приложения.
 * Запускает телеграм-бота для приюта животных.
 */
@SpringBootApplication
public class TeamWorkApplication {

	public static void main(String[] args) {
		SpringApplication.run(TeamWorkApplication.class, args);
		System.out.println("🐾 Бот приюта для животных запущен! 🐾");
	}
}
