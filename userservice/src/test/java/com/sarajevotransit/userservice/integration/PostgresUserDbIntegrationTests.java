package com.sarajevotransit.userservice.integration;

import com.sarajevotransit.userservice.dto.AddTravelHistoryRequest;
import com.sarajevotransit.userservice.dto.CreateUserRequest;
import com.sarajevotransit.userservice.dto.UserProfileResponse;
import com.sarajevotransit.userservice.model.LanguageCode;
import com.sarajevotransit.userservice.model.NotificationChannel;
import com.sarajevotransit.userservice.model.ThemeMode;
import com.sarajevotransit.userservice.repository.TravelHistoryRepository;
import com.sarajevotransit.userservice.repository.UserProfileRepository;
import com.sarajevotransit.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("postgres-it")
@Transactional
class PostgresUserDbIntegrationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserService userService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private TravelHistoryRepository travelHistoryRepository;

    @Test
    void shouldUseRealPostgresConnection() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName())
                    .containsIgnoringCase("PostgreSQL");
            assertThat(connection.getMetaData().getURL())
                    .contains("jdbc:postgresql://localhost:5432/");
        }
    }

    @Test
    void shouldPersistAndReadRealDataInUserDb() {
        String email = "realdb." + System.currentTimeMillis() + "@sarajevotransit.ba";

        UserProfileResponse created = userService.createUser(new CreateUserRequest(
                "Real Db User",
                email,
                "StrongPass123",
                LanguageCode.EN,
                ThemeMode.SYSTEM,
                NotificationChannel.PUSH));

        userService.addTravelHistory(created.id(), new AddTravelHistoryRequest(
                "TRAM-1",
                "Skenderija",
                "Marijin Dvor",
                LocalDateTime.now().minusHours(1),
                11));

        assertThat(userProfileRepository.findById(created.id())).isPresent();
        assertThat(travelHistoryRepository.findByUserIdOrderByTraveledAtDesc(created.id()))
                .hasSize(1);
    }

    @Test
    void shouldHaveSeededDataAvailableInRealDatabase() {
        assertThat(userProfileRepository.count())
                .as("DataSeeder should seed users when the database starts empty")
                .isGreaterThan(0L);
    }
}
