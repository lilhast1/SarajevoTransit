package com.sarajevotransit.feedbackservice.dto;

import com.sarajevotransit.feedbackservice.model.ProblemCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateProblemReportRequest {

    @NotNull
    @Positive
    private Long reporterUserId;

    @Positive
    private Long lineId;

    @Positive
    private Long vehicleId;

    @Size(max = 60)
    private String vehicleRegistrationNumber;

    @Size(max = 60)
    private String vehicleInternalId;

    @Size(max = 30)
    private String vehicleType;

    @Positive
    private Long stationId;

    @NotNull
    private ProblemCategory category;

    @NotBlank
    @Size(max = 1000)
    private String description;

    private List<@Size(max = 500) String> photoUrls = new ArrayList<>();
}
