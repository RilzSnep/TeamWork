package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Сущность для представления пользователя телеграм-бота.
 * Содержит основную информацию о пользователе и его выборе приюта.
 */
@Entity
@Table(name = "users")
@Data
public class User {

    /**
     * Уникальный идентификатор чата с пользователем.
     * Используется как первичный ключ.
     */
    @Id
    @Column(name = "chat_id")
    private Long chatId;

    /**
     * Имя пользователя в Telegram.
     */
    @Column(name = "first_name")
    private String firstName;

    /**
     * Фамилия пользователя в Telegram.
     */
    @Column(name = "last_name")
    private String lastName;

    /**
     * Username пользователя в Telegram.
     */
    @Column(name = "user_name")
    private String userName;

    /**
     * Номер телефона пользователя.
     * Заполняется на этапе сбора контактных данных.
     */
    @Column(name = "phone_number")
    private String phoneNumber;

    /**
     * Выбранный пользователем приют.
     * Определяет, информацию о каком приюте получать пользователь.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "chosen_shelter")
    private ShelterType chosenShelter;

    /**
     * Дата и время регистрации пользователя в боте.
     */
    @Column(name = "registered_at")
    private LocalDateTime registeredAt;
}