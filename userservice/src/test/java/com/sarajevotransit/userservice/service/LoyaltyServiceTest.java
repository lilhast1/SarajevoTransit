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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private LoyaltyTransactionRepository loyaltyTransactionRepository;

    @Mock
    private UserService userService;

    @Mock
    private LoyaltyTransactionMapper loyaltyTransactionMapper;

    @InjectMocks
    private LoyaltyService loyaltyService;

    @Test
    void earnPoints_shouldIncreaseBalanceAndPersistTransaction() {
        UserProfile user = buildUserWithPoints(1L, 10);
        when(userService.findUserById(1L)).thenReturn(user);
        when(loyaltyTransactionRepository.save(any(LoyaltyTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoyaltyBalanceResponse response = loyaltyService.earnPoints(
                1L,
                new LoyaltyEarnRequest(15, "  Ticket purchase bonus  ", "  ticket_purchase  "));

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.currentBalance()).isEqualTo(25);

        ArgumentCaptor<LoyaltyTransaction> transactionCaptor = ArgumentCaptor.forClass(LoyaltyTransaction.class);
        verify(loyaltyTransactionRepository).save(transactionCaptor.capture());
        LoyaltyTransaction transaction = transactionCaptor.getValue();
        assertThat(transaction.getPointsEarned()).isEqualTo(15);
        assertThat(transaction.getPointsSpent()).isZero();
        assertThat(transaction.getDescription()).isEqualTo("Ticket purchase bonus");
        assertThat(transaction.getReferenceType()).isEqualTo("ticket_purchase");

        verify(userProfileRepository).save(user);
    }

    @Test
    void redeemPoints_shouldThrowWhenBalanceIsInsufficient() {
        UserProfile user = buildUserWithPoints(2L, 5);
        when(userService.findUserById(2L)).thenReturn(user);

        assertThatThrownBy(() -> loyaltyService.redeemPoints(
                2L,
                new LoyaltyRedeemRequest(10, "Discount", "discount")))
                .isInstanceOf(InsufficientLoyaltyPointsException.class)
                .hasMessageContaining("enough loyalty points");

        verify(loyaltyTransactionRepository, never()).save(any(LoyaltyTransaction.class));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void redeemPoints_shouldDecreaseBalanceAndPersistTransaction() {
        UserProfile user = buildUserWithPoints(3L, 40);
        when(userService.findUserById(3L)).thenReturn(user);
        when(loyaltyTransactionRepository.save(any(LoyaltyTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoyaltyBalanceResponse response = loyaltyService.redeemPoints(
                3L,
                new LoyaltyRedeemRequest(15, "Ride discount", "discount"));

        assertThat(response.currentBalance()).isEqualTo(25);

        ArgumentCaptor<LoyaltyTransaction> transactionCaptor = ArgumentCaptor.forClass(LoyaltyTransaction.class);
        verify(loyaltyTransactionRepository).save(transactionCaptor.capture());
        LoyaltyTransaction transaction = transactionCaptor.getValue();
        assertThat(transaction.getPointsSpent()).isEqualTo(15);
        assertThat(transaction.getPointsEarned()).isZero();
    }

    @Test
    void getBalance_shouldCreateWalletIfMissing() {
        UserProfile user = new UserProfile();
        user.setId(4L);
        user.setFirstName("Mina");
        user.setLastName("Alic");
        user.setEmail("mina.alic@sarajevotransit.ba");
        user.setPasswordHash("hash");

        when(userService.findUserById(4L)).thenReturn(user);

        LoyaltyBalanceResponse response = loyaltyService.getBalance(4L);

        assertThat(response.userId()).isEqualTo(4L);
        assertThat(response.currentBalance()).isZero();
        assertThat(user.getWallet()).isNotNull();
    }

    @Test
    void getTransactions_shouldReturnMappedResponses() {
        UserProfile user = buildUserWithPoints(5L, 30);
        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setId(77L);
        transaction.setPointsEarned(10);
        transaction.setPointsSpent(0);
        transaction.setDescription("Bonus");
        transaction.setReferenceType("ticket_purchase");

        LoyaltyTransactionResponse response = new LoyaltyTransactionResponse(
                77L,
                LoyaltyTransactionType.EARN,
                10,
                10,
                0,
                "Bonus",
                "ticket_purchase",
                null,
                null,
                LocalDateTime.now());

        when(userService.findUserById(5L)).thenReturn(user);
        when(loyaltyTransactionRepository.findByUserIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(transaction));
        when(loyaltyTransactionMapper.toResponse(transaction)).thenReturn(response);

        List<LoyaltyTransactionResponse> responses = loyaltyService.getTransactions(5L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(77L);
    }

    private UserProfile buildUserWithPoints(Long id, int points) {
        UserProfile user = new UserProfile();
        user.setId(id);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test.user@sarajevotransit.ba");
        user.setPasswordHash("hash");

        DigitalWallet wallet = new DigitalWallet();
        wallet.setLoyaltyPointsTotal(points);
        user.setWallet(wallet);
        return user;
    }
}
