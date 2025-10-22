// ReportController.java
package com.example.demo.controller;

import com.example.demo.entity.DailyReport;
import com.example.demo.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/unreviewed")
    public ResponseEntity<List<DailyReport>> getUnreviewedReports() {
        try {
            List<DailyReport> reports = reportService.getUnreviewedReports();
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("Ошибка при получении непросмотренных отчетов: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{reportId}/review")
    public ResponseEntity<String> markReportAsReviewed(
            @PathVariable Long reportId,
            @RequestBody Map<String, String> request) {

        try {
            String feedback = request.get("feedback");
            reportService.markReportAsReviewed(reportId, feedback);
            return ResponseEntity.ok("Отчет помечен как просмотренный");
        } catch (Exception e) {
            log.error("Ошибка при обновлении отчета: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<DailyReport> getReportById(@PathVariable Long reportId) {
        try {
            // Логика получения отчета по ID
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Ошибка при получении отчета: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}