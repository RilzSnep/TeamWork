// ReportService.java
package com.example.demo.service;

import com.example.demo.entity.DailyReport;
import com.example.demo.entity.Adoption;
import com.example.demo.repository.DailyReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ReportService {

    private final DailyReportRepository reportRepository;

    @Autowired
    public ReportService(DailyReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public boolean submitDailyReport(Long adoptionId, String diet, String healthCondition,
                                     String behaviorChanges, String photoPath) {
        try {
            // Здесь должна быть логика получения Adoption по ID
            // Пока заглушка для демонстрации
            DailyReport report = new DailyReport();
            report.setReportDate(LocalDateTime.now());
            report.setDiet(diet);
            report.setHealthCondition(healthCondition);
            report.setBehaviorChanges(behaviorChanges);
            report.setPhotoPath(photoPath);
            report.setIsReviewed(false);

            reportRepository.save(report);
            log.info("Отчет сохранен для усыновления: {}", adoptionId);
            return true;

        } catch (Exception e) {
            log.error("Ошибка при сохранении отчета: {}", e.getMessage());
            return false;
        }
    }

    public List<DailyReport> getUnreviewedReports() {
        return reportRepository.findByIsReviewedFalse();
    }

    public void markReportAsReviewed(Long reportId, String feedback) {
        reportRepository.findById(reportId).ifPresent(report -> {
            report.setIsReviewed(true);
            report.setVolunteerFeedback(feedback);
            reportRepository.save(report);
        });
    }
}