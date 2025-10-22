// Animal.java
package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "animals")
@Data
public class Animal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer age;
    private String breed;

    @Enumerated(EnumType.STRING)
    private AnimalType type; // CAT, DOG

    @Enumerated(EnumType.STRING)
    private AnimalStatus status; // AVAILABLE, ADOPTED, RESERVED

    private String description;
    private LocalDateTime createdAt;
}



