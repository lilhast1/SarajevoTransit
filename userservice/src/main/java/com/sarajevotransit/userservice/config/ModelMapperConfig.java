package com.sarajevotransit.userservice.config;

import com.sarajevotransit.userservice.dto.LoyaltyTransactionResponse;
import com.sarajevotransit.userservice.dto.TicketPurchaseResponse;
import com.sarajevotransit.userservice.dto.TravelHistoryResponse;
import com.sarajevotransit.userservice.dto.UserPreferenceResponse;
import com.sarajevotransit.userservice.dto.UserProfileResponse;
import com.sarajevotransit.userservice.model.LoyaltyTransaction;
import com.sarajevotransit.userservice.model.TicketPurchaseHistoryEntry;
import com.sarajevotransit.userservice.model.TravelHistoryEntry;
import com.sarajevotransit.userservice.model.UserPreference;
import com.sarajevotransit.userservice.model.UserProfile;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(AccessLevel.PRIVATE)
                .setSkipNullEnabled(true);

        modelMapper.createTypeMap(UserPreference.class, UserPreferenceResponse.class)
                .setConverter(context -> {
                    UserPreference source = context.getSource();
                    if (source == null) {
                        return null;
                    }
                    return new UserPreferenceResponse(
                            source.getLanguageCode(),
                            source.getThemeMode(),
                            source.getNotificationChannel(),
                            Boolean.TRUE.equals(source.getHighContrastEnabled()),
                            Boolean.TRUE.equals(source.getLargeTextEnabled()),
                            Boolean.TRUE.equals(source.getScreenReaderEnabled()),
                            source.getUpdatedAt());
                });

        modelMapper.createTypeMap(UserProfile.class, UserProfileResponse.class)
                .setConverter(context -> {
                    UserProfile source = context.getSource();
                    if (source == null) {
                        return null;
                    }
                    return new UserProfileResponse(
                            source.getId(),
                            source.getFullName(),
                            source.getEmail(),
                            source.getLoyaltyPointsBalance(),
                            modelMapper.map(source.getPreference(), UserPreferenceResponse.class),
                            source.getCreatedAt(),
                            source.getUpdatedAt());
                });

        modelMapper.createTypeMap(TravelHistoryEntry.class, TravelHistoryResponse.class)
                .setConverter(context -> {
                    TravelHistoryEntry source = context.getSource();
                    if (source == null) {
                        return null;
                    }
                    return new TravelHistoryResponse(
                            source.getId(),
                            source.getLineCode(),
                            source.getFromStop(),
                            source.getToStop(),
                            source.getTraveledAt(),
                            source.getDurationMinutes());
                });

        modelMapper.createTypeMap(TicketPurchaseHistoryEntry.class, TicketPurchaseResponse.class)
                .setConverter(context -> {
                    TicketPurchaseHistoryEntry source = context.getSource();
                    if (source == null) {
                        return null;
                    }
                    return new TicketPurchaseResponse(
                            source.getId(),
                            source.getTicketType(),
                            source.getAmount(),
                            source.getPaymentMethod(),
                            source.getExternalTransactionId(),
                            source.getLineCode(),
                            source.getPurchasedAt());
                });

        modelMapper.createTypeMap(LoyaltyTransaction.class, LoyaltyTransactionResponse.class)
                .setConverter(context -> {
                    LoyaltyTransaction source = context.getSource();
                    if (source == null) {
                        return null;
                    }
                    return new LoyaltyTransactionResponse(
                            source.getId(),
                            source.getTransactionType(),
                            source.getPoints(),
                            source.getPointsEarned() == null ? 0 : source.getPointsEarned(),
                            source.getPointsSpent() == null ? 0 : source.getPointsSpent(),
                            source.getDescription(),
                            source.getReferenceType(),
                            source.getTransactionId(),
                            source.getExpiryDate(),
                            source.getCreatedAt());
                });

        return modelMapper;
    }
}
