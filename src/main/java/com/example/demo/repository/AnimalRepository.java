package com.example.demo.repository;

import com.example.demo.entity.Animal;
import com.example.demo.entity.AnimalStatus;
import com.example.demo.entity.AnimalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {
    List<Animal> findByTypeAndStatus(AnimalType type, AnimalStatus status);
    List<Animal> findByStatus(AnimalStatus status);

    // Добавляем метод для поиска по типу
    List<Animal> findByType(AnimalType type);
}