package com.sarajevotransit.feedbackservice.dto;

import com.sarajevotransit.feedbackservice.model.ModerationStatus;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import jakarta.validation.constraints.NotNull;

public class LineModerationRequest {

    @NotNull
    private ReportStatus reportStatus;

    @NotNull
    private ModerationStatus moderationStatus;

    public ReportStatus getReportStatus() {
        return reportStatus;
    }

    public void setReportStatus(ReportStatus reportStatus) {
        this.reportStatus = reportStatus;
    }

    public ModerationStatus getModerationStatus() {
        return moderationStatus;
    }

    public void setModerationStatus(ModerationStatus moderationStatus) {
        this.moderationStatus = moderationStatus;
    }
}
