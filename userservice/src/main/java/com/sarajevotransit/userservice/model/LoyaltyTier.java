package com.sarajevotransit.userservice.model;

public enum LoyaltyTier {
    BRONZE(0, 0, false),
    SILVER(100, 5, false),
    GOLD(250, 10, false),
    PLATINUM(500, 15, true);

    private final int minimumLifetimePoints;
    private final int discountPercent;
    private final boolean freeRideEligible;

    LoyaltyTier(int minimumLifetimePoints, int discountPercent, boolean freeRideEligible) {
        this.minimumLifetimePoints = minimumLifetimePoints;
        this.discountPercent = discountPercent;
        this.freeRideEligible = freeRideEligible;
    }

    public int getMinimumLifetimePoints() {
        return minimumLifetimePoints;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public boolean isFreeRideEligible() {
        return freeRideEligible;
    }

    public static LoyaltyTier fromLifetimePoints(int lifetimePoints) {
        if (lifetimePoints >= PLATINUM.minimumLifetimePoints)
            return PLATINUM;
        if (lifetimePoints >= GOLD.minimumLifetimePoints)
            return GOLD;
        if (lifetimePoints >= SILVER.minimumLifetimePoints)
            return SILVER;
        return BRONZE;
    }
}