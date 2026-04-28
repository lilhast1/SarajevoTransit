package com.sarajevotransit.feedbackservice.dto;

import com.sarajevotransit.feedbackservice.model.ModerationStatus;
import com.sarajevotransit.feedbackservice.model.ReportStatus;

public record LineModerationResponse(
        Long lineId,
        ReportStatus reportStatus,
        ModerationStatus moderationStatus,
        int updatedReports,
        int updatedReviews) {
}
