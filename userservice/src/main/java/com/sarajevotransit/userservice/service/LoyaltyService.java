package com.sarajevotransit.userservice.service;

import com.sarajevotransit.userservice.dto.LoyaltyBalanceResponse;
import com.sarajevotransit.userservice.dto.LoyaltyEarnRequest;
import com.sarajevotransit.userservice.dto.LoyaltyRedeemRequest;
import com.sarajevotransit.userservice.dto.LoyaltyTransactionResponse;
import com.sarajevotransit.userservice.exception.InsufficientLoyaltyPointsException;
import com.sarajevotransit.userservice.mapper.LoyaltyTransactionMapper;
import com.sarajevotransit.userservice.model.DigitalWallet;
import com.sarajevotransit.userservice.model.LoyaltyTransaction;
import com.sarajevotransit.userservice.model.LoyaltyTransactionType;
import com.sarajevotransit.userservice.model.UserProfile;
import com.sarajevotransit.userservice.repository.LoyaltyTransactionRepository;
import com.sarajevotransit.userservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

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
        return user.getWallet();
    }
}
