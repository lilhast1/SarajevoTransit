package com.sarajevotransit.feedbackservice.controller;

import com.sarajevotransit.feedbackservice.dto.CreateLineReviewRequest;
import com.sarajevotransit.feedbackservice.dto.LineRatingSummaryResponse;
import com.sarajevotransit.feedbackservice.dto.LineReviewResponse;
import com.sarajevotransit.feedbackservice.dto.ReviewModerationRequest;
import com.sarajevotransit.feedbackservice.service.LineReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
public class LineReviewController {

    private final LineReviewService lineReviewService;

    public LineReviewController(LineReviewService lineReviewService) {
        this.lineReviewService = lineReviewService;
    }

    @PostMapping
    public ResponseEntity<LineReviewResponse> createReview(@Valid @RequestBody CreateLineReviewRequest request) {
        LineReviewResponse created = lineReviewService.createReview(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public Page<LineReviewResponse> getReviews(
            @RequestParam @Positive Long lineId,
            @RequestParam(defaultValue = "false") boolean includeHidden,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return lineReviewService.getReviewsByLine(lineId, includeHidden, pageable);
    }

    @GetMapping("/{id}")
    public LineReviewResponse getReview(@PathVariable @Positive Long id) {
        return lineReviewService.getReview(id);
    }

    @GetMapping("/line/{lineId}/latest")
    public LineReviewResponse getLatestVisibleReviewByLine(@PathVariable @Positive Long lineId) {
        return lineReviewService.getLatestVisibleReviewByLine(lineId);
    }

    @GetMapping("/reviewer/{reviewerUserId}")
    public Page<LineReviewResponse> getReviewsByReviewer(
            @PathVariable @Positive Long reviewerUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return lineReviewService.getReviewsByReviewer(reviewerUserId, pageable);
    }

    @PatchMapping("/{id}/moderation-status")
    public LineReviewResponse updateModerationStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReviewModerationRequest request) {
        return lineReviewService.updateModerationStatus(id, request.getModerationStatus());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable @Positive Long id) {
        lineReviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public List<LineRatingSummaryResponse> getVisibleSummaries() {
        return lineReviewService.getAllVisibleSummaries();
    }

    @GetMapping("/summary/{lineId}")
    public LineRatingSummaryResponse getVisibleSummaryByLine(@PathVariable @Positive Long lineId) {
        return lineReviewService.getVisibleSummaryByLineId(lineId);
    }
}
