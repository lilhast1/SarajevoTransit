package com.sarajevotransit.feedbackservice.service;

import com.sarajevotransit.feedbackservice.dto.CreateLineReviewRequest;
import com.sarajevotransit.feedbackservice.dto.LineRatingSummaryResponse;
import com.sarajevotransit.feedbackservice.dto.LineReviewResponse;
import com.sarajevotransit.feedbackservice.exception.BadRequestException;
import com.sarajevotransit.feedbackservice.exception.NotFoundException;
import com.sarajevotransit.feedbackservice.mapper.LineReviewMapper;
import com.sarajevotransit.feedbackservice.model.LineReview;
import com.sarajevotransit.feedbackservice.model.ModerationStatus;
import com.sarajevotransit.feedbackservice.repository.LineReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LineReviewServiceTest {

    @Mock
    private LineReviewRepository lineReviewRepository;

    @Mock
    private LineReviewMapper lineReviewMapper;

    @InjectMocks
    private LineReviewService lineReviewService;

    @Test
    void createReview_shouldRejectFutureRideDate() {
        CreateLineReviewRequest request = new CreateLineReviewRequest();
        request.setReviewerUserId(11L);
        request.setLineId(3L);
        request.setRating(4);
        request.setRideDate(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> lineReviewService.createReview(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("rideDate cannot be in the future");

        verify(lineReviewRepository, never()).save(any());
    }

    @Test
    void createReview_shouldRejectRideDateOlderThanWindow() {
        CreateLineReviewRequest request = new CreateLineReviewRequest();
        request.setReviewerUserId(11L);
        request.setLineId(3L);
        request.setRating(4);
        request.setRideDate(LocalDate.now().minusDays(31));

        assertThatThrownBy(() -> lineReviewService.createReview(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("within the last 30 days");
    }

    @Test
    void createReview_shouldTrimTextAndSetVisibleModeration() {
        CreateLineReviewRequest request = new CreateLineReviewRequest();
        request.setReviewerUserId(11L);
        request.setLineId(3L);
        request.setRating(4);
        request.setReviewText("  Nice ride.  ");
        request.setRideDate(LocalDate.now().minusDays(1));

        LineReview mapped = new LineReview();
        mapped.setReviewerUserId(request.getReviewerUserId());
        mapped.setLineId(request.getLineId());
        mapped.setRating(request.getRating());
        mapped.setReviewText(request.getReviewText());
        mapped.setRideDate(request.getRideDate());

        LineReviewResponse response = new LineReviewResponse();
        response.setId(91L);

        when(lineReviewMapper.toEntity(request)).thenReturn(mapped);
        when(lineReviewRepository.save(any(LineReview.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lineReviewMapper.toResponse(any(LineReview.class))).thenReturn(response);

        LineReviewResponse result = lineReviewService.createReview(request);

        assertThat(result.getId()).isEqualTo(91L);
        ArgumentCaptor<LineReview> captor = ArgumentCaptor.forClass(LineReview.class);
        verify(lineReviewRepository).save(captor.capture());
        LineReview saved = captor.getValue();
        assertThat(saved.getReviewText()).isEqualTo("Nice ride.");
        assertThat(saved.getModerationStatus()).isEqualTo(ModerationStatus.VISIBLE);
    }

    @Test
    void getVisibleSummaryByLine_shouldReturnDefaultWhenMissing() {
        when(lineReviewRepository.fetchVisibleSummaryByLineId(77L)).thenReturn(List.of());

        LineRatingSummaryResponse result = lineReviewService.getVisibleSummaryByLineId(77L);

        assertThat(result.getLineId()).isEqualTo(77L);
        assertThat(result.getAverageRating()).isEqualTo(0.0);
        assertThat(result.getTotalReviews()).isEqualTo(0L);
    }

    @Test
    void getReview_shouldThrowWhenMissing() {
        when(lineReviewRepository.findById(22L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lineReviewService.getReview(22L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Line review not found");
    }
}
