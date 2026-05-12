package com.sarajevotransit.feedbackservice.controller;

import com.sarajevotransit.feedbackservice.dto.CreateLineReviewRequest;
import com.sarajevotransit.feedbackservice.dto.LineRatingSummaryResponse;
import com.sarajevotransit.feedbackservice.dto.LineReviewResponse;
import com.sarajevotransit.feedbackservice.dto.ReviewModerationRequest;
import com.sarajevotransit.feedbackservice.service.LineReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class LineReviewController {

    private final LineReviewService lineReviewService;

    @PostMapping
    public ResponseEntity<LineReviewResponse> createReview(
            @Valid @RequestBody CreateLineReviewRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        LineReviewResponse created = lineReviewService.createReview(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(security = {})
    public Page<LineReviewResponse> getReviews(
            @RequestParam @Positive Long lineId,
            @RequestParam(defaultValue = "false") boolean includeHidden,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return lineReviewService.getReviewsByLine(lineId, includeHidden, pageable);
    }

    @GetMapping("/{id}")
    @Operation(security = {})
    public LineReviewResponse getReview(@PathVariable @Positive Long id) {
        return lineReviewService.getReview(id);
    }

    @GetMapping("/line/{lineId}/latest")
    @Operation(security = {})
    public LineReviewResponse getLatestVisibleReviewByLine(@PathVariable @Positive Long lineId) {
        return lineReviewService.getLatestVisibleReviewByLine(lineId);
    }

    @GetMapping("/reviewer/{reviewerUserId}")
    public Page<LineReviewResponse> getReviewsByReviewer(
            @PathVariable @Positive Long reviewerUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        return lineReviewService.getReviewsByReviewer(reviewerUserId, pageable);
    }

    @PatchMapping("/{id}/moderation-status")
    public LineReviewResponse updateModerationStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ReviewModerationRequest request) {
        // ADMIN-only: enforced at gateway level (PATCH /api/v1/reviews/**)
        return lineReviewService.updateModerationStatus(id, request.getModerationStatus());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable @Positive Long id,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        lineReviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    @Operation(security = {})
    public List<LineRatingSummaryResponse> getVisibleSummaries() {
        return lineReviewService.getAllVisibleSummaries();
    }

    @GetMapping("/summary/{lineId}")
    @Operation(security = {})
    public LineRatingSummaryResponse getVisibleSummaryByLine(@PathVariable @Positive Long lineId) {
        return lineReviewService.getVisibleSummaryByLineId(lineId);
    }
}
