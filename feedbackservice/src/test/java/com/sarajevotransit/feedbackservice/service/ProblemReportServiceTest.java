package com.sarajevotransit.feedbackservice.service;

import tools.jackson.databind.ObjectMapper;
import com.sarajevotransit.feedbackservice.dto.BatchCreateProblemReportsResponse;
import com.sarajevotransit.feedbackservice.dto.CreateProblemReportRequest;
import com.sarajevotransit.feedbackservice.dto.LineReportCountResponse;
import com.sarajevotransit.feedbackservice.dto.ProblemReportResponse;
import com.sarajevotransit.feedbackservice.exception.BadRequestException;
import com.sarajevotransit.feedbackservice.exception.NotFoundException;
import com.sarajevotransit.feedbackservice.mapper.ProblemReportMapper;
import com.sarajevotransit.feedbackservice.model.ProblemCategory;
import com.sarajevotransit.feedbackservice.model.ProblemReport;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import com.sarajevotransit.feedbackservice.repository.ProblemReportRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemReportServiceTest {

    @Mock
    private ProblemReportRepository problemReportRepository;

    @Mock
    private ProblemReportMapper problemReportMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Validator validator;

    @InjectMocks
    private ProblemReportService problemReportService;

    @Test
    void createReport_shouldThrowWhenVehicleAndStationReferencesMissing() {
        CreateProblemReportRequest request = new CreateProblemReportRequest();
        request.setReporterUserId(1L);
        request.setCategory(ProblemCategory.DELAY);
        request.setDescription("Delayed");

        assertThatThrownBy(() -> problemReportService.createReport(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("At least one of vehicleId");

        verify(problemReportRepository, never()).save(any());
    }

    @Test
    void createReport_shouldNormalizeVehicleTypeAndTrimFields() {
        CreateProblemReportRequest request = new CreateProblemReportRequest();
        request.setReporterUserId(3L);
        request.setLineId(6L);
        request.setVehicleId(100L);
        request.setVehicleRegistrationNumber("  A12-E-345  ");
        request.setVehicleInternalId("  304 ");
        request.setVehicleType("bus");
        request.setCategory(ProblemCategory.BREAKDOWN);
        request.setDescription("  Driver reported issue.  ");
        request.setPhotoUrls(List.of("https://example.com/p1.png"));

        ProblemReport mappedEntity = new ProblemReport();
        mappedEntity.setReporterUserId(request.getReporterUserId());
        mappedEntity.setLineId(request.getLineId());
        mappedEntity.setVehicleId(request.getVehicleId());
        mappedEntity.setVehicleRegistrationNumber(request.getVehicleRegistrationNumber());
        mappedEntity.setVehicleInternalId(request.getVehicleInternalId());
        mappedEntity.setVehicleType(request.getVehicleType());
        mappedEntity.setCategory(request.getCategory());
        mappedEntity.setDescription(request.getDescription());
        mappedEntity.setPhotoUrls(request.getPhotoUrls());

        ProblemReportResponse response = new ProblemReportResponse();
        response.setId(77L);

        when(problemReportMapper.toEntity(request)).thenReturn(mappedEntity);
        when(problemReportRepository.save(any(ProblemReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(problemReportMapper.toResponse(any(ProblemReport.class))).thenReturn(response);

        ProblemReportResponse result = problemReportService.createReport(request);

        assertThat(result.getId()).isEqualTo(77L);
        ArgumentCaptor<ProblemReport> captor = ArgumentCaptor.forClass(ProblemReport.class);
        verify(problemReportRepository).save(captor.capture());
        ProblemReport saved = captor.getValue();
        assertThat(saved.getVehicleType()).isEqualTo("BUS");
        assertThat(saved.getVehicleRegistrationNumber()).isEqualTo("A12-E-345");
        assertThat(saved.getVehicleInternalId()).isEqualTo("304");
        assertThat(saved.getDescription()).isEqualTo("Driver reported issue.");
        assertThat(saved.getStatus()).isEqualTo(ReportStatus.RECEIVED);
    }

    @Test
    void createReport_shouldThrowForUnsupportedVehicleType() {
        CreateProblemReportRequest request = new CreateProblemReportRequest();
        request.setReporterUserId(3L);
        request.setVehicleId(100L);
        request.setCategory(ProblemCategory.BREAKDOWN);
        request.setDescription("desc");

        ProblemReport mappedEntity = new ProblemReport();
        mappedEntity.setVehicleType("PLANE");

        when(problemReportMapper.toEntity(request)).thenReturn(mappedEntity);

        assertThatThrownBy(() -> problemReportService.createReport(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("vehicleType must be one of");

        verify(problemReportRepository, never()).save(any());
    }

    @Test
    void getReport_shouldThrowWhenReportNotFound() {
        when(problemReportRepository.findById(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> problemReportService.getReport(55L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Problem report not found");
    }

    @Test
    void getReports_shouldUseCombinedFilterRepositoryMethod() {
        ProblemReport entity = new ProblemReport();
        entity.setReporterUserId(44L);
        ProblemReportResponse response = new ProblemReportResponse();
        response.setReporterUserId(44L);

        when(problemReportRepository.findByStatusAndReporterUserId(
                eq(ReportStatus.RECEIVED),
                eq(44L),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(problemReportMapper.toResponse(entity)).thenReturn(response);

        Page<ProblemReportResponse> result = problemReportService.getReports(
                ReportStatus.RECEIVED,
                44L,
                PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getReporterUserId()).isEqualTo(44L);
    }

    @Test
    void createReportsBatch_shouldPersistAllItems() {
        CreateProblemReportRequest firstRequest = new CreateProblemReportRequest();
        firstRequest.setReporterUserId(101L);
        firstRequest.setLineId(6L);
        firstRequest.setStationId(10L);
        firstRequest.setCategory(ProblemCategory.DELAY);
        firstRequest.setDescription("First batch report");

        CreateProblemReportRequest secondRequest = new CreateProblemReportRequest();
        secondRequest.setReporterUserId(102L);
        secondRequest.setLineId(6L);
        secondRequest.setVehicleId(99L);
        secondRequest.setCategory(ProblemCategory.CROWDING);
        secondRequest.setDescription("Second batch report");

        ProblemReport firstEntity = new ProblemReport();
        firstEntity.setReporterUserId(firstRequest.getReporterUserId());
        firstEntity.setLineId(firstRequest.getLineId());
        firstEntity.setStationId(firstRequest.getStationId());
        firstEntity.setCategory(firstRequest.getCategory());
        firstEntity.setDescription(firstRequest.getDescription());

        ProblemReport secondEntity = new ProblemReport();
        secondEntity.setReporterUserId(secondRequest.getReporterUserId());
        secondEntity.setLineId(secondRequest.getLineId());
        secondEntity.setVehicleId(secondRequest.getVehicleId());
        secondEntity.setCategory(secondRequest.getCategory());
        secondEntity.setDescription(secondRequest.getDescription());

        ProblemReportResponse firstResponse = new ProblemReportResponse();
        firstResponse.setId(1L);

        ProblemReportResponse secondResponse = new ProblemReportResponse();
        secondResponse.setId(2L);

        when(problemReportMapper.toEntity(firstRequest)).thenReturn(firstEntity);
        when(problemReportMapper.toEntity(secondRequest)).thenReturn(secondEntity);
        when(problemReportRepository.saveAll(anyList())).thenReturn(List.of(firstEntity, secondEntity));
        when(problemReportMapper.toResponse(firstEntity)).thenReturn(firstResponse);
        when(problemReportMapper.toResponse(secondEntity)).thenReturn(secondResponse);

        BatchCreateProblemReportsResponse result = problemReportService.createReportsBatch(
                List.of(firstRequest, secondRequest));

        assertThat(result.insertedCount()).isEqualTo(2);
        assertThat(result.reports()).hasSize(2);
        assertThat(result.reports()).extracting(ProblemReportResponse::getId).containsExactly(1L, 2L);
        verify(problemReportRepository).saveAll(anyList());
    }

    @Test
    void searchReports_shouldRejectBlankKeyword() {
        assertThatThrownBy(() -> problemReportService.searchReports("   ", null, PageRequest.of(0, 10)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("keyword");
    }

    @Test
    void searchReports_shouldUseCustomRepositoryQuery() {
        ProblemReport entity = new ProblemReport();
        entity.setDescription("Delay report");

        ProblemReportResponse response = new ProblemReportResponse();
        response.setDescription("Delay report");

        when(problemReportRepository.searchByDescriptionKeyword(
                eq("delay"),
                eq(ReportStatus.RECEIVED),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(problemReportMapper.toResponse(entity)).thenReturn(response);

        Page<ProblemReportResponse> result = problemReportService.searchReports(
                " delay ",
                ReportStatus.RECEIVED,
                PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getDescription()).isEqualTo("Delay report");
    }

    @Test
    void countReportsByLine_shouldReturnCountResponse() {
        when(problemReportRepository.countByLineId(33L)).thenReturn(4L);

        LineReportCountResponse result = problemReportService.countReportsByLine(33L);

        assertThat(result.lineId()).isEqualTo(33L);
        assertThat(result.totalReports()).isEqualTo(4L);
    }

    @Test
    void deleteReport_shouldDeleteExistingReport() {
        ProblemReport report = new ProblemReport();
        report.setId(88L);
        when(problemReportRepository.findById(88L)).thenReturn(Optional.of(report));

        problemReportService.deleteReport(88L);

        verify(problemReportRepository).delete(report);
    }
}
