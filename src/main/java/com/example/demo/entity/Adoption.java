package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "adoptions")
@Data
public class Adoption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "animal_id")
    private Animal animal;

    private LocalDateTime adoptionDate;
    private LocalDateTime trialEndDate;

    @Enumerated(EnumType.STRING)
    private AdoptionStatus status; // TRIAL, SUCCESS, EXTENDED, FAILED

    private Integer extendedDays;
}
