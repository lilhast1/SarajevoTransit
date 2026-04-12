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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class LineReviewService {

    private static final int REVIEW_WINDOW_DAYS = 30;

    private final LineReviewRepository lineReviewRepository;
    private final LineReviewMapper lineReviewMapper;

    public LineReviewService(LineReviewRepository lineReviewRepository, LineReviewMapper lineReviewMapper) {
        this.lineReviewRepository = lineReviewRepository;
        this.lineReviewMapper = lineReviewMapper;
    }

    @Transactional
    public LineReviewResponse createReview(CreateLineReviewRequest request) {
        LocalDate today = LocalDate.now();
        if (request.getRideDate().isAfter(today)) {
            throw new BadRequestException("rideDate cannot be in the future.");
        }
        if (request.getRideDate().isBefore(today.minusDays(REVIEW_WINDOW_DAYS))) {
            throw new BadRequestException("Review is allowed only for rides within the last 30 days.");
        }

        LineReview entity = lineReviewMapper.toEntity(request);
        entity.setReviewText(trimToNull(entity.getReviewText()));
        entity.setModerationStatus(ModerationStatus.VISIBLE);

        LineReview saved = lineReviewRepository.save(entity);
        return lineReviewMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LineReviewResponse> getReviewsByLine(Long lineId, boolean includeHidden) {
        List<LineReview> reviews;
        if (includeHidden) {
            reviews = lineReviewRepository.findByLineIdOrderByCreatedAtDesc(lineId);
        } else {
            reviews = lineReviewRepository.findByLineIdAndModerationStatusOrderByCreatedAtDesc(lineId,
                    ModerationStatus.VISIBLE);
        }
        return reviews.stream().map(lineReviewMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LineReviewResponse getReview(Long id) {
        LineReview review = lineReviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Line review not found: id=" + id));
        return lineReviewMapper.toResponse(review);
    }

    @Transactional(readOnly = true)
    public List<LineReviewResponse> getReviewsByReviewer(Long reviewerUserId) {
        return lineReviewRepository.findByReviewerUserIdOrderByCreatedAtDesc(reviewerUserId)
                .stream()
                .map(lineReviewMapper::toResponse)
                .toList();
    }

    @Transactional
    public LineReviewResponse updateModerationStatus(Long id, ModerationStatus moderationStatus) {
        LineReview review = lineReviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Line review not found: id=" + id));
        review.setModerationStatus(moderationStatus);
        LineReview saved = lineReviewRepository.save(review);
        return lineReviewMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LineRatingSummaryResponse> getAllVisibleSummaries() {
        return lineReviewRepository.fetchVisibleLineRatingSummaries();
    }

    @Transactional(readOnly = true)
    public LineRatingSummaryResponse getVisibleSummaryByLineId(Long lineId) {
        return lineReviewRepository.fetchVisibleSummaryByLineId(lineId)
                .stream()
                .findFirst()
                .orElse(new LineRatingSummaryResponse(lineId, 0.0, 0L));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
