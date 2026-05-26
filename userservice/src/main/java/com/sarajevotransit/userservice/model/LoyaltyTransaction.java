package com.sarajevotransit.userservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_points")
@Getter
@Setter
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

    @Column(name = "coupon_code", unique = true)
    private String couponCode;

    @Column(name = "coupon_type")
    @Enumerated(EnumType.STRING)
    private LoyaltyCouponType couponType;

    @Column(name = "coupon_tier")
    @Enumerated(EnumType.STRING)
    private LoyaltyTier couponTier;

    @Column(name = "coupon_discount_percent")
    private Integer couponDiscountPercent;

    @Column(name = "coupon_ride_code")
    private String couponRideCode;

    @Column(name = "coupon_redeemed_at")
    private LocalDateTime couponRedeemedAt;

    @Column(name = "coupon_active")
    private Boolean couponActive;

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
}
