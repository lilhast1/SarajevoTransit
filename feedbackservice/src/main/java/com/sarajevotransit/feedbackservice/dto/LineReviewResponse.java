package com.sarajevotransit.feedbackservice.dto;

import com.sarajevotransit.feedbackservice.model.ModerationStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
public class LineReviewResponse {

    private Long id;
    private Long reviewerUserId;
    private Long lineId;
    private Integer rating;
    private String reviewText;
    private LocalDate rideDate;
    private ModerationStatus moderationStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
