package com.sarajevotransit.userservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_tier_configs")
@Getter
@Setter
public class LoyaltyTierConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 20)
    private String tierName;

    @Min(0)
    @Column(name = "minimum_lifetime_points", nullable = false)
    private int minimumLifetimePoints;

    @Min(0)
    @Column(name = "discount_percent", nullable = false)
    private int discountPercent;

    @Column(name = "free_ride_eligible", nullable = false)
    private boolean freeRideEligible;

    @Min(0)
    @Column(name = "coupon_cost_discount", nullable = false)
    private int couponCostDiscount;

    @Column(name = "coupon_cost_free_ride")
    private Integer couponCostFreeRide;

    @Min(0)
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
