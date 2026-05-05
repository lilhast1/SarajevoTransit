package com.sarajevotransit.userservice.config;

import com.sarajevotransit.userservice.dto.AddTicketPurchaseRequest;
import com.sarajevotransit.userservice.dto.AddTravelHistoryRequest;
import com.sarajevotransit.userservice.dto.CreateUserRequest;
import com.sarajevotransit.userservice.dto.LoyaltyEarnRequest;
import com.sarajevotransit.userservice.dto.LoyaltyRedeemRequest;
import com.sarajevotransit.userservice.model.LanguageCode;
import com.sarajevotransit.userservice.model.NotificationChannel;
import com.sarajevotransit.userservice.model.ThemeMode;
import com.sarajevotransit.userservice.model.TicketType;
import com.sarajevotransit.userservice.model.UserRole;
import com.sarajevotransit.userservice.repository.UserProfileRepository;
import com.sarajevotransit.userservice.service.LoyaltyService;
import com.sarajevotransit.userservice.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class DataSeeder {

        @Bean
        CommandLineRunner seedData(
                        UserProfileRepository userProfileRepository,
                        UserService userService,
                        LoyaltyService loyaltyService) {
                return args -> {
                        if (userProfileRepository.count() > 0) {
                                return;
                        }

                        // Admin user — upgrade role after creation
                        var adminResponse = userService.createUser(new CreateUserRequest(
                                        "Admin User",
                                        "admin@sarajevotransit.ba",
                                        "AdminPass123!",
                                        LanguageCode.BS,
                                        ThemeMode.LIGHT,
                                        NotificationChannel.PUSH));
                        userProfileRepository.findById(adminResponse.id()).ifPresent(admin -> {
                                admin.setRole(UserRole.ADMIN);
                                userProfileRepository.save(admin);
                        });

                        var amina = userService.createUser(new CreateUserRequest(
                                        "Amina Hadzic",
                                        "amina.hadzic@sarajevotransit.ba",
                                        "AminaPass123",
                                        LanguageCode.BS,
                                        ThemeMode.LIGHT,
                                        NotificationChannel.PUSH));

                        userService.addTravelHistory(amina.id(), new AddTravelHistoryRequest(
                                        "TRAM-3",
                                        "Skenderija",
                                        "Bascarsija",
                                        LocalDateTime.now().minusDays(3),
                                        18));

                        userService.addTravelHistory(amina.id(), new AddTravelHistoryRequest(
                                        "BUS-31E",
                                        "Nedzarici",
                                        "Dobrinja",
                                        LocalDateTime.now().minusDays(1),
                                        22));

                        userService.addTicketPurchase(amina.id(), new AddTicketPurchaseRequest(
                                        TicketType.MONTHLY,
                                        new BigDecimal("53.00"),
                                        "CARD",
                                        "TXN-AMINA-0001",
                                        "TRAM-3",
                                        LocalDateTime.now().minusDays(5)));

                        loyaltyService.earnPoints(amina.id(), new LoyaltyEarnRequest(
                                        120,
                                        "Monthly ticket purchase",
                                        "ticket_purchase"));

                        loyaltyService.redeemPoints(amina.id(), new LoyaltyRedeemRequest(
                                        30,
                                        "Loyalty discount for next ride",
                                        "discount"));

                        var tar = userService.createUser(new CreateUserRequest(
                                        "Tarik Kovac",
                                        "tarik.kovac@sarajevotransit.ba",
                                        "TarikPass123",
                                        LanguageCode.EN,
                                        ThemeMode.DARK,
                                        NotificationChannel.EMAIL));

                        userService.addTravelHistory(tar.id(), new AddTravelHistoryRequest(
                                        "TROL-103",
                                        "Trg Austrije",
                                        "Jezero",
                                        LocalDateTime.now().minusDays(2),
                                        27));

                        userService.addTicketPurchase(tar.id(), new AddTicketPurchaseRequest(
                                        TicketType.WEEKLY,
                                        new BigDecimal("17.50"),
                                        "PAYPAL",
                                        "TXN-TARIK-0001",
                                        "TROL-103",
                                        LocalDateTime.now().minusDays(4)));

                        loyaltyService.earnPoints(tar.id(), new LoyaltyEarnRequest(
                                        45,
                                        "Weekly ticket purchase",
                                        "ticket_purchase"));
                };
        }
}
