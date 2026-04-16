package com.sarajevotransit.feedbackservice.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.sarajevotransit.feedbackservice.dto.BatchCreateProblemReportsResponse;
import com.sarajevotransit.feedbackservice.dto.CreateProblemReportRequest;
import com.sarajevotransit.feedbackservice.dto.LineReportCountResponse;
import com.sarajevotransit.feedbackservice.dto.ProblemReportResponse;
import com.sarajevotransit.feedbackservice.dto.ProblemReportPatchRequest;
import com.sarajevotransit.feedbackservice.exception.BadRequestException;
import com.sarajevotransit.feedbackservice.exception.NotFoundException;
import com.sarajevotransit.feedbackservice.mapper.ProblemReportMapper;
import com.sarajevotransit.feedbackservice.model.ProblemReport;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import com.sarajevotransit.feedbackservice.repository.ProblemReportRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProblemReportService {

    private final ProblemReportRepository problemReportRepository;
    private final ProblemReportMapper problemReportMapper;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Transactional
    public ProblemReportResponse createReport(CreateProblemReportRequest request) {
        ProblemReport entity = buildEntityForCreate(request);
        ProblemReport saved = problemReportRepository.save(entity);
        return problemReportMapper.toResponse(saved);
    }

    @Transactional
    public BatchCreateProblemReportsResponse createReportsBatch(List<CreateProblemReportRequest> requests) {
        List<ProblemReport> entities = requests.stream()
                .map(this::buildEntityForCreate)
                .toList();

        List<ProblemReport> savedEntities = problemReportRepository.saveAll(entities);
        List<ProblemReportResponse> responses = savedEntities.stream()
                .map(problemReportMapper::toResponse)
                .toList();

        return new BatchCreateProblemReportsResponse(responses.size(), responses);
    }

    @Transactional
    public ProblemReportResponse patchReport(Long id, JsonNode patchDocument) {
        ProblemReport report = findReportOrThrow(id);

        ProblemReportPatchRequest currentRequest = mapToPatchRequest(report);
        ProblemReportPatchRequest patchedRequest = applyJsonPatchToRequest(currentRequest, patchDocument);
        validatePatchedRequest(patchedRequest);

        applyPatchResultToEntity(report, patchedRequest);
        applyBusinessRules(report);

        ProblemReport saved = problemReportRepository.save(report);
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
    public Page<ProblemReportResponse> searchReports(String keyword, ReportStatus status, Pageable pageable) {
        if (!StringUtils.hasText(keyword)) {
            throw new BadRequestException("Search keyword must not be blank.");
        }

        return problemReportRepository.searchByDescriptionKeyword(keyword.trim(), status, pageable)
                .map(problemReportMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProblemReportResponse getReport(Long id) {
        ProblemReport report = findReportOrThrow(id);
        return problemReportMapper.toResponse(report);
    }

    @Transactional(readOnly = true)
    public Page<ProblemReportResponse> getReportsByLineId(Long lineId, Pageable pageable) {
        return problemReportRepository.findByLineId(lineId, pageable)
                .map(problemReportMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public LineReportCountResponse countReportsByLine(Long lineId) {
        return new LineReportCountResponse(lineId, problemReportRepository.countByLineId(lineId));
    }

    @Transactional
    public ProblemReportResponse updateStatus(Long id, ReportStatus status) {
        ProblemReport report = findReportOrThrow(id);
        report.setStatus(status);
        ProblemReport saved = problemReportRepository.save(report);
        return problemReportMapper.toResponse(saved);
    }

    @Transactional
    public void deleteReport(Long id) {
        ProblemReport report = findReportOrThrow(id);
        problemReportRepository.delete(report);
    }

    private ProblemReport findReportOrThrow(Long id) {
        return problemReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Problem report not found: id=" + id));
    }

    private ProblemReport buildEntityForCreate(CreateProblemReportRequest request) {
        validateVehicleOrStationReference(request.getVehicleId(),
                request.getVehicleRegistrationNumber(),
                request.getVehicleInternalId(),
                request.getStationId());

        ProblemReport entity = problemReportMapper.toEntity(request);
        entity.setStatus(ReportStatus.RECEIVED);
        applyNormalization(entity);
        return entity;
    }

    private ProblemReportPatchRequest mapToPatchRequest(ProblemReport report) {
        ProblemReportPatchRequest request = new ProblemReportPatchRequest();
        request.setLineId(report.getLineId());
        request.setVehicleId(report.getVehicleId());
        request.setVehicleRegistrationNumber(report.getVehicleRegistrationNumber());
        request.setVehicleInternalId(report.getVehicleInternalId());
        request.setVehicleType(report.getVehicleType());
        request.setStationId(report.getStationId());
        request.setCategory(report.getCategory());
        request.setDescription(report.getDescription());
        request.setPhotoUrls(report.getPhotoUrls());
        request.setStatus(report.getStatus());
        return request;
    }

    private ProblemReportPatchRequest applyJsonPatchToRequest(
            ProblemReportPatchRequest currentRequest,
            JsonNode patchDocument) {
        if (patchDocument == null || !patchDocument.isArray()) {
            throw new BadRequestException("JSON Patch payload must be an array of operations.");
        }

        try {
            JsonNode currentNode = objectMapper.convertValue(currentRequest, JsonNode.class);
            JsonNode patchedNode = applyPatchOperations(currentNode.deepCopy(), patchDocument);
            return objectMapper.treeToValue(patchedNode, ProblemReportPatchRequest.class);
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new BadRequestException("Invalid JSON Patch payload.");
        }
    }

    private JsonNode applyPatchOperations(JsonNode targetNode, JsonNode patchDocument) {
        for (JsonNode operation : patchDocument) {
            String op = getRequiredText(operation, "op");
            String path = getRequiredText(operation, "path");

            switch (op) {
                case "replace" -> setNodeValue(targetNode, path, operation.get("value"), false);
                case "add" -> setNodeValue(targetNode, path, operation.get("value"), true);
                case "remove" -> removeNodeValue(targetNode, path);
                default -> throw new BadRequestException("Unsupported JSON Patch operation: " + op);
            }
        }
        return targetNode;
    }

    private String getRequiredText(JsonNode operation, String field) {
        JsonNode value = operation.get(field);
        if (value == null || !value.isTextual() || !StringUtils.hasText(value.asText())) {
            throw new BadRequestException("JSON Patch operation must contain non-empty '" + field + "'.");
        }
        return value.asText();
    }

    private void setNodeValue(JsonNode root, String path, JsonNode value, boolean isAddOperation) {
        if (value == null) {
            throw new BadRequestException("JSON Patch '" + (isAddOperation ? "add" : "replace")
                    + "' operation requires 'value'.");
        }

        PatchLocation location = resolvePath(root, path, isAddOperation);
        JsonNode parent = location.parent();
        String token = location.token();

        if (parent instanceof ObjectNode objectNode) {
            if (!isAddOperation && !objectNode.has(token)) {
                throw new BadRequestException("Cannot replace missing path: " + path);
            }
            objectNode.set(token, value.deepCopy());
            return;
        }

        if (parent instanceof ArrayNode arrayNode) {
            int index = parseArrayIndex(token, arrayNode.size(), isAddOperation);
            if (isAddOperation) {
                if (index == arrayNode.size()) {
                    arrayNode.add(value.deepCopy());
                } else {
                    arrayNode.insert(index, value.deepCopy());
                }
            } else {
                arrayNode.set(index, value.deepCopy());
            }
            return;
        }

        throw new BadRequestException("Unsupported JSON Patch target path: " + path);
    }

    private void removeNodeValue(JsonNode root, String path) {
        PatchLocation location = resolvePath(root, path, false);
        JsonNode parent = location.parent();
        String token = location.token();

        if (parent instanceof ObjectNode objectNode) {
            if (!objectNode.has(token)) {
                throw new BadRequestException("Cannot remove missing path: " + path);
            }
            objectNode.remove(token);
            return;
        }

        if (parent instanceof ArrayNode arrayNode) {
            int index = parseArrayIndex(token, arrayNode.size(), false);
            arrayNode.remove(index);
            return;
        }

        throw new BadRequestException("Unsupported JSON Patch target path: " + path);
    }

    private PatchLocation resolvePath(JsonNode root, String path, boolean isAddOperation) {
        if (!StringUtils.hasText(path) || !path.startsWith("/")) {
            throw new BadRequestException("Invalid JSON Patch path: " + path);
        }

        String[] tokens = path.substring(1).split("/");
        if (tokens.length == 0 || (tokens.length == 1 && tokens[0].isEmpty())) {
            throw new BadRequestException("Invalid JSON Patch path: " + path);
        }

        JsonNode current = root;
        for (int i = 0; i < tokens.length - 1; i++) {
            String token = decodePathToken(tokens[i]);
            if (current instanceof ObjectNode objectNode) {
                current = objectNode.get(token);
            } else if (current instanceof ArrayNode arrayNode) {
                int index = parseArrayIndex(token, arrayNode.size(), false);
                current = arrayNode.get(index);
            } else {
                throw new BadRequestException("Invalid JSON Patch path segment: " + token);
            }

            if (current == null) {
                throw new BadRequestException("JSON Patch path does not exist: " + path);
            }
        }

        String finalToken = decodePathToken(tokens[tokens.length - 1]);
        if (isAddOperation && "-".equals(finalToken)) {
            return new PatchLocation(current, "-");
        }
        return new PatchLocation(current, finalToken);
    }

    private String decodePathToken(String token) {
        return token.replace("~1", "/").replace("~0", "~");
    }

    private int parseArrayIndex(String token, int size, boolean allowAppend) {
        if (allowAppend && "-".equals(token)) {
            return size;
        }

        int index;
        try {
            index = Integer.parseInt(token);
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Invalid array index in JSON Patch path: " + token);
        }

        int max = allowAppend ? size : size - 1;
        if (index < 0 || index > max) {
            throw new BadRequestException("Array index out of bounds in JSON Patch path: " + token);
        }
        return index;
    }

    private void validatePatchedRequest(ProblemReportPatchRequest patchedRequest) {
        Set<ConstraintViolation<ProblemReportPatchRequest>> violations = validator.validate(patchedRequest);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void applyPatchResultToEntity(ProblemReport report, ProblemReportPatchRequest patchedRequest) {
        report.setLineId(patchedRequest.getLineId());
        report.setVehicleId(patchedRequest.getVehicleId());
        report.setVehicleRegistrationNumber(patchedRequest.getVehicleRegistrationNumber());
        report.setVehicleInternalId(patchedRequest.getVehicleInternalId());
        report.setVehicleType(patchedRequest.getVehicleType());
        report.setStationId(patchedRequest.getStationId());
        report.setCategory(patchedRequest.getCategory());
        report.setDescription(patchedRequest.getDescription());
        report.setPhotoUrls(patchedRequest.getPhotoUrls());
        report.setStatus(patchedRequest.getStatus());
    }

    private void applyBusinessRules(ProblemReport entity) {
        validateVehicleOrStationReference(entity.getVehicleId(),
                entity.getVehicleRegistrationNumber(),
                entity.getVehicleInternalId(),
                entity.getStationId());

        applyNormalization(entity);
    }

    private void applyNormalization(ProblemReport entity) {
        entity.setVehicleRegistrationNumber(trimToNull(entity.getVehicleRegistrationNumber()));
        entity.setVehicleInternalId(trimToNull(entity.getVehicleInternalId()));
        entity.setVehicleType(normalizeVehicleType(entity.getVehicleType()));
        entity.setDescription(entity.getDescription().trim());
        entity.setPhotoUrls(entity.getPhotoUrls());
    }

    private void validateVehicleOrStationReference(Long vehicleId,
            String vehicleRegistrationNumber,
            String vehicleInternalId,
            Long stationId) {
        boolean hasVehicleReference = vehicleId != null
                || StringUtils.hasText(vehicleRegistrationNumber)
                || StringUtils.hasText(vehicleInternalId);
        if (!hasVehicleReference && stationId == null) {
            throw new BadRequestException(
                    "At least one of vehicleId/vehicleRegistrationNumber/vehicleInternalId or stationId must be provided.");
        }
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

    private record PatchLocation(JsonNode parent, String token) {
    }
}
