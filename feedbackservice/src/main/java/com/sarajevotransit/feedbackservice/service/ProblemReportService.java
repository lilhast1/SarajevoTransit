package com.sarajevotransit.feedbackservice.service;

import com.sarajevotransit.feedbackservice.dto.CreateProblemReportRequest;
import com.sarajevotransit.feedbackservice.dto.ProblemReportResponse;
import com.sarajevotransit.feedbackservice.exception.BadRequestException;
import com.sarajevotransit.feedbackservice.exception.NotFoundException;
import com.sarajevotransit.feedbackservice.mapper.ProblemReportMapper;
import com.sarajevotransit.feedbackservice.model.ProblemReport;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import com.sarajevotransit.feedbackservice.repository.ProblemReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class ProblemReportService {

    private final ProblemReportRepository problemReportRepository;
    private final ProblemReportMapper problemReportMapper;

    public ProblemReportService(ProblemReportRepository problemReportRepository,
            ProblemReportMapper problemReportMapper) {
        this.problemReportRepository = problemReportRepository;
        this.problemReportMapper = problemReportMapper;
    }

    @Transactional
    public ProblemReportResponse createReport(CreateProblemReportRequest request) {
        boolean hasVehicleReference = request.getVehicleId() != null
                || StringUtils.hasText(request.getVehicleRegistrationNumber())
                || StringUtils.hasText(request.getVehicleInternalId());
        if (!hasVehicleReference && request.getStationId() == null) {
            throw new BadRequestException(
                    "At least one of vehicleId/vehicleRegistrationNumber/vehicleInternalId or stationId must be provided.");
        }

        ProblemReport entity = problemReportMapper.toEntity(request);
        entity.setVehicleRegistrationNumber(trimToNull(entity.getVehicleRegistrationNumber()));
        entity.setVehicleInternalId(trimToNull(entity.getVehicleInternalId()));
        entity.setVehicleType(normalizeVehicleType(entity.getVehicleType()));
        entity.setDescription(request.getDescription().trim());
        entity.setPhotoUrls(request.getPhotoUrls());
        entity.setStatus(ReportStatus.RECEIVED);

        ProblemReport saved = problemReportRepository.save(entity);
        return problemReportMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ProblemReportResponse> getReports(ReportStatus status, Long reporterUserId, Pageable pageable) {
        Page<ProblemReport> reports;
        if (status != null && reporterUserId != null) {
            reports = problemReportRepository.findByStatusAndReporterUserId(status, reporterUserId, pageable);
        } else if (status != null) {
            reports = problemReportRepository.findByStatus(status, pageable);
        } else if (reporterUserId != null) {
            reports = problemReportRepository.findByReporterUserId(reporterUserId, pageable);
        } else {
            reports = problemReportRepository.findAll(pageable);
        }
        return reports.map(problemReportMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProblemReportResponse getReport(Long id) {
        ProblemReport report = problemReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Problem report not found: id=" + id));
        return problemReportMapper.toResponse(report);
    }

    @Transactional(readOnly = true)
    public Page<ProblemReportResponse> getReportsByLineId(Long lineId, Pageable pageable) {
        return problemReportRepository.findByLineId(lineId, pageable)
                .map(problemReportMapper::toResponse);
    }

    @Transactional
    public ProblemReportResponse updateStatus(Long id, ReportStatus status) {
        ProblemReport report = problemReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Problem report not found: id=" + id));
        report.setStatus(status);
        ProblemReport saved = problemReportRepository.save(report);
        return problemReportMapper.toResponse(saved);
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
