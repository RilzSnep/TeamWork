package com.example.demo.controller;

import com.example.demo.config.BotConfig;
import com.example.demo.entity.ShelterType;
import com.example.demo.entity.User;
import com.example.demo.exception.ShelterNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.service.AdoptionService;
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
    private final AdoptionService adoptionService;

    /**
     * Конструктор бота с внедрением зависимостей.
     */
    @Autowired
    public TelegramBot(BotConfig botConfig, UserService userService,
                       ShelterInfoService shelterInfoService, AdoptionService adoptionService) {
        this.botConfig = botConfig;
        this.userService = userService;
        this.shelterInfoService = shelterInfoService;
        this.adoptionService = adoptionService;
        log.info("Бот инициализирован: {}", botConfig.getName());
    }

    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    /**
     * Основной метод обработки входящих обновлений от Telegram.
     */
    @Override
    public void onUpdateReceived(Update update) {
        try {
            log.debug("Получено обновление: {}", update.getUpdateId());

            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                Long chatId = update.getMessage().getChatId();
                String userName = update.getMessage().getChat().getUserName();
                String firstName = update.getMessage().getChat().getFirstName();
                String lastName = update.getMessage().getChat().getLastName();

                log.info("Получено сообщение от {} ({} {}): {}", userName, firstName, lastName, messageText);

                // Получаем информацию о пользователе и выбранном приюте
                Optional<User> userOptional = userService.findByChatId(chatId);
                ShelterType shelterType = userOptional
                        .map(User::getChosenShelter)
                        .orElse(null);

                // Обработка команд с обработкой исключений
                handleUserMessage(chatId, messageText, shelterType, firstName, lastName, userName);
            }
        } catch (Exception e) {
            log.error("Критическая ошибка при обработке обновления: {}", e.getMessage(), e);
            // Отправляем сообщение об ошибке пользователю, если возможно получить chatId
            if (update.hasMessage()) {
                sendSafeMessage(update.getMessage().getChatId(),
                        "⚠️ Произошла непредвиденная ошибка. Пожалуйста, попробуйте позже.");
            }
        }
    }

    /**
     * Безопасная обработка сообщения пользователя с обработкой исключений
     */
    private void handleUserMessage(Long chatId, String messageText, ShelterType shelterType,
                                   String firstName, String lastName, String userName) {
        try {
            switch (messageText) {
                case "/start":
                    handleStartCommand(chatId, firstName, lastName, userName);
                    break;
                case "Приют для кошек":
                    handleShelterSelection(chatId, ShelterType.CAT);
                    break;
                case "Приют для собак":
                    handleShelterSelection(chatId, ShelterType.DOG);
                    break;
                default:
                    if (shelterType != null) {
                        if (isMainMenuCommand(messageText)) {
                            handleMainMenuCommand(chatId, messageText, shelterType);
                        } else if (isShelterInfoCommand(messageText)) {
                            handleShelterInfoCommand(chatId, messageText, shelterType);
                        } else if (isAdoptionCommand(messageText)) {
                            handleAdoptionCommand(chatId, messageText, shelterType);
                        } else {
                            sendSafeMessage(chatId,
                                    "Извините, я не понимаю эту команду. Используйте кнопки меню для навигации.");
                        }
                    } else {
                        sendSafeMessage(chatId,
                                "Пожалуйста, начните с команды /start и выберите приют.");
                    }
            }
        } catch (UserNotFoundException e) {
            log.warn("Ошибка пользователя: {}", e.getMessage());
            sendSafeMessage(chatId, e.getMessage());
        } catch (ShelterNotFoundException e) {
            log.warn("Ошибка приюта: {}", e.getMessage());
            sendSafeMessage(chatId, e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при обработке сообщения пользователя: {}", e.getMessage(), e);
            sendSafeMessage(chatId,
                    "⚠️ Произошла ошибка при обработке вашего запроса. Пожалуйста, попробуйте позже.");
        }
    }

    /**
     * Обрабатывает команду /start - входную точку взаимодействия с ботом.
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
     */
    private void handleShelterSelection(Long chatId, ShelterType shelterType) {
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
            sendSafeMessage(chatId, "Произошла ошибка. Пожалуйста, начните снова с команды /start");
        }
    }

    /**
     * Обрабатывает запросы из главного меню (Этап 1).
     */
    private void handleMainMenuCommand(Long chatId, String command, ShelterType shelterType) {
        log.info("Обработка команды главного меню: {} для приюта {}", command, shelterType);

        switch (command) {
            case "Узнать информацию о приюте":
                sendShelterInfoMenu(chatId, shelterType);
                break;
            case "Как взять животное из приюта":
                showAdoptionMenu(chatId, shelterType);
                break;
            case "Прислать отчет о питомце":
                sendSafeMessage(chatId, "Раздел 'Отчет о питомце' в разработке. Скоро будет доступен!");
                break;
            case "Позвать волонтера":
                callVolunteer(chatId);
                break;
            default:
                sendSafeMessage(chatId, "Пожалуйста, используйте кнопки меню для навигации.");
        }
    }

    /**
     * Обрабатывает запросы информации о приюте (подменю Этапа 1).
     */
    private void handleShelterInfoCommand(Long chatId, String command, ShelterType shelterType) {
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

        sendSafeMessage(message);
    }

    /**
     * Обрабатывает команды меню "Как взять животное из приюта"
     */
    private void handleAdoptionCommand(Long chatId, String command, ShelterType shelterType) {
        log.info("Обработка команды усыновления: {} для приюта {}", command, shelterType);

        String response;
        switch (command) {
            case "Правила знакомства":
                response = adoptionService.getAdoptionInfo("meeting_rules");
                break;
            case "Необходимые документы":
                response = adoptionService.getAdoptionInfo("documents");
                break;
            case "Рекомендации по транспортировке":
                response = adoptionService.getAdoptionInfo("transportation");
                break;
            case "Обустройство дома для щенка/котенка":
                response = adoptionService.getAdoptionInfo("home_preparation_young");
                break;
            case "Обустройство дома для взрослого животного":
                response = adoptionService.getAdoptionInfo("home_preparation_adult");
                break;
            case "Обустройство для животного-инвалида":
                response = adoptionService.getAdoptionInfo("home_preparation_disabled");
                break;
            case "Советы кинолога":
                response = adoptionService.getShelterSpecificInfo("dog_behavior_tips", shelterType);
                break;
            case "Рекомендации кинологов":
                response = adoptionService.getShelterSpecificInfo("dog_trainer_recommendations", shelterType);
                break;
            case "Причины отказа":
                response = adoptionService.getShelterSpecificInfo("rejection_reasons", shelterType);
                break;
            case "Записать контакты":
                response = handleContactRegistration(chatId);
                break;
            case "Назад в главное меню":
                sendMessageWithKeyboard(chatId, "Возврат в главное меню:", createMainMenuKeyboard());
                return;
            default:
                response = "Информация временно недоступна.";
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(response);
        message.setReplyMarkup(createAdoptionMenuKeyboard());

        sendSafeMessage(message);
    }

    /**
     * Показывает меню информации о приюте.
     */
    private void sendShelterInfoMenu(Long chatId, ShelterType shelterType) {
        String menuText = """
                ℹ️ Выберите, какую информацию хотите получить о приюте:
                """;

        ReplyKeyboardMarkup keyboard = createShelterInfoKeyboard();
        sendMessageWithKeyboard(chatId, menuText, keyboard);
    }

    /**
     * Показывает меню "Как взять животное из приюта"
     */
    private void showAdoptionMenu(Long chatId, ShelterType shelterType) {
        String menuText = """
                📋 Раздел "Как взять животное из приюта"
                
                Здесь вы найдете всю информацию о процессе усыновления:
                """;

        ReplyKeyboardMarkup keyboard = createAdoptionMenuKeyboard();
        sendMessageWithKeyboard(chatId, menuText, keyboard);
    }

    /**
     * Обрабатывает вызов волонтера.
     */
    private void callVolunteer(Long chatId) {
        String volunteerText = """
                📞 Волонтер будет связываться с вами в ближайшее время!
                
                Если вопрос срочный, вы можете:
                • Написать на email: volunteer@shelter.ru
                • Позвонить: +7 (800) 123-45-67
                
                Опишите кратко ваш вопрос, чтобы волонтер мог лучше подготовиться.
                """;
        sendSafeMessage(chatId, volunteerText);
    }

    /**
     * Обрабатывает регистрацию контактов
     */
    private String handleContactRegistration(Long chatId) {
        try {
            // Здесь будет логика сохранения контактов
            // Пока заглушка
            return """
                    📞 Ваши контактные данные записаны!
                    
                    Волонтер свяжется с вами в ближайшее время для обсуждения деталей.
                    """;
        } catch (Exception e) {
            log.error("Ошибка при записи контактов: {}", e.getMessage());
            return "❌ Не удалось записать контакты. Пожалуйста, попробуйте позже или позовите волонтера.";
        }
    }

    /**
     * Проверяет, является ли команда командой главного меню.
     */
    private boolean isMainMenuCommand(String command) {
        return command.equals("Узнать информацию о приюте") ||
                command.equals("Как взять животное из приюта") ||
                command.equals("Прислать отчет о питомце") ||
                command.equals("Позвать волонтера");
    }

    /**
     * Проверяет, является ли команда командой информации о приюте.
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
     * Проверяет, является ли команда командой усыновления.
     */
    private boolean isAdoptionCommand(String command) {
        return command.equals("Правила знакомства") ||
                command.equals("Необходимые документы") ||
                command.equals("Рекомендации по транспортировке") ||
                command.equals("Обустройство дома для щенка/котенка") ||
                command.equals("Обустройство дома для взрослого животного") ||
                command.equals("Обустройство для животного-инвалида") ||
                command.equals("Советы кинолога") ||
                command.equals("Рекомендации кинологов") ||
                command.equals("Причины отказа") ||
                command.equals("Записать контакты") ||
                command.equals("Назад в главное меню");
    }

    /**
     * Создает клавиатуру для выбора приюта.
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
     * Создает клавиатуру для меню усыновления
     */
    private ReplyKeyboardMarkup createAdoptionMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Правила знакомства");
        row1.add("Необходимые документы");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Рекомендации по транспортировке");
        row2.add("Обустройство дома для щенка/котенка");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Обустройство дома для взрослого животного");
        row3.add("Обустройство для животного-инвалида");

        KeyboardRow row4 = new KeyboardRow();
        row4.add("Советы кинолога");
        row4.add("Рекомендации кинологов");

        KeyboardRow row5 = new KeyboardRow();
        row5.add("Причины отказа");
        row5.add("Записать контакты");

        KeyboardRow row6 = new KeyboardRow();
        row6.add("Назад в главное меню");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);
        keyboard.add(row5);
        keyboard.add(row6);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Отправляет сообщение с клавиатурой.
     */
    private void sendMessageWithKeyboard(Long chatId, String text, ReplyKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(keyboard);

        sendSafeMessage(message);
    }

    /**
     * Безопасная отправка сообщения с обработкой исключений Telegram API
     */
    private void sendSafeMessage(Long chatId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(text);
            execute(message);
            log.debug("Сообщение отправлено в чат: {}", chatId);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения в чат {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Безопасная отправка сообщения (перегруженная версия)
     */
    private void sendSafeMessage(SendMessage message) {
        try {
            execute(message);
            log.debug("Сообщение отправлено в чат: {}", message.getChatId());
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }
}