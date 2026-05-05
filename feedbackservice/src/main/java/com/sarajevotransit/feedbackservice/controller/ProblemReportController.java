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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ProblemReportController {

    private final ProblemReportService problemReportService;

    @PostMapping
    public ResponseEntity<ProblemReportResponse> createReport(
            @Valid @RequestBody CreateProblemReportRequest request,
            HttpServletRequest httpRequest) {
        // Override reporterUserId from gateway header
        request.setReporterUserId(extractUserId(httpRequest));
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
            HttpServletRequest httpRequest) {
        Long requestingUserId = extractUserId(httpRequest);
        // Override reporterUserId on every report in the batch
        request.getReports().forEach(r -> r.setReporterUserId(requestingUserId));
        BatchCreateProblemReportsResponse created = problemReportService.createReportsBatch(request.getReports());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public Page<ProblemReportResponse> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Long reporterUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        // ADMIN sees all; passenger can only query their own reports
        String role = httpRequest.getHeader("X-User-Role");
        if (!"ADMIN".equals(role)) {
            Long requestingUserId = extractUserId(httpRequest);
            reporterUserId = requestingUserId;
        }
        return problemReportService.getReports(status, reporterUserId, pageable);
    }

    @GetMapping("/search")
    public Page<ProblemReportResponse> searchReports(
            @RequestParam("q") String keyword,
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
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
            HttpServletRequest httpRequest) {
        ProblemReportResponse report = problemReportService.getReport(id);
        requireOwnerOrAdmin(httpRequest, report.getReporterUserId());
        return report;
    }

    @GetMapping("/line/{lineId}")
    public Page<ProblemReportResponse> getReportsByLineId(
            @PathVariable @Positive Long lineId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return problemReportService.getReportsByLineId(lineId, pageable);
    }

    @GetMapping("/line/{lineId}/count")
    public LineReportCountResponse countReportsByLine(@PathVariable @Positive Long lineId) {
        return problemReportService.countReportsByLine(lineId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable @Positive Long id) {
        // ADMIN-only: enforced at gateway level (DELETE /api/v1/reports/**)
        problemReportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ProblemReportResponse updateStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ReportStatusUpdateRequest request) {
        // ADMIN-only: enforced at gateway level (PATCH /api/v1/reports/**)
        return problemReportService.updateStatus(id, request.getStatus());
    }

    private void requireOwnerOrAdmin(HttpServletRequest request, Long resourceUserId) {
        String role = request.getHeader("X-User-Role");
        if ("ADMIN".equals(role)) return;
        String requestingUserId = request.getHeader("X-User-Id");
        if (requestingUserId == null || !requestingUserId.equals(String.valueOf(resourceUserId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private void requireAdmin(HttpServletRequest request) {
        if (!"ADMIN".equals(request.getHeader("X-User-Role"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private Long extractUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user identity");
        }
        return Long.parseLong(userId);
    }
}
