package com.sarajevotransit.userservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_points")
public class LoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private UserProfile user;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "points_earned", nullable = false)
    @NotNull
    @Min(0)
    private Integer pointsEarned = 0;

    @Column(name = "points_spent", nullable = false)
    @NotNull
    @Min(0)
    private Integer pointsSpent = 0;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(nullable = false)
    @NotBlank
    @Size(max = 500)
    private String description;

    @Column(name = "reference_type", nullable = false)
    @NotBlank
    @Size(max = 100)
    private String referenceType;

    @Column(name = "created_at", nullable = false)
    @NotNull
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserProfile getUser() {
        return user;
    }

    public void setUser(UserProfile user) {
        this.user = user;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Integer getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(Integer pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public Integer getPointsSpent() {
        return pointsSpent;
    }

    public void setPointsSpent(Integer pointsSpent) {
        this.pointsSpent = pointsSpent;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LoyaltyTransactionType getTransactionType() {
        if (pointsSpent != null && pointsSpent > 0) {
            return LoyaltyTransactionType.REDEEM;
        }
        return LoyaltyTransactionType.EARN;
    }

    public Integer getPoints() {
        if (pointsSpent != null && pointsSpent > 0) {
            return pointsSpent;
        }
        return pointsEarned == null ? 0 : pointsEarned;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
