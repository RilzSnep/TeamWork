package com.example.demo.repository;

import com.example.demo.entity.Adoption;
import com.example.demo.entity.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {
    List<DailyReport> findByAdoptionAndReportDateBetween(Adoption adoption, LocalDateTime start, LocalDateTime end);
    List<DailyReport> findByIsReviewedFalse();
    List<DailyReport> findByAdoption(Adoption adoption);
}
