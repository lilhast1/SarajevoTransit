package com.sarajevotransit.userservice.repository;

import com.sarajevotransit.userservice.model.LoyaltyTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {

    List<LoyaltyTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<LoyaltyTransaction> findByUserId(Long userId, Pageable pageable);

    List<LoyaltyTransaction> findByUserIdAndCouponCodeIsNotNullOrderByCreatedAtDesc(Long userId);

    Optional<LoyaltyTransaction> findByUserIdAndCouponCodeIgnoreCase(Long userId, String couponCode);
}
