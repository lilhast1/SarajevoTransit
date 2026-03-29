package com.sarajevotransit.feedbackservice.controller;

import com.sarajevotransit.feedbackservice.dto.CreateProblemReportRequest;
import com.sarajevotransit.feedbackservice.dto.ProblemReportResponse;
import com.sarajevotransit.feedbackservice.dto.ReportStatusUpdateRequest;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import com.sarajevotransit.feedbackservice.service.ProblemReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ProblemReportController {

    private final ProblemReportService problemReportService;

    public ProblemReportController(ProblemReportService problemReportService) {
        this.problemReportService = problemReportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProblemReportResponse createReport(@Valid @RequestBody CreateProblemReportRequest request) {
        return problemReportService.createReport(request);
    }

    @GetMapping
    public List<ProblemReportResponse> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Long reporterUserId) {
        return problemReportService.getReports(status, reporterUserId);
    }

    @GetMapping("/{id}")
    public ProblemReportResponse getReport(@PathVariable Long id) {
        return problemReportService.getReport(id);
    }

    @PatchMapping("/{id}/status")
    public ProblemReportResponse updateStatus(@PathVariable Long id,
            @Valid @RequestBody ReportStatusUpdateRequest request) {
        return problemReportService.updateStatus(id, request.getStatus());
    }
}
