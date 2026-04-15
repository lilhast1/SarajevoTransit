package com.sarajevotransit.feedbackservice.controller;

import com.sarajevotransit.feedbackservice.dto.LineModerationRequest;
import com.sarajevotransit.feedbackservice.dto.LineModerationResponse;
import com.sarajevotransit.feedbackservice.service.FeedbackWorkflowService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workflows")
public class FeedbackWorkflowController {

    private final FeedbackWorkflowService feedbackWorkflowService;

    public FeedbackWorkflowController(FeedbackWorkflowService feedbackWorkflowService) {
        this.feedbackWorkflowService = feedbackWorkflowService;
    }

    @PostMapping("/lines/{lineId}/moderation")
    public LineModerationResponse moderateLineFeedback(
            @PathVariable @Positive Long lineId,
            @Valid @RequestBody LineModerationRequest request) {
        return feedbackWorkflowService.moderateLineFeedback(
                lineId,
                request.getReportStatus(),
                request.getModerationStatus());
    }
}
