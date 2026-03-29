package com.sarajevotransit.feedbackservice.service;

import com.sarajevotransit.feedbackservice.dto.CreateLineReviewRequest;
import com.sarajevotransit.feedbackservice.dto.LineRatingSummaryResponse;
import com.sarajevotransit.feedbackservice.dto.LineReviewResponse;
import com.sarajevotransit.feedbackservice.exception.BadRequestException;
import com.sarajevotransit.feedbackservice.exception.NotFoundException;
import com.sarajevotransit.feedbackservice.model.LineReview;
import com.sarajevotransit.feedbackservice.model.ModerationStatus;
import com.sarajevotransit.feedbackservice.repository.LineReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class LineReviewService {

    private static final int REVIEW_WINDOW_DAYS = 30;

    private final LineReviewRepository lineReviewRepository;

    public LineReviewService(LineReviewRepository lineReviewRepository) {
        this.lineReviewRepository = lineReviewRepository;
    }

    public LineReviewResponse createReview(CreateLineReviewRequest request) {
        LocalDate today = LocalDate.now();
        if (request.getRideDate().isAfter(today)) {
            throw new BadRequestException("rideDate cannot be in the future.");
        }
        if (request.getRideDate().isBefore(today.minusDays(REVIEW_WINDOW_DAYS))) {
            throw new BadRequestException("Review is allowed only for rides within the last 30 days.");
        }

        LineReview entity = new LineReview();
        entity.setReviewerUserId(request.getReviewerUserId());
        entity.setLineCode(request.getLineCode().trim());
        entity.setRating(request.getRating());
        entity.setReviewText(trimToNull(request.getReviewText()));
        entity.setRideDate(request.getRideDate());
        entity.setModerationStatus(ModerationStatus.VISIBLE);

        LineReview saved = lineReviewRepository.save(entity);
        return toResponse(saved);
    }

    public List<LineReviewResponse> getReviewsByLine(String lineCode, boolean includeHidden) {
        List<LineReview> reviews;
        if (includeHidden) {
            reviews = lineReviewRepository.findByLineCodeOrderByCreatedAtDesc(lineCode);
        } else {
            reviews = lineReviewRepository.findByLineCodeAndModerationStatusOrderByCreatedAtDesc(lineCode,
                    ModerationStatus.VISIBLE);
        }
        return reviews.stream().map(this::toResponse).toList();
    }

    public LineReviewResponse updateModerationStatus(Long id, ModerationStatus moderationStatus) {
        LineReview review = lineReviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Line review not found: id=" + id));
        review.setModerationStatus(moderationStatus);
        LineReview saved = lineReviewRepository.save(review);
        return toResponse(saved);
    }

    public List<LineRatingSummaryResponse> getAllVisibleSummaries() {
        return lineReviewRepository.fetchVisibleLineRatingSummaries();
    }

    public LineRatingSummaryResponse getVisibleSummaryByLineCode(String lineCode) {
        return lineReviewRepository.fetchVisibleSummaryByLineCode(lineCode)
                .stream()
                .findFirst()
                .orElse(new LineRatingSummaryResponse(lineCode, 0.0, 0L));
    }

    private LineReviewResponse toResponse(LineReview entity) {
        LineReviewResponse response = new LineReviewResponse();
        response.setId(entity.getId());
        response.setReviewerUserId(entity.getReviewerUserId());
        response.setLineCode(entity.getLineCode());
        response.setRating(entity.getRating());
        response.setReviewText(entity.getReviewText());
        response.setRideDate(entity.getRideDate());
        response.setModerationStatus(entity.getModerationStatus());
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
}
