package com.sarajevotransit.userservice.repository;

import com.sarajevotransit.userservice.dto.AddTicketPurchaseRequest;
import com.sarajevotransit.userservice.dto.AddTravelHistoryRequest;
import com.sarajevotransit.userservice.dto.CreateUserRequest;
import com.sarajevotransit.userservice.dto.LoyaltyEarnRequest;
import com.sarajevotransit.userservice.dto.LoyaltyRedeemRequest;
import com.sarajevotransit.userservice.model.LanguageCode;
import com.sarajevotransit.userservice.model.NotificationChannel;
import com.sarajevotransit.userservice.model.ThemeMode;
import com.sarajevotransit.userservice.model.TicketPurchaseHistoryEntry;
import com.sarajevotransit.userservice.model.TicketType;
import com.sarajevotransit.userservice.model.TravelHistoryEntry;
import com.sarajevotransit.userservice.model.UserProfile;
import com.sarajevotransit.userservice.service.LoyaltyService;
import com.sarajevotransit.userservice.service.UserService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RepositoryQueryPerformanceTests {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private TravelHistoryRepository travelHistoryRepository;

    @Autowired
    private TicketPurchaseHistoryRepository ticketPurchaseHistoryRepository;

    @Autowired
    private LoyaltyTransactionRepository loyaltyTransactionRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;
    private Long primaryUserId;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        userProfileRepository.deleteAll();
        seedData();
    }

    @Test
    void findAllWithWalletAndPreference_shouldNotHaveNPlusOneSelects() {
        statistics.clear();

        List<UserProfile> users = userProfileRepository.findAllWithWalletAndPreference();

        assertThat(users).hasSize(3);
        users.forEach(user -> {
            assertThat(user.getPreference()).isNotNull();
            assertThat(user.getLoyaltyPointsBalance()).isNotNull();
        });

        assertThat(statistics.getPrepareStatementCount())
                .as("Fetching users with wallet and preference should not trigger N+1 selects")
                .isLessThanOrEqualTo(2);
    }

    @Test
    void findByUserIdOrderByTraveledAtDesc_shouldExecuteSingleStatement() {
        statistics.clear();

        List<TravelHistoryEntry> entries = travelHistoryRepository.findByUserIdOrderByTraveledAtDesc(primaryUserId);

        assertThat(entries).hasSize(3);
        assertThat(statistics.getPrepareStatementCount())
                .as("Travel history repository query should use one select")
                .isLessThanOrEqualTo(1);
    }

    @Test
    void findByUserIdOrderByPurchasedAtDesc_shouldExecuteSingleStatement() {
        statistics.clear();

        List<TicketPurchaseHistoryEntry> entries = ticketPurchaseHistoryRepository
                .findByUserIdOrderByPurchasedAtDesc(primaryUserId);

        assertThat(entries).hasSize(2);
        assertThat(statistics.getPrepareStatementCount())
                .as("Ticket purchase repository query should use one select")
                .isLessThanOrEqualTo(1);
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_shouldExecuteSingleStatement() {
        statistics.clear();

        var transactions = loyaltyTransactionRepository.findByUserIdOrderByCreatedAtDesc(primaryUserId);

        assertThat(transactions).hasSize(2);
        assertThat(statistics.getPrepareStatementCount())
                .as("Loyalty transaction repository query should use one select")
                .isLessThanOrEqualTo(1);
    }

    private void seedData() {
        var amina = userService.createUser(new CreateUserRequest(
                "Amina Hadzic",
                "amina.hadzic@sarajevotransit.ba",
                "AminaPass123",
                LanguageCode.BS,
                ThemeMode.LIGHT,
                NotificationChannel.PUSH));

        var tar = userService.createUser(new CreateUserRequest(
                "Tarik Kovac",
                "tarik.kovac@sarajevotransit.ba",
                "TarikPass123",
                LanguageCode.EN,
                ThemeMode.DARK,
                NotificationChannel.EMAIL));

        userService.createUser(new CreateUserRequest(
                "Lejla Music",
                "lejla.music@sarajevotransit.ba",
                "LejlaPass123",
                LanguageCode.BS,
                ThemeMode.SYSTEM,
                NotificationChannel.PUSH));

        primaryUserId = amina.id();

        userService.addTravelHistory(primaryUserId, new AddTravelHistoryRequest(
                "TRAM-3",
                "Skenderija",
                "Bascarsija",
                LocalDateTime.now().minusDays(3),
                18));

        userService.addTravelHistory(primaryUserId, new AddTravelHistoryRequest(
                "BUS-31E",
                "Nedzarici",
                "Dobrinja",
                LocalDateTime.now().minusDays(2),
                22));

        userService.addTravelHistory(primaryUserId, new AddTravelHistoryRequest(
                "TRAM-3",
                "Bascarsija",
                "Skenderija",
                LocalDateTime.now().minusDays(1),
                17));

        userService.addTicketPurchase(primaryUserId, new AddTicketPurchaseRequest(
                TicketType.MONTHLY,
                new BigDecimal("53.00"),
                "CARD",
                "TXN-AMINA-0001",
                "TRAM-3",
                LocalDateTime.now().minusDays(4)));

        userService.addTicketPurchase(primaryUserId, new AddTicketPurchaseRequest(
                TicketType.DAILY,
                new BigDecimal("2.00"),
                "CARD",
                "TXN-AMINA-0002",
                "BUS-31E",
                LocalDateTime.now().minusDays(1)));

        loyaltyService.earnPoints(primaryUserId, new LoyaltyEarnRequest(
                120,
                "Monthly ticket purchase",
                "ticket_purchase"));

        loyaltyService.redeemPoints(primaryUserId, new LoyaltyRedeemRequest(
                30,
                "Loyalty discount for next ride",
                "discount"));

        loyaltyService.earnPoints(tar.id(), new LoyaltyEarnRequest(
                45,
                "Weekly ticket purchase",
                "ticket_purchase"));
    }
}
