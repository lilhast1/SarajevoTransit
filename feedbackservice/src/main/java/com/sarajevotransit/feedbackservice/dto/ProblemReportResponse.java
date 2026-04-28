package com.sarajevotransit.feedbackservice.dto;

import com.sarajevotransit.feedbackservice.model.ProblemCategory;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProblemReportResponse {

    private Long id;
    private Long reporterUserId;
    private Long lineId;
    private Long vehicleId;
    private String vehicleRegistrationNumber;
    private String vehicleInternalId;
    private String vehicleType;
    private Long stationId;
    private ProblemCategory category;
    private String description;
    private List<String> photoUrls;
    private ReportStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
