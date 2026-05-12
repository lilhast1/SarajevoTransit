package com.sarajevotransit.feedbackservice.controller;

import tools.jackson.databind.JsonNode;
import com.sarajevotransit.feedbackservice.dto.BatchCreateProblemReportsRequest;
import com.sarajevotransit.feedbackservice.dto.BatchCreateProblemReportsResponse;
import com.sarajevotransit.feedbackservice.dto.CreateProblemReportRequest;
import com.sarajevotransit.feedbackservice.dto.LineReportCountResponse;
import com.sarajevotransit.feedbackservice.dto.ProblemReportResponse;
import com.sarajevotransit.feedbackservice.dto.ReportStatusUpdateRequest;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import com.sarajevotransit.feedbackservice.service.ProblemReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ProblemReportController {

    private final ProblemReportService problemReportService;

    @PostMapping
    public ResponseEntity<ProblemReportResponse> createReport(
            @Valid @RequestBody CreateProblemReportRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        ProblemReportResponse created = problemReportService.createReport(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchCreateProblemReportsResponse> createReportsBatch(
            @Valid @RequestBody BatchCreateProblemReportsRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        BatchCreateProblemReportsResponse created = problemReportService.createReportsBatch(request.getReports());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public Page<ProblemReportResponse> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Long reporterUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        return problemReportService.getReports(status, reporterUserId, pageable);
    }

    @GetMapping("/search")
    public Page<ProblemReportResponse> searchReports(
            @RequestParam("q") String keyword,
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        return problemReportService.searchReports(keyword, status, pageable);
    }

    @PatchMapping(path = "/{id}", consumes = "application/json-patch+json")
    public ProblemReportResponse patchReport(
            @PathVariable @Positive Long id,
            @RequestBody JsonNode patch) {
        // ADMIN-only: enforced at gateway level (PATCH /api/v1/reports/**)
        return problemReportService.patchReport(id, patch);
    }

    @GetMapping("/{id}")
    public ProblemReportResponse getReport(
            @PathVariable @Positive Long id,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        return problemReportService.getReport(id);
    }

    @GetMapping("/line/{lineId}")
    public Page<ProblemReportResponse> getReportsByLineId(
            @PathVariable @Positive Long lineId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        return problemReportService.getReportsByLineId(lineId, pageable);
    }

    @GetMapping("/line/{lineId}/count")
    @Operation(security = {})
    public LineReportCountResponse countReportsByLine(@PathVariable @Positive Long lineId) {
        return problemReportService.countReportsByLine(lineId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable @Positive Long id) {
        problemReportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ProblemReportResponse updateStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ReportStatusUpdateRequest request) {
        return problemReportService.updateStatus(id, request.getStatus());
    }
}
