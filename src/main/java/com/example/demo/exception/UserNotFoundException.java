package com.example.demo.exception;

/**
 * Исключение, выбрасываемое когда пользователь не найден
 */
public class UserNotFoundException extends CustomException {
  public UserNotFoundException(Long chatId) {
    super("Пользователь с chatId " + chatId + " не найден");
  }
}