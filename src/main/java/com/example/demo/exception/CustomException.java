package com.example.demo.exception;

/**
 * Базовое кастомное исключение для приложения
 */
public class CustomException extends RuntimeException {
    public CustomException(String message) {
        super(message);
    }

    public CustomException(String message, Throwable cause) {
        super(message, cause);
    }
}