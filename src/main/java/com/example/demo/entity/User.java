package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Сущность для представления пользователя телеграм-бота.
 * Содержит основную информацию о пользователе и его выборе приюта.
 */
// User.java - добавляем поля для контактов
@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "user_name")
    private String userName;

    /**
     * Номер телефона пользователя.
     * Заполняется на этапе сбора контактных данных.
     */
    @Column(name = "phone_number")
    private String phoneNumber;

    /**
     * Email пользователя.
     */
    @Column(name = "email")
    private String email;

    /**
     * Адрес пользователя.
     */
    @Column(name = "address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "chosen_shelter")
    private ShelterType chosenShelter;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    /**
     * Дата и время последнего обновления контактов
     */
    @Column(name = "contacts_updated_at")
    private LocalDateTime contactsUpdatedAt;
}