package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.AdoptionRepository;
import com.example.demo.repository.AnimalRepository;
import com.example.demo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AdoptionProcessService {

    private final AdoptionRepository adoptionRepository;
    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;

    @Autowired
    public AdoptionProcessService(AdoptionRepository adoptionRepository,
                                  AnimalRepository animalRepository,
                                  UserRepository userRepository) {
        this.adoptionRepository = adoptionRepository;
        this.animalRepository = animalRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public boolean startAdoptionProcess(Long userId, Long animalId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            Optional<Animal> animalOpt = animalRepository.findById(animalId);

            if (userOpt.isEmpty() || animalOpt.isEmpty()) {
                log.warn("Пользователь {} или животное {} не найдены", userId, animalId);
                return false;
            }

            User user = userOpt.get();
            Animal animal = animalOpt.get();

            // Проверяем, не начат ли уже процесс усыновления
            Optional<Adoption> existingAdoption = adoptionRepository.findByUserIdAndAnimalId(userId, animalId);
            if (existingAdoption.isPresent()) {
                log.warn("Процесс усыновления уже начат для пользователя {} и животного {}", userId, animalId);
                return false;
            }

            // Проверяем, доступно ли животное
            if (animal.getStatus() != AnimalStatus.AVAILABLE) {
                log.warn("Животное {} недоступно для усыновления. Статус: {}", animalId, animal.getStatus());
                return false;
            }

            Adoption adoption = new Adoption();
            adoption.setUser(user);
            adoption.setAnimal(animal);
            adoption.setAdoptionDate(LocalDateTime.now());
            adoption.setTrialEndDate(LocalDateTime.now().plusDays(30));
            adoption.setStatus(AdoptionStatus.TRIAL);

            // Меняем статус животного
            animal.setStatus(AnimalStatus.RESERVED);
            animal.setCreatedAt(LocalDateTime.now());

            adoptionRepository.save(adoption);
            animalRepository.save(animal);

            log.info("Начат процесс усыновления: пользователь {} -> животное {}", userId, animalId);
            return true;

        } catch (Exception e) {
            log.error("Ошибка при начале процесса усыновления: {}", e.getMessage(), e);
            return false;
        }
    }

    public List<Adoption> getUserAdoptions(Long userId) {
        try {
            return adoptionRepository.findByUserIdAndStatus(userId, AdoptionStatus.TRIAL);
        } catch (Exception e) {
            log.error("Ошибка при получении усыновлений пользователя {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    public List<Adoption> getAllUserAdoptions(Long userId) {
        try {
            return adoptionRepository.findByUserId(userId);
        } catch (Exception e) {
            log.error("Ошибка при получении всех усыновлений пользователя {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    @Transactional
    public boolean completeAdoption(Long adoptionId, boolean success) {
        try {
            Optional<Adoption> adoptionOpt = adoptionRepository.findById(adoptionId);
            if (adoptionOpt.isEmpty()) {
                return false;
            }

            Adoption adoption = adoptionOpt.get();
            Animal animal = adoption.getAnimal();

            if (success) {
                adoption.setStatus(AdoptionStatus.SUCCESS);
                animal.setStatus(AnimalStatus.ADOPTED);
            } else {
                adoption.setStatus(AdoptionStatus.FAILED);
                animal.setStatus(AnimalStatus.AVAILABLE);
            }

            adoptionRepository.save(adoption);
            animalRepository.save(animal);

            log.info("Процесс усыновления {} завершен со статусом: {}", adoptionId,
                    success ? "УСПЕХ" : "НЕУДАЧА");
            return true;

        } catch (Exception e) {
            log.error("Ошибка при завершении процесса усыновления {}: {}", adoptionId, e.getMessage());
            return false;
        }
    }
}