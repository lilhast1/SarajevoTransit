package com.sarajevotransit.feedbackservice.service;

import com.sarajevotransit.feedbackservice.dto.CreateProblemReportRequest;
import com.sarajevotransit.feedbackservice.dto.ProblemReportResponse;
import com.sarajevotransit.feedbackservice.exception.BadRequestException;
import com.sarajevotransit.feedbackservice.exception.NotFoundException;
import com.sarajevotransit.feedbackservice.model.ProblemReport;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import com.sarajevotransit.feedbackservice.repository.ProblemReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
public class ProblemReportService {

    private final ProblemReportRepository problemReportRepository;

    public ProblemReportService(ProblemReportRepository problemReportRepository) {
        this.problemReportRepository = problemReportRepository;
    }

    public ProblemReportResponse createReport(CreateProblemReportRequest request) {
        boolean hasVehicleReference = request.getVehicleId() != null
                || StringUtils.hasText(request.getVehicleRegistrationNumber())
                || StringUtils.hasText(request.getVehicleInternalId());
        if (!hasVehicleReference && request.getStationId() == null) {
            throw new BadRequestException(
                    "At least one of vehicleId/vehicleRegistrationNumber/vehicleInternalId or stationId must be provided.");
        }

        ProblemReport entity = new ProblemReport();
        entity.setReporterUserId(request.getReporterUserId());
        entity.setLineId(request.getLineId());
        entity.setVehicleId(request.getVehicleId());
        entity.setVehicleRegistrationNumber(trimToNull(request.getVehicleRegistrationNumber()));
        entity.setVehicleInternalId(trimToNull(request.getVehicleInternalId()));
        entity.setVehicleType(normalizeVehicleType(request.getVehicleType()));
        entity.setStationId(request.getStationId());
        entity.setCategory(request.getCategory());
        entity.setDescription(request.getDescription().trim());
        entity.setPhotoUrls(request.getPhotoUrls());
        entity.setStatus(ReportStatus.RECEIVED);

        ProblemReport saved = problemReportRepository.save(entity);
        return toResponse(saved);
    }

    public List<ProblemReportResponse> getReports(ReportStatus status, Long reporterUserId) {
        List<ProblemReport> reports;
        if (status != null && reporterUserId != null) {
            reports = problemReportRepository.findByStatusAndReporterUserIdOrderByCreatedAtDesc(status, reporterUserId);
        } else if (status != null) {
            reports = problemReportRepository.findByStatusOrderByCreatedAtDesc(status);
        } else if (reporterUserId != null) {
            reports = problemReportRepository.findByReporterUserIdOrderByCreatedAtDesc(reporterUserId);
        } else {
            reports = problemReportRepository.findAllByOrderByCreatedAtDesc();
        }
        return reports.stream().map(this::toResponse).toList();
    }

    public ProblemReportResponse getReport(Long id) {
        ProblemReport report = problemReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Problem report not found: id=" + id));
        return toResponse(report);
    }

    public ProblemReportResponse updateStatus(Long id, ReportStatus status) {
        ProblemReport report = problemReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Problem report not found: id=" + id));
        report.setStatus(status);
        ProblemReport saved = problemReportRepository.save(report);
        return toResponse(saved);
    }

    private ProblemReportResponse toResponse(ProblemReport entity) {
        ProblemReportResponse response = new ProblemReportResponse();
        response.setId(entity.getId());
        response.setReporterUserId(entity.getReporterUserId());
        response.setLineId(entity.getLineId());
        response.setVehicleId(entity.getVehicleId());
        response.setVehicleRegistrationNumber(entity.getVehicleRegistrationNumber());
        response.setVehicleInternalId(entity.getVehicleInternalId());
        response.setVehicleType(entity.getVehicleType());
        response.setStationId(entity.getStationId());
        response.setCategory(entity.getCategory());
        response.setDescription(entity.getDescription());
        response.setPhotoUrls(entity.getPhotoUrls());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeVehicleType(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }

        String upper = normalized.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "BUS", "TRAM", "TROLLEY", "MINIBUS" -> upper;
            default -> throw new BadRequestException(
                    "vehicleType must be one of BUS, TRAM, TROLLEY, MINIBUS.");
        };
    }
}
