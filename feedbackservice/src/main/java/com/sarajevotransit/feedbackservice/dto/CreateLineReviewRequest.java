package com.sarajevotransit.feedbackservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class CreateLineReviewRequest {

    @NotNull
    @Positive
    private Long reviewerUserId;

    @NotNull
    @Positive
    private Long lineId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 1500)
    private String reviewText;

    @NotNull
    private LocalDate rideDate;
}
