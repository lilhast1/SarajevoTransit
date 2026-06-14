package com.sarajevotransit.userservice.service;

import com.sarajevotransit.userservice.dto.LoyaltyTierConfigRequest;
import com.sarajevotransit.userservice.dto.LoyaltyTierConfigResponse;
import com.sarajevotransit.userservice.exception.ResourceNotFoundException;
import com.sarajevotransit.userservice.model.LoyaltyTier;
import com.sarajevotransit.userservice.model.LoyaltyTierConfig;
import com.sarajevotransit.userservice.repository.LoyaltyTierConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoyaltyTierConfigService {

    private final LoyaltyTierConfigRepository repository;

    @PostConstruct
    @Transactional
    public void seedDefaults() {
        if (repository.count() > 0) return;

        for (LoyaltyTier tier : LoyaltyTier.values()) {
            LoyaltyTierConfig config = new LoyaltyTierConfig();
            config.setTierName(tier.name());
            config.setMinimumLifetimePoints(tier.getMinimumLifetimePoints());
            config.setDiscountPercent(tier.getDiscountPercent());
            config.setFreeRideEligible(tier.isFreeRideEligible());
            switch (tier) {
                case SILVER -> { config.setCouponCostDiscount(100); config.setCouponCostFreeRide(null); }
                case GOLD ->   { config.setCouponCostDiscount(200); config.setCouponCostFreeRide(null); }
                case PLATINUM -> { config.setCouponCostDiscount(300); config.setCouponCostFreeRide(500); }
                default ->     { config.setCouponCostDiscount(0); config.setCouponCostFreeRide(null); }
            }
            config.setSortOrder(tier.ordinal());
            repository.save(config);
        }
    }

    public LoyaltyTierConfig getTierByLifetimePoints(int lifetimePoints) {
        List<LoyaltyTierConfig> tiers = repository.findAllByOrderByMinimumLifetimePointsAsc();
        LoyaltyTierConfig best = tiers.getFirst();
        for (LoyaltyTierConfig tier : tiers) {
            if (lifetimePoints >= tier.getMinimumLifetimePoints()) {
                best = tier;
            }
        }
        return best;
    }

    @Transactional(readOnly = true)
    public List<LoyaltyTierConfigResponse> getAllTiers() {
        return repository.findAllByOrderByMinimumLifetimePointsAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LoyaltyTierConfigResponse updateTier(Long id, LoyaltyTierConfigRequest request) {
        LoyaltyTierConfig config = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tier config not found with id " + id));

        config.setTierName(request.tierName());
        config.setMinimumLifetimePoints(request.minimumLifetimePoints());
        config.setDiscountPercent(request.discountPercent());
        config.setFreeRideEligible(request.freeRideEligible());
        config.setCouponCostDiscount(request.couponCostDiscount());
        config.setCouponCostFreeRide(request.couponCostFreeRide());
        config.setSortOrder(request.sortOrder());

        return toResponse(repository.save(config));
    }

    private LoyaltyTierConfigResponse toResponse(LoyaltyTierConfig config) {
        return new LoyaltyTierConfigResponse(
                config.getId(),
                config.getTierName(),
                config.getMinimumLifetimePoints(),
                config.getDiscountPercent(),
                config.isFreeRideEligible(),
                config.getCouponCostDiscount(),
                config.getCouponCostFreeRide(),
                config.getSortOrder(),
                config.getUpdatedAt());
    }
}
