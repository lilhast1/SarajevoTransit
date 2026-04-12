package com.sarajevotransit.feedbackservice.service;

import com.sarajevotransit.feedbackservice.dto.CreateProblemReportRequest;
import com.sarajevotransit.feedbackservice.dto.ProblemReportResponse;
import com.sarajevotransit.feedbackservice.exception.BadRequestException;
import com.sarajevotransit.feedbackservice.exception.NotFoundException;
import com.sarajevotransit.feedbackservice.mapper.ProblemReportMapper;
import com.sarajevotransit.feedbackservice.model.ProblemCategory;
import com.sarajevotransit.feedbackservice.model.ProblemReport;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import com.sarajevotransit.feedbackservice.repository.ProblemReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemReportServiceTest {

    @Mock
    private ProblemReportRepository problemReportRepository;

    @Mock
    private ProblemReportMapper problemReportMapper;

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

        when(problemReportRepository.findByStatusAndReporterUserIdOrderByCreatedAtDesc(ReportStatus.RECEIVED, 44L))
                .thenReturn(List.of(entity));
        when(problemReportMapper.toResponse(entity)).thenReturn(response);

        List<ProblemReportResponse> result = problemReportService.getReports(ReportStatus.RECEIVED, 44L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getReporterUserId()).isEqualTo(44L);
    }
}
