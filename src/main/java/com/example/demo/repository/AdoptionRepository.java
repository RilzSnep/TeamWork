package com.example.demo.repository;

import com.example.demo.entity.Adoption;
import com.example.demo.entity.AdoptionStatus;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdoptionRepository extends JpaRepository<Adoption, Long> {

    // Используем @Query для явного указания полей
    @Query("SELECT a FROM Adoption a WHERE a.user.chatId = :userId AND a.animal.id = :animalId")
    Optional<Adoption> findByUserIdAndAnimalId(@Param("userId") Long userId, @Param("animalId") Long animalId);

    // Исправляем другие методы
    @Query("SELECT a FROM Adoption a WHERE a.user.chatId = :userId AND a.status = :status")
    List<Adoption> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") AdoptionStatus status);

    List<Adoption> findByTrialEndDateBeforeAndStatus(LocalDateTime date, AdoptionStatus status);

    // Добавляем метод для поиска по пользователю
    @Query("SELECT a FROM Adoption a WHERE a.user.chatId = :userId")
    List<Adoption> findByUserId(@Param("userId") Long userId);
}