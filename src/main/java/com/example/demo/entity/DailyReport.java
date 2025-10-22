package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "daily_reports")
@Data
public class DailyReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "adoption_id")
    private Adoption adoption;

    private LocalDateTime reportDate;
    private String photoPath;
    private String diet;
    private String healthCondition;
    private String behaviorChanges;
    private Boolean isReviewed = false;
    private String volunteerFeedback;
}
