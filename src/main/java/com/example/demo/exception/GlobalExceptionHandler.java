package com.example.demo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Глобальный обработчик исключений для всего приложения
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UserNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String handleUserNotFoundException(UserNotFoundException e) {
    log.warn("Пользователь не найден: {}", e.getMessage());
    return "❌ Пользователь не найден. Пожалуйста, начните с команды /start";
  }

  @ExceptionHandler(ShelterNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String handleShelterNotFoundException(ShelterNotFoundException e) {
    log.warn("Приют не найден: {}", e.getMessage());
    return "❌ Информация о приюте временно недоступна";
  }

  @ExceptionHandler(CustomException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public String handleCustomException(CustomException e) {
    log.error("Кастомная ошибка приложения: {}", e.getMessage(), e);
    return "⚠️ Произошла ошибка в приложении. Пожалуйста, попробуйте позже или позовите волонтера.";
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public String handleGenericException(Exception e) {
    log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);
    return "⚠️ Произошла внутренняя ошибка. Пожалуйста, попробуйте позже или позовите волонтера.";
  }
}