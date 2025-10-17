package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Сервис для выполнения операций с сущностью {@link User}.
 * Обеспечивает бизнес-логику работы с пользователями.
 */
@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Сохраняет или обновляет информацию о пользователе в базе данных.
     * Если пользователь новый, устанавливает дату регистрации.
     *
     * @param user объект пользователя, который должен быть сохранен
     * @return сохраненный объект пользователя
     */
    public User saveUser(User user) {
        log.info("Сохранение пользователя с chatId: {}", user.getChatId());

        // Если пользователь новый, устанавливаем дату регистрации
        if (user.getRegisteredAt() == null) {
            user.setRegisteredAt(LocalDateTime.now());
        }

        return userRepository.save(user);
    }

    /**
     * Ищет пользователя по его уникальному идентификатору чата.
     *
     * @param chatId идентификатор чата пользователя в Telegram
     * @return {@link Optional}, содержащий пользователя, если он найден, иначе пустой {@link Optional}
     */
    public Optional<User> findByChatId(Long chatId) {
        log.debug("Поиск пользователя по chatId: {}", chatId);
        return userRepository.findById(chatId);
    }

    /**
     * Создает нового пользователя на основе данных из Telegram.
     *
     * @param chatId идентификатор чата
     * @param firstName имя пользователя
     * @param lastName фамилия пользователя
     * @param userName username пользователя
     * @return новый объект пользователя
     */
    public User createNewUser(Long chatId, String firstName, String lastName, String userName) {
        log.info("Создание нового пользователя: {}", userName);

        User user = new User();
        user.setChatId(chatId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUserName(userName);
        user.setRegisteredAt(LocalDateTime.now());

        return user;
    }

    /**
     * Обновляет выбранный приют для пользователя.
     *
     * @param chatId идентификатор чата пользователя
     * @param shelterType выбранный тип приюта
     * @return обновленный пользователь или null, если пользователь не найден
     */
    public User updateUserShelter(Long chatId, com.example.demo.entity.ShelterType shelterType) {
        Optional<User> userOptional = findByChatId(chatId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setChosenShelter(shelterType);
            log.info("Обновлен приют пользователя {}: {}", chatId, shelterType);
            return saveUser(user);
        }
        log.warn("Пользователь с chatId {} не найден для обновления приюта", chatId);
        return null;
    }
}