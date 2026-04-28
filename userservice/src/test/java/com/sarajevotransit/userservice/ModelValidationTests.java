package com.sarajevotransit.userservice;

import com.sarajevotransit.userservice.model.LoyaltyTransaction;
import com.sarajevotransit.userservice.model.TicketPurchaseHistoryEntry;
import com.sarajevotransit.userservice.model.TravelHistoryEntry;
import com.sarajevotransit.userservice.model.UserProfile;
import com.sarajevotransit.userservice.repository.LoyaltyTransactionRepository;
import com.sarajevotransit.userservice.repository.TicketPurchaseHistoryRepository;
import com.sarajevotransit.userservice.repository.TravelHistoryRepository;
import com.sarajevotransit.userservice.repository.UserProfileRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ModelValidationTests {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private TravelHistoryRepository travelHistoryRepository;

    @Autowired
    private TicketPurchaseHistoryRepository ticketPurchaseHistoryRepository;

    @Autowired
    private LoyaltyTransactionRepository loyaltyTransactionRepository;

    @BeforeEach
    void cleanDatabase() {
        userProfileRepository.deleteAll();
    }

    @Test
    void shouldRejectInvalidUserProfileModel() {
        UserProfile user = new UserProfile();
        user.setFirstName(" ");
        user.setLastName("Test");
        user.setEmail("not-an-email");
        user.setPasswordHash("");

        assertThatThrownBy(() -> userProfileRepository.saveAndFlush(user))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldRejectInvalidTravelHistoryModel() {
        TravelHistoryEntry entry = new TravelHistoryEntry();
        entry.setLineCode("");
        entry.setFromStop(" ");
        entry.setToStop("");
        entry.setDurationMinutes(0);

        assertThatThrownBy(() -> travelHistoryRepository.saveAndFlush(entry))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldRejectInvalidTicketPurchaseModel() {
        TicketPurchaseHistoryEntry entry = new TicketPurchaseHistoryEntry();
        entry.setAmount(new BigDecimal("0.00"));
        entry.setPaymentMethod(" ");
        entry.setExternalTransactionId("");
        entry.setLineCode("X".repeat(41));

        assertThatThrownBy(() -> ticketPurchaseHistoryRepository.saveAndFlush(entry))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldRejectInvalidLoyaltyTransactionModel() {
        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setPointsEarned(-1);
        transaction.setPointsSpent(-1);
        transaction.setDescription(" ");
        transaction.setReferenceType("");

        assertThatThrownBy(() -> loyaltyTransactionRepository.saveAndFlush(transaction))
                .isInstanceOf(ConstraintViolationException.class);
    }
}
