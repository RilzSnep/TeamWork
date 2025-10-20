package com.example.demo.exception;

/**
 * Исключение, выбрасываемое когда приют не найден
 */
public class ShelterNotFoundException extends CustomException {
  public ShelterNotFoundException(String message) {
    super(message);
  }
}