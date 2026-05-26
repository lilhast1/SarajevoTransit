package com.sarajevotransit.userservice.service;

import com.sarajevotransit.userservice.dto.LoyaltyBalanceResponse;
import com.sarajevotransit.userservice.dto.ApplyCouponRequest;
import com.sarajevotransit.userservice.dto.CouponApplicationResponse;
import com.sarajevotransit.userservice.dto.GenerateLoyaltyCouponRequest;
import com.sarajevotransit.userservice.dto.LoyaltyEarnRequest;
import com.sarajevotransit.userservice.dto.LoyaltyCouponResponse;
import com.sarajevotransit.userservice.dto.LoyaltyRedeemRequest;
import com.sarajevotransit.userservice.dto.LoyaltyTransactionResponse;
import com.sarajevotransit.userservice.exception.InsufficientLoyaltyPointsException;
import com.sarajevotransit.userservice.mapper.LoyaltyTransactionMapper;
import com.sarajevotransit.userservice.model.DigitalWallet;
import com.sarajevotransit.userservice.model.LoyaltyCouponType;
import com.sarajevotransit.userservice.model.LoyaltyTransaction;
import com.sarajevotransit.userservice.model.LoyaltyTransactionType;
import com.sarajevotransit.userservice.model.LoyaltyTier;
import com.sarajevotransit.userservice.model.UserProfile;
import com.sarajevotransit.userservice.repository.LoyaltyTransactionRepository;
import com.sarajevotransit.userservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final UserProfileRepository userProfileRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final UserService userService;
    private final LoyaltyTransactionMapper loyaltyTransactionMapper;

    @Transactional
    public LoyaltyBalanceResponse earnPoints(Long userId, LoyaltyEarnRequest request) {
        UserProfile user = userService.findUserById(userId);
        DigitalWallet wallet = getOrCreateWallet(user);
        wallet.setLoyaltyPointsTotal(wallet.getLoyaltyPointsTotal() + request.points());
        wallet.setLoyaltyPointsLifetime(wallet.getLoyaltyPointsLifetime() + request.points());
        createTransaction(user, LoyaltyTransactionType.EARN, request.points(), request.description(),
                request.referenceType());
        userProfileRepository.save(user);

        return new LoyaltyBalanceResponse(user.getId(), wallet.getLoyaltyPointsTotal());
    }

    @Transactional
    public LoyaltyBalanceResponse redeemPoints(Long userId, LoyaltyRedeemRequest request) {
        UserProfile user = userService.findUserById(userId);
        DigitalWallet wallet = getOrCreateWallet(user);
        if (wallet.getLoyaltyPointsTotal() < request.points()) {
            throw new InsufficientLoyaltyPointsException("User does not have enough loyalty points for redemption.");
        }

        wallet.setLoyaltyPointsTotal(wallet.getLoyaltyPointsTotal() - request.points());
        createTransaction(user, LoyaltyTransactionType.REDEEM, request.points(), request.description(),
                request.referenceType());
        userProfileRepository.save(user);

        return new LoyaltyBalanceResponse(user.getId(), wallet.getLoyaltyPointsTotal());
    }

    @Transactional
    public LoyaltyCouponResponse generateCoupon(Long userId, GenerateLoyaltyCouponRequest request) {
        UserProfile user = userService.findUserById(userId);
        DigitalWallet wallet = getOrCreateWallet(user);
        LoyaltyTier tier = LoyaltyTier.fromLifetimePoints(wallet.getLoyaltyPointsLifetime());

        int pointsCost = resolveCouponCost(tier, request.couponType());
        if (wallet.getLoyaltyPointsTotal() < pointsCost) {
            throw new InsufficientLoyaltyPointsException("User does not have enough loyalty points for this coupon.");
        }

        String rideCode = request.rideCode() == null ? null : request.rideCode().trim().toUpperCase();
        if (request.couponType() == LoyaltyCouponType.FREE_RIDE) {
            if (!tier.isFreeRideEligible()) {
                throw new IllegalArgumentException("Free ride coupons are only available at the highest loyalty tier.");
            }
            if (rideCode == null || rideCode.isBlank()) {
                throw new IllegalArgumentException("A ride must be selected for a free ride coupon.");
            }
        }

        wallet.setLoyaltyPointsTotal(wallet.getLoyaltyPointsTotal() - pointsCost);

        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setPointsSpent(pointsCost);
        transaction.setPointsEarned(0);
        transaction.setDescription(buildCouponDescription(request.couponType(), tier, rideCode));
        transaction.setReferenceType("COUPON_GENERATED");
        transaction.setCouponCode(generateCouponCode(userId, request.couponType(), tier));
        transaction.setCouponType(request.couponType());
        transaction.setCouponTier(tier);
        transaction.setCouponDiscountPercent(resolveCouponDiscountPercent(tier, request.couponType()));
        transaction.setCouponRideCode(rideCode);
        transaction.setCouponActive(true);
        transaction.setExpiryDate(LocalDate.now().plusDays(30));
        user.addLoyaltyTransaction(transaction);
        loyaltyTransactionRepository.save(transaction);
        userProfileRepository.save(user);

        return toCouponResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<LoyaltyCouponResponse> getCoupons(Long userId) {
        userService.findUserById(userId);
        return loyaltyTransactionRepository.findByUserIdAndCouponCodeIsNotNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toCouponResponse)
                .toList();
    }

    @Transactional
    public CouponApplicationResponse applyCoupon(Long userId, String couponCode, ApplyCouponRequest request) {
        UserProfile user = userService.findUserById(userId);
        LoyaltyTransaction coupon = loyaltyTransactionRepository
                .findByUserIdAndCouponCodeIgnoreCase(userId, couponCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found."));

        if (Boolean.FALSE.equals(coupon.getCouponActive())) {
            throw new IllegalArgumentException("Coupon has already been used.");
        }
        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now())) {
            coupon.setCouponActive(false);
            loyaltyTransactionRepository.save(coupon);
            throw new IllegalArgumentException("Coupon has expired.");
        }

        if (coupon.getCouponType() == LoyaltyCouponType.FREE_RIDE) {
            String rideCode = request == null || request.rideCode() == null ? null : request.rideCode().trim().toUpperCase();
            if (rideCode == null || !rideCode.equalsIgnoreCase(coupon.getCouponRideCode())) {
                throw new IllegalArgumentException("This free ride coupon is only valid for the selected ride.");
            }
        }

        coupon.setCouponActive(false);
        coupon.setCouponRedeemedAt(LocalDateTime.now());
        loyaltyTransactionRepository.save(coupon);

        return new CouponApplicationResponse(
                coupon.getCouponCode(),
                coupon.getCouponType(),
                coupon.getCouponTier(),
                coupon.getCouponDiscountPercent() == null ? 0 : coupon.getCouponDiscountPercent(),
                coupon.getCouponRideCode(),
                coupon.getCouponType() == LoyaltyCouponType.FREE_RIDE
        );
    }

    @Transactional(readOnly = true)
    public LoyaltyBalanceResponse getBalance(Long userId) {
        UserProfile user = userService.findUserById(userId);
        DigitalWallet wallet = getOrCreateWallet(user);
        return new LoyaltyBalanceResponse(user.getId(), wallet.getLoyaltyPointsTotal());
    }

    @Transactional(readOnly = true)
    public List<LoyaltyTransactionResponse> getTransactions(Long userId) {
        userService.findUserById(userId);
        return loyaltyTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(loyaltyTransactionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<LoyaltyTransactionResponse> getTransactions(Long userId, int page, int size, String sort) {
        userService.findUserById(userId);
        Pageable pageable = PaginationUtils.buildPageable(
                page,
                size,
                sort,
                "createdAt",
                Sort.Direction.DESC,
                Set.of("id", "transactionType", "pointsEarned", "pointsSpent", "description", "referenceType",
                        "createdAt", "expiryDate"));

        return loyaltyTransactionRepository.findByUserId(userId, pageable)
                .map(loyaltyTransactionMapper::toResponse);
    }

    private LoyaltyCouponResponse toCouponResponse(LoyaltyTransaction transaction) {
        return new LoyaltyCouponResponse(
                transaction.getCouponCode(),
                transaction.getCouponType(),
                transaction.getCouponTier(),
                transaction.getCouponDiscountPercent() == null ? 0 : transaction.getCouponDiscountPercent(),
                transaction.getCouponRideCode(),
                transaction.getPointsSpent() == null ? 0 : transaction.getPointsSpent(),
                Boolean.TRUE.equals(transaction.getCouponActive()),
                transaction.getExpiryDate(),
                transaction.getCouponRedeemedAt(),
                transaction.getCreatedAt());
    }

    private int resolveCouponCost(LoyaltyTier tier, LoyaltyCouponType type) {
        return switch (type) {
            case DISCOUNT -> switch (tier) {
                case SILVER -> 100;
                case GOLD -> 200;
                case PLATINUM -> 300;
                default -> throw new IllegalArgumentException("Coupons are available from silver tier and above.");
            };
            case FREE_RIDE -> {
                if (tier != LoyaltyTier.PLATINUM) {
                    throw new IllegalArgumentException("Free ride coupons are only available at platinum tier.");
                }
                yield 500;
            }
        };
    }

    private int resolveCouponDiscountPercent(LoyaltyTier tier, LoyaltyCouponType type) {
        if (type == LoyaltyCouponType.FREE_RIDE) {
            return 100;
        }
        return tier.getDiscountPercent();
    }

    private String generateCouponCode(Long userId, LoyaltyCouponType type, LoyaltyTier tier) {
        return "CP-" + tier.name().substring(0, 3) + "-" + type.name().substring(0, 3) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String buildCouponDescription(LoyaltyCouponType type, LoyaltyTier tier, String rideCode) {
        if (type == LoyaltyCouponType.FREE_RIDE) {
            return "Free ride coupon for " + rideCode + " at " + tier.name().toLowerCase() + " tier";
        }
        return tier.name().toLowerCase() + " tier discount coupon";
    }

    private void createTransaction(UserProfile user, LoyaltyTransactionType type, int points, String description,
            String referenceType) {
        LoyaltyTransaction transaction = new LoyaltyTransaction();
        if (type == LoyaltyTransactionType.REDEEM) {
            transaction.setPointsSpent(points);
            transaction.setPointsEarned(0);
        } else {
            transaction.setPointsEarned(points);
            transaction.setPointsSpent(0);
        }
        transaction.setDescription(description.trim());
        transaction.setReferenceType(referenceType.trim());
        user.addLoyaltyTransaction(transaction);
        loyaltyTransactionRepository.save(transaction);
    }

    private DigitalWallet getOrCreateWallet(UserProfile user) {
        if (user.getWallet() == null) {
            user.setWallet(new DigitalWallet());
        }
        if (user.getWallet().getLoyaltyPointsTotal() == null) {
            user.getWallet().setLoyaltyPointsTotal(0);
        }
        if (user.getWallet().getLoyaltyPointsLifetime() == null) {
            user.getWallet().setLoyaltyPointsLifetime(0);
        }
        return user.getWallet();
    }
}
