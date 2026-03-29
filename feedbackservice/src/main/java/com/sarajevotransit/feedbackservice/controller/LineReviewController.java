package com.sarajevotransit.feedbackservice.controller;

import com.sarajevotransit.feedbackservice.dto.CreateLineReviewRequest;
import com.sarajevotransit.feedbackservice.dto.LineRatingSummaryResponse;
import com.sarajevotransit.feedbackservice.dto.LineReviewResponse;
import com.sarajevotransit.feedbackservice.dto.ReviewModerationRequest;
import com.sarajevotransit.feedbackservice.service.LineReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
public class LineReviewController {

    private final LineReviewService lineReviewService;

    public LineReviewController(LineReviewService lineReviewService) {
        this.lineReviewService = lineReviewService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LineReviewResponse createReview(@Valid @RequestBody CreateLineReviewRequest request) {
        return lineReviewService.createReview(request);
    }

    @GetMapping
    public List<LineReviewResponse> getReviews(
            @RequestParam String lineCode,
            @RequestParam(defaultValue = "false") boolean includeHidden) {
        return lineReviewService.getReviewsByLine(lineCode, includeHidden);
    }

    @PatchMapping("/{id}/moderation-status")
    public LineReviewResponse updateModerationStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReviewModerationRequest request) {
        return lineReviewService.updateModerationStatus(id, request.getModerationStatus());
    }

    @GetMapping("/summary")
    public List<LineRatingSummaryResponse> getVisibleSummaries() {
        return lineReviewService.getAllVisibleSummaries();
    }

    @GetMapping("/summary/{lineCode}")
    public LineRatingSummaryResponse getVisibleSummaryByLine(@PathVariable String lineCode) {
        return lineReviewService.getVisibleSummaryByLineCode(lineCode);
    }
}
