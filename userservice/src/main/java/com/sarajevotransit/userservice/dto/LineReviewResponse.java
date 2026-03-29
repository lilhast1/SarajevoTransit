package com.sarajevotransit.userservice.dto;

import com.sarajevotransit.userservice.model.ModerationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LineReviewResponse(
                Long id,
                Long reviewerUserId,
                String lineCode,
                int rating,
                String reviewText,
                LocalDate rideDate,
                ModerationStatus moderationStatus,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
}
