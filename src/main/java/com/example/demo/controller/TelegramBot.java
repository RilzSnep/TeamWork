package com.example.demo.controller;

import com.example.demo.config.BotConfig;
import com.example.demo.entity.User;
import com.example.demo.service.ShelterInfoService;
import com.example.demo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Основной класс Telegram бота для приюта животных.
 * Обрабатывает входящие сообщения и команды от пользователей.
 * Реализует функционал выбора приюта и предоставления информации о приютах.
 */
@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private final UserService userService;
    private final ShelterInfoService shelterInfoService;

    /**
     * Конструктор бота с внедрением зависимостей.
     *
     * @param botConfig конфигурация бота
     * @param userService сервис для работы с пользователями
     * @param shelterInfoService сервис для предоставления информации о приютах
     */
    @Autowired
    public TelegramBot(BotConfig botConfig, UserService userService, ShelterInfoService shelterInfoService) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.userService = userService;
        this.shelterInfoService = shelterInfoService;
        log.info("Бот инициализирован: {}", botConfig.getName());
    }

    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }

    /**
     * Основной метод обработки входящих обновлений от Telegram.
     * Обрабатывает текстовые сообщения и команды от пользователей.
     *
     * @param update объект обновления от Telegram API
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            String userName = update.getMessage().getChat().getUserName();
            String firstName = update.getMessage().getChat().getFirstName();
            String lastName = update.getMessage().getChat().getLastName();

            log.info("Получено сообщение от {} ({} {}): {}", userName, firstName, lastName, messageText);

            // Получаем информацию о пользователе и выбранном приюте
            Optional<User> userOptional = userService.findByChatId(chatId);
            com.example.demo.entity.ShelterType shelterType = userOptional
                    .map(User::getChosenShelter)
                    .orElse(null);

            // Обработка команд
            switch (messageText) {
                case "/start":
                    handleStartCommand(chatId, firstName, lastName, userName);
                    break;
                case "Приют для кошек":
                    handleShelterSelection(chatId, com.example.demo.entity.ShelterType.CAT);
                    break;
                case "Приют для собак":
                    handleShelterSelection(chatId, com.example.demo.entity.ShelterType.DOG);
                    break;
                default:
                    // Если приют уже выбран, обрабатываем команды главного меню
                    if (shelterType != null) {
                        if (isMainMenuCommand(messageText)) {
                            handleMainMenuCommand(chatId, messageText, shelterType);
                        } else if (isShelterInfoCommand(messageText)) {
                            handleShelterInfoCommand(chatId, messageText, shelterType);
                        } else {
                            sendMessage(chatId, "Извините, я не понимаю эту команду. Используйте кнопки меню для навигации.");
                        }
                    } else {
                        sendMessage(chatId, "Пожалуйста, начните с команды /start и выберите приют.");
                    }
            }
        }
    }

    /**
     * Обрабатывает команду /start - входную точку взаимодействия с ботом.
     * Создает нового пользователя или приветствует существующего.
     *
     * @param chatId идентификатор чата
     * @param firstName имя пользователя
     * @param lastName фамилия пользователя
     * @param userName username пользователя
     */
    private void handleStartCommand(Long chatId, String firstName, String lastName, String userName) {
        // Проверяем, есть ли пользователь в базе
        Optional<User> existingUser = userService.findByChatId(chatId);

        if (existingUser.isEmpty()) {
            // Создаем нового пользователя
            User newUser = userService.createNewUser(chatId, firstName, lastName, userName);
            userService.saveUser(newUser);
            log.info("Зарегистрирован новый пользователь: {}", userName);
        }

        // Отправляем приветственное сообщение с клавиатурой выбора приюта
        String welcomeText = """
                🐾 Добро пожаловать в бот приюта для животных! 🐾
                
                Я помогу вам:
                • Узнать информацию о приюте
                • Разобраться с процедурой взятия животного
                • Принимать ежедневные отчеты о питомце
                
                Пожалуйста, выберите приют:
                """;

        sendMessageWithKeyboard(chatId, welcomeText, createShelterSelectionKeyboard());
    }

    /**
     * Обрабатывает выбор приюта пользователем.
     * Сохраняет выбор приюта в базе данных и показывает главное меню.
     *
     * @param chatId идентификатор чата
     * @param shelterType выбранный тип приюта
     */
    private void handleShelterSelection(Long chatId, com.example.demo.entity.ShelterType shelterType) {
        // Обновляем выбранный приют в базе данных
        User updatedUser = userService.updateUserShelter(chatId, shelterType);

        if (updatedUser != null) {
            String shelterInfo = String.format("""
                    🎉 Отлично! Вы выбрали: %s
                    
                    Теперь я могу предоставить вам информацию об этом приюте.
                    
                    Что вас интересует?
                    """, shelterType.getDescription());

            sendMessageWithKeyboard(chatId, shelterInfo, createMainMenuKeyboard());
        } else {
            sendMessage(chatId, "Произошла ошибка. Пожалуйста, начните снова с команды /start");
        }
    }

    /**
     * Обрабатывает запросы из главного меню (Этап 1).
     * Направляет пользователя в соответствующий раздел.
     *
     * @param chatId идентификатор чата
     * @param command команда из главного меню
     * @param shelterType выбранный тип приюта
     */
    private void handleMainMenuCommand(Long chatId, String command, com.example.demo.entity.ShelterType shelterType) {
        log.info("Обработка команды главного меню: {} для приюта {}", command, shelterType);

        switch (command) {
            case "Узнать информацию о приюте":
                sendShelterInfoMenu(chatId, shelterType);
                break;
            case "Как взять животное из приюта":
                sendMessage(chatId, "Раздел 'Как взять животное' в разработке. Скоро будет доступен!");
                break;
            case "Прислать отчет о питомце":
                sendMessage(chatId, "Раздел 'Отчет о питомце' в разработке. Скоро будет доступен!");
                break;
            case "Позвать волонтера":
                callVolunteer(chatId);
                break;
            default:
                sendMessage(chatId, "Пожалуйста, используйте кнопки меню для навигации.");
        }
    }

    /**
     * Обрабатывает запросы информации о приюте (подменю Этапа 1).
     * Предоставляет конкретную информацию о выбранном приюте.
     *
     * @param chatId идентификатор чата
     * @param command команда запроса информации
     * @param shelterType выбранный тип приюта
     */
    private void handleShelterInfoCommand(Long chatId, String command, com.example.demo.entity.ShelterType shelterType) {
        log.info("Обработка запроса информации: {} для {}", command, shelterType);

        String response;
        switch (command) {
            case "Рассказать о приюте":
                response = shelterInfoService.getAbout(shelterType);
                break;
            case "Расписание и адрес":
                response = shelterInfoService.getSchedule(shelterType);
                break;
            case "Контакты охраны":
                response = shelterInfoService.getSecurityContact(shelterType);
                break;
            case "Техника безопасности":
                response = shelterInfoService.getSafetyRules(shelterType);
                break;
            case "Записать контакты":
                response = "Функция записи контактов в разработке. Позовите волонтера для связи.";
                break;
            case "Назад":
                sendMessageWithKeyboard(chatId, "Возврат в главное меню:", createMainMenuKeyboard());
                return;
            default:
                response = "Информация временно недоступна.";
        }

        // Добавляем кнопку "Назад" к ответу
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(response);
        message.setReplyMarkup(createShelterInfoKeyboard());

        sendMessage(message);
    }

    /**
     * Показывает меню информации о приюте.
     *
     * @param chatId идентификатор чата
     * @param shelterType выбранный тип приюта
     */
    private void sendShelterInfoMenu(Long chatId, com.example.demo.entity.ShelterType shelterType) {
        String menuText = """
                ℹ️ Выберите, какую информацию хотите получить о приюте:
                """;

        ReplyKeyboardMarkup keyboard = createShelterInfoKeyboard();
        sendMessageWithKeyboard(chatId, menuText, keyboard);
    }

    /**
     * Обрабатывает вызов волонтера.
     * Предоставляет контактную информацию для связи с волонтером.
     *
     * @param chatId идентификатор чата
     */
    private void callVolunteer(Long chatId) {
        String volunteerText = """
                📞 Волонтер будет связываться с вами в ближайшее время!
                
                Если вопрос срочный, вы можете:
                • Написать на email: volunteer@shelter.ru
                • Позвонить: +7 (800) 123-45-67
                
                Опишите кратко ваш вопрос, чтобы волонтер мог лучше подготовиться.
                """;
        sendMessage(chatId, volunteerText);
    }

    /**
     * Проверяет, является ли команда командой главного меню.
     *
     * @param command текст команды
     * @return true если команда относится к главному меню
     */
    private boolean isMainMenuCommand(String command) {
        return command.equals("Узнать информацию о приюте") ||
                command.equals("Как взять животное из приюта") ||
                command.equals("Прислать отчет о питомце") ||
                command.equals("Позвать волонтера");
    }

    /**
     * Проверяет, является ли команда командой информации о приюте.
     *
     * @param command текст команды
     * @return true если команда относится к меню информации о приюте
     */
    private boolean isShelterInfoCommand(String command) {
        return command.equals("Рассказать о приюте") ||
                command.equals("Расписание и адрес") ||
                command.equals("Контакты охраны") ||
                command.equals("Техника безопасности") ||
                command.equals("Записать контакты") ||
                command.equals("Назад");
    }

    /**
     * Создает клавиатуру для выбора приюта.
     *
     * @return настроенная клавиатура выбора приюта
     */
    private ReplyKeyboardMarkup createShelterSelectionKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Приют для кошек");
        row1.add("Приют для собак");

        keyboard.add(row1);
        keyboardMarkup.setKeyboard(keyboard);

        return keyboardMarkup;
    }

    /**
     * Создает основное меню после выбора приюта.
     *
     * @return настроенная клавиатура главного меню
     */
    private ReplyKeyboardMarkup createMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Узнать информацию о приюте");
        row1.add("Как взять животное из приюта");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Прислать отчет о питомце");
        row2.add("Позвать волонтера");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);

        return keyboardMarkup;
    }

    /**
     * Создает клавиатуру для меню информации о приюте.
     *
     * @return настроенная клавиатура меню информации
     */
    private ReplyKeyboardMarkup createShelterInfoKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Рассказать о приюте");
        row1.add("Расписание и адрес");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Контакты охраны");
        row2.add("Техника безопасности");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Записать контакты");
        row3.add("Назад");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboardMarkup.setKeyboard(keyboard);

        return keyboardMarkup;
    }

    /**
     * Отправляет сообщение с клавиатурой.
     *
     * @param chatId идентификатор чата
     * @param text текст сообщения
     * @param keyboard клавиатура для сообщения
     */
    private void sendMessageWithKeyboard(Long chatId, String text, ReplyKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(keyboard);

        sendMessage(message);
    }

    /**
     * Отправляет простое текстовое сообщение.
     *
     * @param chatId идентификатор чата
     * @param text текст сообщения
     */
    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        sendMessage(message);
    }

    /**
     * Общий метод для отправки сообщений.
     * Обрабатывает исключения Telegram API.
     *
     * @param message объект сообщения для отправки
     */
    private void sendMessage(SendMessage message) {
        try {
            execute(message);
            log.debug("Сообщение отправлено в чат: {}", message.getChatId());
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }
}