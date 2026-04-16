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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LineReviewService {

    private static final int REVIEW_WINDOW_DAYS = 30;

    private final LineReviewRepository lineReviewRepository;
    private final LineReviewMapper lineReviewMapper;

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
    public Page<LineReviewResponse> getReviewsByLine(Long lineId, boolean includeHidden, Pageable pageable) {
        Page<LineReview> reviews;
        if (includeHidden) {
            reviews = lineReviewRepository.findByLineId(lineId, pageable);
        } else {
            reviews = lineReviewRepository.findByLineIdAndModerationStatus(lineId,
                    ModerationStatus.VISIBLE,
                    pageable);
        }
        return reviews.map(lineReviewMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public LineReviewResponse getReview(Long id) {
        LineReview review = findReviewOrThrow(id);
        return lineReviewMapper.toResponse(review);
    }

    @Transactional(readOnly = true)
    public LineReviewResponse getLatestVisibleReviewByLine(Long lineId) {
        LineReview review = lineReviewRepository.findFirstByLineIdAndModerationStatusOrderByCreatedAtDesc(
                lineId,
                ModerationStatus.VISIBLE)
                .orElseThrow(() -> new NotFoundException("No visible review found for lineId=" + lineId));
        return lineReviewMapper.toResponse(review);
    }

    @Transactional(readOnly = true)
    public Page<LineReviewResponse> getReviewsByReviewer(Long reviewerUserId, Pageable pageable) {
        return lineReviewRepository.findByReviewerUserId(reviewerUserId, pageable)
                .map(lineReviewMapper::toResponse);
    }

    @Transactional
    public LineReviewResponse updateModerationStatus(Long id, ModerationStatus moderationStatus) {
        LineReview review = findReviewOrThrow(id);
        review.setModerationStatus(moderationStatus);
        LineReview saved = lineReviewRepository.save(review);
        return lineReviewMapper.toResponse(saved);
    }

    @Transactional
    public void deleteReview(Long id) {
        LineReview review = findReviewOrThrow(id);
        lineReviewRepository.delete(review);
    }

    private LineReview findReviewOrThrow(Long id) {
        return lineReviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Line review not found: id=" + id));
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
