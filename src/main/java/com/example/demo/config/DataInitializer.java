// DataInitializer.java
package com.example.demo.config;

import com.example.demo.service.AnimalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final AnimalService animalService;

    @Autowired
    public DataInitializer(AnimalService animalService) {
        this.animalService = animalService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Инициализация данных приложения...");
        animalService.initializeTestData();
        log.info("Инициализация данных завершена");
    }
}