package com.sarajevotransit.feedbackservice.dto;

import com.sarajevotransit.feedbackservice.model.ReportStatus;
import jakarta.validation.constraints.NotNull;

public class ReportStatusUpdateRequest {

    @NotNull
    private ReportStatus status;

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }
}
