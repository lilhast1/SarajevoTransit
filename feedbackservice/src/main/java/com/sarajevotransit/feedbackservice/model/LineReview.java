package com.sarajevotransit.feedbackservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_reviews_line_created", columnList = "line_id, createdAt"),
        @Index(name = "idx_reviews_line_moderation_created", columnList = "line_id, moderation_status, createdAt"),
        @Index(name = "idx_reviews_user_created", columnList = "user_id, createdAt")
})
@Check(constraints = "rating between 1 and 5")
@Getter
@Setter
@NoArgsConstructor
public class LineReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @NotNull
    @Positive
    @Column(name = "user_id", nullable = false)
    private Long reviewerUserId;

    @NotNull
    @Positive
    @Column(name = "line_id", nullable = false)
    private Long lineId;

    @NotNull
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer rating;

    @Size(max = 1500)
    @Column(name = "comment", length = 1500)
    private String reviewText;

    @NotNull
    @Column(name = "ride_date", nullable = false)
    private LocalDate rideDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 30)
    private ModerationStatus moderationStatus = ModerationStatus.VISIBLE;

    @NotNull
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @NotNull
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}