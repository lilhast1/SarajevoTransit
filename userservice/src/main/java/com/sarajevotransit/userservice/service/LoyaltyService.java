package com.sarajevotransit.userservice.service;

import com.sarajevotransit.userservice.dto.LoyaltyBalanceResponse;
import com.sarajevotransit.userservice.dto.LoyaltyEarnRequest;
import com.sarajevotransit.userservice.dto.LoyaltyRedeemRequest;
import com.sarajevotransit.userservice.dto.LoyaltyTransactionResponse;
import com.sarajevotransit.userservice.exception.InsufficientLoyaltyPointsException;
import com.sarajevotransit.userservice.model.LoyaltyTransaction;
import com.sarajevotransit.userservice.model.LoyaltyTransactionType;
import com.sarajevotransit.userservice.model.UserProfile;
import com.sarajevotransit.userservice.repository.LoyaltyTransactionRepository;
import com.sarajevotransit.userservice.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoyaltyService {

    private final UserProfileRepository userProfileRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final UserService userService;

    public LoyaltyService(
            UserProfileRepository userProfileRepository,
            LoyaltyTransactionRepository loyaltyTransactionRepository,
            UserService userService) {
        this.userProfileRepository = userProfileRepository;
        this.loyaltyTransactionRepository = loyaltyTransactionRepository;
        this.userService = userService;
    }

    @Transactional
    public LoyaltyBalanceResponse earnPoints(Long userId, LoyaltyEarnRequest request) {
        UserProfile user = userService.findUserById(userId);
        if (request.points() <= 0) {
            throw new IllegalArgumentException("Points to earn must be greater than zero.");
        }

        user.setLoyaltyPointsBalance(user.getLoyaltyPointsBalance() + request.points());
        createTransaction(user, LoyaltyTransactionType.EARN, request.points(), request.description(),
                request.referenceType());
        userProfileRepository.save(user);

        return new LoyaltyBalanceResponse(user.getId(), user.getLoyaltyPointsBalance());
    }

    @Transactional
    public LoyaltyBalanceResponse redeemPoints(Long userId, LoyaltyRedeemRequest request) {
        UserProfile user = userService.findUserById(userId);
        if (request.points() <= 0) {
            throw new IllegalArgumentException("Points to redeem must be greater than zero.");
        }

        if (user.getLoyaltyPointsBalance() < request.points()) {
            throw new InsufficientLoyaltyPointsException("User does not have enough loyalty points for redemption.");
        }

        user.setLoyaltyPointsBalance(user.getLoyaltyPointsBalance() - request.points());
        createTransaction(user, LoyaltyTransactionType.REDEEM, request.points(), request.description(),
                request.referenceType());
        userProfileRepository.save(user);

        return new LoyaltyBalanceResponse(user.getId(), user.getLoyaltyPointsBalance());
    }

    @Transactional(readOnly = true)
    public LoyaltyBalanceResponse getBalance(Long userId) {
        UserProfile user = userService.findUserById(userId);
        return new LoyaltyBalanceResponse(user.getId(), user.getLoyaltyPointsBalance());
    }

    @Transactional(readOnly = true)
    public List<LoyaltyTransactionResponse> getTransactions(Long userId) {
        userService.findUserById(userId);
        return loyaltyTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(userService::toLoyaltyTransactionResponse)
                .toList();
    }

    private void createTransaction(UserProfile user, LoyaltyTransactionType type, int points, String description,
            String referenceType) {
        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setTransactionType(type);
        transaction.setPoints(points);
        transaction.setDescription(description.trim());
        transaction.setReferenceType(referenceType.trim());
        user.addLoyaltyTransaction(transaction);
        loyaltyTransactionRepository.save(transaction);
    }
}
