package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.exception.CustomException;
import com.example.demo.exception.UserNotFoundException;
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
        try {
            log.info("Сохранение пользователя с chatId: {}", user.getChatId());

            if (user.getRegisteredAt() == null) {
                user.setRegisteredAt(LocalDateTime.now());
            }

            User savedUser = userRepository.save(user);
            log.info("Пользователь успешно сохранен: {}", savedUser.getChatId());
            return savedUser;
        } catch (Exception e) {
            log.error("Ошибка при сохранении пользователя {}: {}", user.getChatId(), e.getMessage());
            throw new CustomException("Не удалось сохранить пользователя", e);
        }
    }

    /**
     * Ищет пользователя по его уникальному идентификатору чата.
     *
     * @param chatId идентификатор чата пользователя в Telegram
     * @return {@link Optional}, содержащий пользователя, если он найден, иначе пустой {@link Optional}
     */
    public Optional<User> findByChatId(Long chatId) {
        try {
            log.debug("Поиск пользователя по chatId: {}", chatId);
            return userRepository.findById(chatId);
        } catch (Exception e) {
            log.error("Ошибка при поиске пользователя {}: {}", chatId, e.getMessage());
            throw new CustomException("Не удалось найти пользователя", e);
        }
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
        try {
            log.info("Создание нового пользователя: {}", userName);

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setUserName(userName);
            user.setRegisteredAt(LocalDateTime.now());

            return user;
        } catch (Exception e) {
            log.error("Ошибка при создании пользователя {}: {}", userName, e.getMessage());
            throw new CustomException("Не удалось создать пользователя", e);
        }
    }

    /**
     * Обновляет выбранный приют для пользователя.
     *
     * @param chatId идентификатор чата пользователя
     * @param shelterType выбранный тип приюта
     * @return обновленный пользователь или null, если пользователь не найден
     */
    public User updateUserShelter(Long chatId, com.example.demo.entity.ShelterType shelterType) {
        try {
            Optional<User> userOptional = findByChatId(chatId);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setChosenShelter(shelterType);
                log.info("Обновлен приют пользователя {}: {}", chatId, shelterType);
                return saveUser(user);
            }
            throw new UserNotFoundException(chatId);
        } catch (UserNotFoundException e) {
            throw e; // Пробрасываем дальше специфичные исключения
        } catch (Exception e) {
            log.error("Ошибка при обновлении приюта для пользователя {}: {}", chatId, e.getMessage());
            throw new CustomException("Не удалось обновить информацию о приюте", e);
        }
    }

    /**
     * Проверяет существование пользователя
     *
     * @param chatId идентификатор чата пользователя
     * @return true если пользователь существует
     */
    public boolean userExists(Long chatId) {
        try {
            return userRepository.existsById(chatId);
        } catch (Exception e) {
            log.error("Ошибка при проверке существования пользователя {}: {}", chatId, e.getMessage());
            throw new CustomException("Не удалось проверить существование пользователя", e);
        }
    }

    /**
     * Удаляет пользователя по chatId
     *
     * @param chatId идентификатор чата пользователя
     */
    public void deleteUser(Long chatId) {
        try {
            if (userRepository.existsById(chatId)) {
                userRepository.deleteById(chatId);
                log.info("Пользователь {} удален", chatId);
            } else {
                throw new UserNotFoundException(chatId);
            }
        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при удалении пользователя {}: {}", chatId, e.getMessage());
            throw new CustomException("Не удалось удалить пользователя", e);
        }
    }
}