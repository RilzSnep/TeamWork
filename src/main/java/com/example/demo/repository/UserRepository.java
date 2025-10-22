package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с сущностью {@link User}.
 * Предоставляет базовые CRUD-операции и методы для поиска пользователей.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
// На данном этапе используем стандартные методы JpaRepository:
// save(), findById(), findAll(), deleteById() и т.д.
}