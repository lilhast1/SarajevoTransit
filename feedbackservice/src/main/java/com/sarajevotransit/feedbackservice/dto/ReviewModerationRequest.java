package com.sarajevotransit.feedbackservice.dto;

import com.sarajevotransit.feedbackservice.model.ModerationStatus;
import jakarta.validation.constraints.NotNull;

public class ReviewModerationRequest {

    @NotNull
    private ModerationStatus moderationStatus;

    public ModerationStatus getModerationStatus() {
        return moderationStatus;
    }

    public void setModerationStatus(ModerationStatus moderationStatus) {
        this.moderationStatus = moderationStatus;
    }
}
