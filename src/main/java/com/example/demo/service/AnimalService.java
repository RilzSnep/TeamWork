package com.example.demo.service;

import com.example.demo.entity.Animal;
import com.example.demo.entity.AnimalStatus;
import com.example.demo.entity.AnimalType;
import com.example.demo.entity.ShelterType;
import com.example.demo.repository.AnimalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
public class AnimalService {

    private final AnimalRepository animalRepository;

    @Autowired
    public AnimalService(AnimalRepository animalRepository) {
        this.animalRepository = animalRepository;
    }

    public List<Animal> getAvailableAnimalsByShelterType(ShelterType shelterType) {
        try {
            AnimalType animalType = shelterType == ShelterType.CAT ? AnimalType.CAT : AnimalType.DOG;
            return animalRepository.findByTypeAndStatus(animalType, AnimalStatus.AVAILABLE);
        } catch (Exception e) {
            log.error("Ошибка при получении доступных животных для приюта {}: {}", shelterType, e.getMessage());
            return List.of();
        }
    }

    public String getAvailableAnimalsList(ShelterType shelterType) {
        List<Animal> animals = getAvailableAnimalsByShelterType(shelterType);

        if (animals.isEmpty()) {
            return "🐾 В настоящее время нет доступных животных в этом приюте.\n\nПожалуйста, проверьте позже или свяжитесь с волонтером для уточнения информации.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🐾 Доступные животные для усыновления:\n\n");

        for (int i = 0; i < animals.size(); i++) {
            Animal animal = animals.get(i);
            sb.append(String.format("%d. %s", i + 1, animal.getName()));

            if (animal.getAge() != null) {
                sb.append(String.format(", %d %s", animal.getAge(), getAgeSuffix(animal.getAge())));
            }

            if (animal.getBreed() != null && !animal.getBreed().isEmpty()) {
                sb.append(String.format(", порода: %s", animal.getBreed()));
            }

            sb.append("\n");

            if (animal.getDescription() != null && !animal.getDescription().isEmpty()) {
                sb.append(String.format("   Описание: %s\n", animal.getDescription()));
            }

            sb.append("\n");
        }

        sb.append("Для получения более подробной информации или начала процесса усыновления, пожалуйста, свяжитесь с волонтером.");

        return sb.toString();
    }

    private String getAgeSuffix(int age) {
        if (age % 10 == 1 && age % 100 != 11) {
            return "год";
        } else if (age % 10 >= 2 && age % 10 <= 4 && (age % 100 < 10 || age % 100 >= 20)) {
            return "года";
        } else {
            return "лет";
        }
    }

    // Метод для инициализации тестовых данных
    public void initializeTestData() {
        if (animalRepository.count() == 0) {
            log.info("Инициализация тестовых данных о животных...");

            // Тестовые данные для кошек
            Animal cat1 = new Animal();
            cat1.setName("Мурзик");
            cat1.setAge(2);
            cat1.setBreed("Британский");
            cat1.setType(AnimalType.CAT);
            cat1.setStatus(AnimalStatus.AVAILABLE);
            cat1.setDescription("Ласковый и игривый котенок, приучен к лотку");
            animalRepository.save(cat1);

            Animal cat2 = new Animal();
            cat2.setName("Васька");
            cat2.setAge(4);
            cat2.setBreed("Дворовый");
            cat2.setType(AnimalType.CAT);
            cat2.setStatus(AnimalStatus.AVAILABLE);
            cat2.setDescription("Спокойный и независимый кот");
            animalRepository.save(cat2);

            // Тестовые данные для собак
            Animal dog1 = new Animal();
            dog1.setName("Бобик");
            dog1.setAge(3);
            dog1.setBreed("Лабрадор");
            dog1.setType(AnimalType.DOG);
            dog1.setStatus(AnimalStatus.AVAILABLE);
            dog1.setDescription("Дружелюбная и активная собака, любит детей");
            animalRepository.save(dog1);

            Animal dog2 = new Animal();
            dog2.setName("Шарик");
            dog2.setAge(5);
            dog2.setBreed("Овчарка");
            dog2.setType(AnimalType.DOG);
            dog2.setStatus(AnimalStatus.AVAILABLE);
            dog2.setDescription("Умная и преданная собака, нуждается в активных прогулках");
            animalRepository.save(dog2);

            log.info("Тестовые данные о животных успешно созданы");
        }
    }
}