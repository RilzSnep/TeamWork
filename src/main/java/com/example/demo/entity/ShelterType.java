package com.example.demo.entity;


/**
 * Enum для представления типов приютов.
 * Определяет, с каким приютом работает пользователь.
 */
public enum ShelterType {
    CAT("Приют для кошек"),
    DOG("Приют для собак");

    private final String description;

    ShelterType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}