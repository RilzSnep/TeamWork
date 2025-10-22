// ContactService.java
package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ContactService {

    private final UserRepository userRepository;

    // Регулярное выражение для валидации российских номеров телефонов
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+7-9\\d{2}-\\d{3}-\\d{2}-\\d{2}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    @Autowired
    public ContactService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Сохраняет контактные данные пользователя
     */
    @Transactional
    public boolean saveUserContacts(Long chatId, String phoneNumber, String email, String address) {
        try {
            Optional<User> userOpt = userRepository.findById(chatId);
            if (userOpt.isEmpty()) {
                throw new UserNotFoundException(chatId);
            }

            User user = userOpt.get();

            // Валидация номера телефона
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                if (!isValidPhoneNumber(phoneNumber)) {
                    log.warn("Неверный формат номера телефона: {}", phoneNumber);
                    return false;
                }
                user.setPhoneNumber(phoneNumber);
            }

            // Валидация email
            if (email != null && !email.isEmpty()) {
                if (!isValidEmail(email)) {
                    log.warn("Неверный формат email: {}", email);
                    return false;
                }
                user.setEmail(email);
            }

            // Сохранение адреса
            if (address != null && !address.isEmpty()) {
                user.setAddress(address);
            }

            user.setContactsUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Контактные данные сохранены для пользователя: {}", chatId);
            return true;

        } catch (UserNotFoundException e) {
            log.error("Пользователь не найден: {}", chatId);
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при сохранении контактных данных для пользователя {}: {}", chatId, e.getMessage());
            return false;
        }
    }

    /**
     * Валидация номера телефона по маске +7-9**-***-**-**
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        return PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    /**
     * Валидация email
     */
    public boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Получает контактные данные пользователя
     */
    public String getUserContactsInfo(Long chatId) {
        try {
            Optional<User> userOpt = userRepository.findById(chatId);
            if (userOpt.isEmpty()) {
                throw new UserNotFoundException(chatId);
            }

            User user = userOpt.get();
            StringBuilder contacts = new StringBuilder();

            contacts.append("📋 Ваши контактные данные:\n\n");

            if (user.getPhoneNumber() != null) {
                contacts.append("📞 Телефон: ").append(user.getPhoneNumber()).append("\n");
            } else {
                contacts.append("📞 Телефон: не указан\n");
            }

            if (user.getEmail() != null) {
                contacts.append("📧 Email: ").append(user.getEmail()).append("\n");
            } else {
                contacts.append("📧 Email: не указан\n");
            }

            if (user.getAddress() != null) {
                contacts.append("🏠 Адрес: ").append(user.getAddress()).append("\n");
            } else {
                contacts.append("🏠 Адрес: не указан\n");
            }

            if (user.getContactsUpdatedAt() != null) {
                contacts.append("\n🕐 Обновлено: ").append(user.getContactsUpdatedAt().toLocalDate()).append("\n");
            }

            return contacts.toString();

        } catch (Exception e) {
            log.error("Ошибка при получении контактных данных пользователя {}: {}", chatId, e.getMessage());
            return "❌ Не удалось получить ваши контактные данные.";
        }
    }

    /**
     * Проверяет, заполнены ли контактные данные
     */
    public boolean areContactsFilled(Long chatId) {
        try {
            Optional<User> userOpt = userRepository.findById(chatId);
            if (userOpt.isEmpty()) {
                return false;
            }

            User user = userOpt.get();
            return user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty();
        } catch (Exception e) {
            log.error("Ошибка при проверке контактных данных пользователя {}: {}", chatId, e.getMessage());
            return false;
        }
    }
}