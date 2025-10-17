package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Сервис для предоставления информации о приютах.
 * Содержит статические данные о приютах для кошек и собак.
 */
@Slf4j
@Service
public class ShelterInfoService {

    private final Map<String, String> catShelterInfo;
    private final Map<String, String> dogShelterInfo;

    public ShelterInfoService() {
        // Инициализация информации о приюте для кошек
        catShelterInfo = new HashMap<>();
        catShelterInfo.put("about", """
                🐱 Приют для кошек "Мурчащий уголок" 🐱
                
                Наш приют существует с 2015 года и заботится о бездомных кошках.
                Мы предоставляем:
                • Временный дом для 50+ кошек
                • Ветеринарное обслуживание
                • Социализацию и подготовку к adoption
                • Консультации для новых хозяев
                """);

        catShelterInfo.put("schedule", """
                🕒 Расписание работы приюта для кошек:
                
                Понедельник-Пятница: 10:00 - 19:00
                Суббота: 11:00 - 17:00
                Воскресенье: Выходной
                
                Адрес: г. Москва, ул. Кошачья, д. 15
                """);

        catShelterInfo.put("security_contact", """
                📞 Контакты охраны для пропуска:
                
                Телефон: +7 (495) 123-45-67
                Имя: Александр (начальник охраны)
                
                Заявку на пропск необходимо оформлять за 24 часа.
                """);

        catShelterInfo.put("safety_rules", """
                ⚠️ Правила безопасности в приюте для кошек:
                
                1. Не берите животных на руки без разрешения
                2. Не кормите животных принесенной едой
                3. Дети до 12 лет только в сопровождении взрослых
                4. Закрывайте за собой все двери
                5. Используйте антисептик для рук
                """);

        // Инициализация информации о приюте для собак
        dogShelterInfo = new HashMap<>();
        dogShelterInfo.put("about", """
                🐶 Приют для собак "Верный друг" 🐶
                
                Наш приют помогает бездомным собакам с 2010 года.
                Наши возможности:
                • Помещение для 80+ собак
                • Профессиональные кинологи
                • Полный ветеринарный контроль
                • Программы адаптации и тренировки
                """);

        dogShelterInfo.put("schedule", """
                🕒 Расписание работы приюта для собак:
                
                Понедельник-Пятница: 09:00 - 20:00
                Суббота-Воскресенье: 10:00 - 18:00
                
                Адрес: г. Москва, ул. Собачья, д. 28
                """);

        dogShelterInfo.put("security_contact", """
                📞 Контакты охраны для пропуска:
                
                Телефон: +7 (495) 765-43-21
                Имя: Сергей (начальник охраны)
                
                Пропуск оформляется минимум за 2 часа до визита.
                """);

        dogShelterInfo.put("safety_rules", """
                ⚠️ Правила безопасности в приюте для собак:
                
                1. Не заходите в вольеры без сотрудника
                2. Не бегайте и не кричите на территории
                3. Дети до 14 лет только под присмотром взрослых
                4. Соблюдайте дистанцию с незнакомыми собаками
                5. Сообщайте сотрудникам о любых проблемах
                """);
    }

    /**
     * Получает информацию о приюте по типу и категории
     */
    public String getShelterInfo(com.example.demo.entity.ShelterType shelterType, String infoType) {
        log.info("Запрос информации: {} - {}", shelterType, infoType);

        Map<String, String> shelterData = shelterType == com.example.demo.entity.ShelterType.CAT ?
                catShelterInfo : dogShelterInfo;

        return shelterData.getOrDefault(infoType, "Информация временно недоступна.");
    }

    /**
     * Получает контактные данные охраны
     */
    public String getSecurityContact(com.example.demo.entity.ShelterType shelterType) {
        return getShelterInfo(shelterType, "security_contact");
    }

    /**
     * Получает расписание работы
     */
    public String getSchedule(com.example.demo.entity.ShelterType shelterType) {
        return getShelterInfo(shelterType, "schedule");
    }

    /**
     * Получает правила безопасности
     */
    public String getSafetyRules(com.example.demo.entity.ShelterType shelterType) {
        return getShelterInfo(shelterType, "safety_rules");
    }

    /**
     * Получает общую информацию о приюте
     */
    public String getAbout(com.example.demo.entity.ShelterType shelterType) {
        return getShelterInfo(shelterType, "about");
    }
}