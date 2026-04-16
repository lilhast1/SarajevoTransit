package com.sarajevotransit.feedbackservice.dto;

import com.sarajevotransit.feedbackservice.model.ModerationStatus;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LineModerationRequest {

    @NotNull
    private ReportStatus reportStatus;

    @NotNull
    private ModerationStatus moderationStatus;
}
