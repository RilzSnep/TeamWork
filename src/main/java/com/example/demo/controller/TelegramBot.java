package com.example.demo.controller;

import com.example.demo.config.BotConfig;
import com.example.demo.entity.ShelterType;
import com.example.demo.entity.User;
import com.example.demo.exception.ShelterNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private final AnimalService animalService;
    private final AdoptionProcessService adoptionProcessService;
    private final ContactService contactService;
    private final UserRepository userRepository;
    private final ReportService reportService;

    // Состояния пользователей для отслеживания процессов
    private final Map<Long, String> userStates = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, String>> userTempData = new ConcurrentHashMap<>();

    /**
     * Конструктор бота с внедрением зависимостей.
     */
    @Autowired
    public TelegramBot(BotConfig botConfig, UserService userService,
                       ShelterInfoService shelterInfoService, AdoptionService adoptionService,
                       AnimalService animalService, AdoptionProcessService adoptionProcessService,
                       ContactService contactService, UserRepository userRepository,
                       ReportService reportService) {
        this.botConfig = botConfig;
        this.userService = userService;
        this.shelterInfoService = shelterInfoService;
        this.adoptionService = adoptionService;
        this.animalService = animalService;
        this.adoptionProcessService = adoptionProcessService;
        this.contactService = contactService;
        this.userRepository = userRepository;
        this.reportService = reportService;
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
            // Проверяем, находится ли пользователь в процессе регистрации контактов или отчета
            String userState = userStates.get(chatId);
            if (userState != null && userState.startsWith("CONTACT_")) {
                handleContactRegistrationState(chatId, messageText, userState);
                return;
            } else if (userState != null && userState.startsWith("REPORT_")) {
                handleReportState(chatId, messageText, userState);
                return;
            }

            // Обработка обычных команд
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
                case "Отмена":
                    handleCancel(chatId);
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
                startReportProcess(chatId);
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
                startContactRegistration(chatId);
                return;  // Выходим, так как отправляем отдельное сообщение
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
            case "Список животных":
                response = animalService.getAvailableAnimalsList(shelterType);
                break;
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
                startContactRegistration(chatId);
                return;  // Выходим, так как отправляем отдельное сообщение
            case "Назад в главное меню":
                userStates.remove(chatId);  // Сбрасываем состояние
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

    // ========== ФУНКЦИОНАЛ РЕГИСТРАЦИИ КОНТАКТОВ ==========

    /**
     * Начинает процесс регистрации контактов
     */
    private void startContactRegistration(Long chatId) {
        String currentState = userStates.get(chatId);

        if (currentState == null) {
            // Начинаем процесс регистрации
            userStates.put(chatId, "CONTACT_AWAITING_PHONE");

            String instruction = """
                    📝 Запись контактных данных
                    
                    Для связи с вами нам необходимы ваши контактные данные.
                    
                    📱 Пожалуйста, введите ваш номер телефона в формате:
                    +7-9**-***-**-**
                    
                    Например: +7-912-345-67-89
                    
                    ❗ Важно: соблюдайте указанный формат!
                    """;

            // Создаем клавиатуру с кнопкой отмены
            ReplyKeyboardMarkup keyboard = createCancelKeyboard();
            sendMessageWithKeyboard(chatId, instruction, keyboard);
        } else {
            // Пользователь уже в процессе регистрации
            sendSafeMessage(chatId, "⚠️ Вы уже находитесь в процессе записи контактов. Пожалуйста, завершите его или отмените.");
        }
    }

    /**
     * Обрабатывает состояния регистрации контактов
     */
    private void handleContactRegistrationState(Long chatId, String messageText, String userState) {
        switch (userState) {
            case "CONTACT_AWAITING_PHONE":
                handlePhoneInput(chatId, messageText);
                break;
            case "CONTACT_AWAITING_EMAIL":
                handleEmailInput(chatId, messageText);
                break;
            case "CONTACT_AWAITING_ADDRESS":
                handleAddressInput(chatId, messageText);
                break;
            default:
                userStates.remove(chatId);
                sendSafeMessage(chatId, "⚠️ Произошла ошибка. Пожалуйста, начните заново.");
        }
    }

    /**
     * Обработка ввода телефона
     */
    private void handlePhoneInput(Long chatId, String phoneNumber) {
        if (phoneNumber.equals("Отмена")) {
            handleCancel(chatId);
            return;
        }

        if (contactService.isValidPhoneNumber(phoneNumber)) {
            // Сохраняем телефон и запрашиваем email
            contactService.saveUserContacts(chatId, phoneNumber, null, null);
            userStates.put(chatId, "CONTACT_AWAITING_EMAIL");

            String message = """
                    ✅ Номер телефона сохранен!
                    
                    📧 Теперь введите ваш email (необязательно):
                    
                    Или нажмите "Пропустить", чтобы перейти к следующему шагу.
                    """;

            ReplyKeyboardMarkup keyboard = createSkipCancelKeyboard();
            sendMessageWithKeyboard(chatId, message, keyboard);

        } else {
            String errorMessage = """
                    ❌ Неверный формат номера телефона!
                    
                    📱 Пожалуйста, введите номер в формате:
                    +7-9**-***-**-**
                    
                    Например: +7-912-345-67-89
                    
                    Попробуйте еще раз:
                    """;
            sendSafeMessage(chatId, errorMessage);
        }
    }

    /**
     * Обработка ввода email
     */
    private void handleEmailInput(Long chatId, String email) {
        if (email.equals("Отмена")) {
            handleCancel(chatId);
            return;
        }

        if (email.equals("Пропустить")) {
            // Пропускаем email и переходим к адресу
            userStates.put(chatId, "CONTACT_AWAITING_ADDRESS");

            String message = """
                    📧 Email пропущен.
                    
                    🏠 Теперь введите ваш адрес (необязательно):
                    
                    Или нажмите "Завершить", чтобы сохранить контакты.
                    """;

            ReplyKeyboardMarkup keyboard = createFinishCancelKeyboard();
            sendMessageWithKeyboard(chatId, message, keyboard);
            return;
        }

        if (contactService.isValidEmail(email)) {
            // Сохраняем email и переходим к адресу
            String currentPhone = userRepository.findById(chatId)
                    .map(User::getPhoneNumber)
                    .orElse(null);
            contactService.saveUserContacts(chatId, currentPhone, email, null);
            userStates.put(chatId, "CONTACT_AWAITING_ADDRESS");

            String message = """
                    ✅ Email сохранен!
                    
                    🏠 Теперь введите ваш адрес (необязательно):
                    
                    Или нажмите "Завершить", чтобы сохранить контакты.
                    """;

            ReplyKeyboardMarkup keyboard = createFinishCancelKeyboard();
            sendMessageWithKeyboard(chatId, message, keyboard);

        } else {
            String errorMessage = """
                    ❌ Неверный формат email!
                    
                    📧 Пожалуйста, введите корректный email:
                    
                    Например: example@mail.ru
                    
                    Или нажмите "Пропустить", чтобы перейти к следующему шагу.
                    """;
            sendSafeMessage(chatId, errorMessage);
        }
    }

    /**
     * Обработка ввода адреса
     */
    private void handleAddressInput(Long chatId, String address) {
        if (address.equals("Отмена")) {
            handleCancel(chatId);
            return;
        }

        if (address.equals("Завершить")) {
            // Завершаем процесс регистрации
            completeContactRegistration(chatId);
            return;
        }

        // Сохраняем адрес и завершаем процесс
        String currentPhone = userRepository.findById(chatId)
                .map(User::getPhoneNumber)
                .orElse(null);
        String currentEmail = userRepository.findById(chatId)
                .map(User::getEmail)
                .orElse(null);

        if (contactService.saveUserContacts(chatId, currentPhone, currentEmail, address)) {
            completeContactRegistration(chatId);
        } else {
            sendSafeMessage(chatId, "❌ Произошла ошибка при сохранении адреса. Попробуйте еще раз.");
        }
    }

    /**
     * Завершение процесса регистрации контактов
     */
    private void completeContactRegistration(Long chatId) {
        userStates.remove(chatId);

        String contactsInfo = contactService.getUserContactsInfo(chatId);
        String successMessage = """
                ✅ Ваши контактные данные успешно сохранены!
                
                """ + contactsInfo + """
                
                📞 Волонтер свяжется с вами в ближайшее время для обсуждения деталей.
                
                Спасибо, что обратились в наш приют! 🐾
                """;

        // Возвращаем в соответствующее меню
        Optional<User> userOpt = userService.findByChatId(chatId);
        ShelterType shelterType = userOpt.map(User::getChosenShelter).orElse(null);

        if (shelterType != null) {
            sendMessageWithKeyboard(chatId, successMessage, createMainMenuKeyboard());
        } else {
            sendMessageWithKeyboard(chatId, successMessage, createShelterSelectionKeyboard());
        }
    }

    // ========== ФУНКЦИОНАЛ ОТЧЕТОВ О ПИТОМЦАХ ==========

    /**
     * Начинает процесс отправки отчета о питомце
     */
    private void startReportProcess(Long chatId) {
        // Проверяем, есть ли у пользователя активные усыновления
        List<Object> adoptions = Collections.singletonList(adoptionProcessService.getUserAdoptions(chatId));

        if (adoptions.isEmpty()) {
            sendSafeMessage(chatId, """
                    ❌ У вас нет активных усыновлений для отправки отчетов.
                    
                    Если вы считаете, что это ошибка, пожалуйста, свяжитесь с волонтером.
                    """);
            return;
        }

        // Начинаем процесс отправки отчета
        userStates.put(chatId, "REPORT_AWAITING_DIET");
        userTempData.put(chatId, new ConcurrentHashMap<>());

        String instruction = """
                📊 Ежедневный отчет о питомце
                
                Пожалуйста, заполните информацию о вашем питомце за сегодня.
                
                🍽️ Шаг 1: Опишите рацион питания питомца:
                - Что и сколько кушал питомец сегодня?
                - Были ли какие-то особенности в питании?
                """;

        ReplyKeyboardMarkup keyboard = createCancelKeyboard();
        sendMessageWithKeyboard(chatId, instruction, keyboard);
    }

    /**
     * Обрабатывает состояния отправки отчета
     */
    private void handleReportState(Long chatId, String messageText, String userState) {
        if (messageText.equals("Отмена")) {
            handleCancel(chatId);
            return;
        }

        switch (userState) {
            case "REPORT_AWAITING_DIET":
                handleDietInput(chatId, messageText);
                break;
            case "REPORT_AWAITING_HEALTH":
                handleHealthInput(chatId, messageText);
                break;
            case "REPORT_AWAITING_BEHAVIOR":
                handleBehaviorInput(chatId, messageText);
                break;
            case "REPORT_AWAITING_PHOTO":
                handlePhotoInput(chatId, messageText);
                break;
            default:
                userStates.remove(chatId);
                userTempData.remove(chatId);
                sendSafeMessage(chatId, "⚠️ Произошла ошибка. Пожалуйста, начните заново.");
        }
    }

    private void handleDietInput(Long chatId, String diet) {
        userTempData.get(chatId).put("diet", diet);
        userStates.put(chatId, "REPORT_AWAITING_HEALTH");

        String message = """
                ✅ Рацион питания сохранен!
                
                🏥 Шаг 2: Опишите общее самочувствие и привыкание к новому месту:
                - Как питомец себя чувствует?
                - Есть ли изменения в состоянии здоровья?
                - Как проходит адаптация?
                """;

        sendSafeMessage(chatId, message);
    }

    private void handleHealthInput(Long chatId, String health) {
        userTempData.get(chatId).put("health", health);
        userStates.put(chatId, "REPORT_AWAITING_BEHAVIOR");

        String message = """
                ✅ Состояние здоровья сохранено!
                
                🐕 Шаг 3: Опишите изменения в поведении:
                - Отказ от старых привычек?
                - Приобретение новых привычек?
                - Изменения в поведении с членами семьи?
                """;

        sendSafeMessage(chatId, message);
    }

    private void handleBehaviorInput(Long chatId, String behavior) {
        userTempData.get(chatId).put("behavior", behavior);
        userStates.put(chatId, "REPORT_AWAITING_PHOTO");

        String message = """
                ✅ Изменения в поведении сохранены!
                
                📷 Шаг 4: Пришлите фото питомца:
                - Сделайте четкое фото питомца
                - Желательно, чтобы питомец был в кадре полностью
                - Можно отправить несколько фото
                
                Или нажмите "Пропустить фото", если не можете отправить фото сейчас.
                """;

        ReplyKeyboardMarkup keyboard = createSkipPhotoKeyboard();
        sendMessageWithKeyboard(chatId, message, keyboard);
    }

    private void handlePhotoInput(Long chatId, String messageText) {
        if (messageText.equals("Пропустить фото")) {
            userTempData.get(chatId).put("photo", "Фото не приложено");
        } else {
            userTempData.get(chatId).put("photo", "Фото приложено (в разработке)");
        }

        completeReportProcess(chatId);
    }

    private void completeReportProcess(Long chatId) {
        Map<String, String> reportData = userTempData.get(chatId);

        // Сохраняем отчет в базу данных
        boolean success = reportService.submitDailyReport(
                chatId, // В реальной реализации нужно передать adoptionId
                reportData.get("diet"),
                reportData.get("health"),
                reportData.get("behavior"),
                reportData.get("photo")
        );

        userStates.remove(chatId);
        userTempData.remove(chatId);

        if (success) {
            String successMessage = """
                    ✅ Ежедневный отчет успешно отправлен!
                    
                    Благодарим вас за ответственность и заботу о питомце! 🐾
                    
                    Волонтеры проверят ваш отчет и при необходимости свяжутся с вами.
                    
                    Не забывайте отправлять отчет ежедневно до 21:00.
                    """;
            sendMessageWithKeyboard(chatId, successMessage, createMainMenuKeyboard());
        } else {
            String errorMessage = """
                    ❌ Не удалось сохранить отчет.
                    
                    Пожалуйста, попробуйте еще раз или свяжитесь с волонтером.
                    """;
            sendMessageWithKeyboard(chatId, errorMessage, createMainMenuKeyboard());
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Обработка отмены
     */
    private void handleCancel(Long chatId) {
        userStates.remove(chatId);
        userTempData.remove(chatId);
        sendSafeMessage(chatId, "❌ Операция отменена.");

        // Возвращаем в соответствующее меню
        Optional<User> userOpt = userService.findByChatId(chatId);
        ShelterType shelterType = userOpt.map(User::getChosenShelter).orElse(null);

        if (shelterType != null) {
            sendMessageWithKeyboard(chatId, "Возврат в главное меню:", createMainMenuKeyboard());
        } else {
            sendMessageWithKeyboard(chatId, "Пожалуйста, выберите приют:", createShelterSelectionKeyboard());
        }
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
        return command.equals("Список животных") ||
                command.equals("Правила знакомства") ||
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

    // ========== МЕТОДЫ СОЗДАНИЯ КЛАВИАТУР ==========

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
        row1.add("Список животных");
        row1.add("Правила знакомства");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Необходимые документы");
        row2.add("Рекомендации по транспортировке");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Обустройство дома для щенка/котенка");
        row3.add("Обустройство дома для взрослого животного");

        KeyboardRow row4 = new KeyboardRow();
        row4.add("Обустройство для животного-инвалида");
        row4.add("Советы кинолога");

        KeyboardRow row5 = new KeyboardRow();
        row5.add("Рекомендации кинологов");
        row5.add("Причины отказа");

        KeyboardRow row6 = new KeyboardRow();
        row6.add("Записать контакты");
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
     * Создает клавиатуру для отмены
     */
    private ReplyKeyboardMarkup createCancelKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Отмена");
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);

        return keyboardMarkup;
    }

    /**
     * Создает клавиатуру с пропуском и отменой
     */
    private ReplyKeyboardMarkup createSkipCancelKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Пропустить");
        KeyboardRow row2 = new KeyboardRow();
        row2.add("Отмена");
        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);

        return keyboardMarkup;
    }

    /**
     * Создает клавиатуру с завершением и отменой
     */
    private ReplyKeyboardMarkup createFinishCancelKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Завершить");
        KeyboardRow row2 = new KeyboardRow();
        row2.add("Отмена");
        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);

        return keyboardMarkup;
    }

    /**
     * Создает клавиатуру для пропуска фото
     */
    private ReplyKeyboardMarkup createSkipPhotoKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Пропустить фото");
        KeyboardRow row2 = new KeyboardRow();
        row2.add("Отмена");
        keyboard.add(row1);
        keyboard.add(row2);
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