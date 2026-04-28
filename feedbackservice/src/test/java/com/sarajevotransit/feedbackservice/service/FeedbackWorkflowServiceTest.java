package com.sarajevotransit.feedbackservice.service;

import com.sarajevotransit.feedbackservice.dto.LineModerationResponse;
import com.sarajevotransit.feedbackservice.exception.NotFoundException;
import com.sarajevotransit.feedbackservice.model.LineReview;
import com.sarajevotransit.feedbackservice.model.ModerationStatus;
import com.sarajevotransit.feedbackservice.model.ProblemReport;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import com.sarajevotransit.feedbackservice.repository.LineReviewRepository;
import com.sarajevotransit.feedbackservice.repository.ProblemReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackWorkflowServiceTest {

    @Mock
    private ProblemReportRepository problemReportRepository;

    @Mock
    private LineReviewRepository lineReviewRepository;

    @InjectMocks
    private FeedbackWorkflowService feedbackWorkflowService;

    @Test
    void moderateLineFeedback_shouldUpdateAndPersistReportsAndReviews() {
        ProblemReport firstReport = new ProblemReport();
        firstReport.setStatus(ReportStatus.RECEIVED);

        ProblemReport secondReport = new ProblemReport();
        secondReport.setStatus(ReportStatus.RESOLVED);

        LineReview firstReview = new LineReview();
        firstReview.setModerationStatus(ModerationStatus.VISIBLE);

        LineReview secondReview = new LineReview();
        secondReview.setModerationStatus(ModerationStatus.HIDDEN);

        when(problemReportRepository.findByLineIdOrderByCreatedAtDesc(44L))
                .thenReturn(List.of(firstReport, secondReport));
        when(lineReviewRepository.findByLineIdOrderByCreatedAtDesc(44L))
                .thenReturn(List.of(firstReview, secondReview));

        LineModerationResponse result = feedbackWorkflowService.moderateLineFeedback(
                44L,
                ReportStatus.RESOLVED,
                ModerationStatus.HIDDEN);

        assertThat(result.lineId()).isEqualTo(44L);
        assertThat(result.updatedReports()).isEqualTo(1);
        assertThat(result.updatedReviews()).isEqualTo(1);
        assertThat(firstReport.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(firstReview.getModerationStatus()).isEqualTo(ModerationStatus.HIDDEN);

        verify(problemReportRepository).saveAll(anyList());
        verify(lineReviewRepository).saveAll(anyList());
    }

    @Test
    void moderateLineFeedback_shouldThrowWhenNoFeedbackExists() {
        when(problemReportRepository.findByLineIdOrderByCreatedAtDesc(999L)).thenReturn(List.of());
        when(lineReviewRepository.findByLineIdOrderByCreatedAtDesc(999L)).thenReturn(List.of());

        assertThatThrownBy(() -> feedbackWorkflowService.moderateLineFeedback(
                999L,
                ReportStatus.RESOLVED,
                ModerationStatus.HIDDEN))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("lineId=999");

        verify(problemReportRepository, never()).saveAll(anyList());
        verify(lineReviewRepository, never()).saveAll(anyList());
    }
}
