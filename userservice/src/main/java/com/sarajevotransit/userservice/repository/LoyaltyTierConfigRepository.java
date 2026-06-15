package com.sarajevotransit.userservice.repository;

import com.sarajevotransit.userservice.model.LoyaltyTierConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoyaltyTierConfigRepository extends JpaRepository<LoyaltyTierConfig, Long> {

    List<LoyaltyTierConfig> findAllByOrderByMinimumLifetimePointsAsc();

    Optional<LoyaltyTierConfig> findByTierNameIgnoreCase(String tierName);
}
