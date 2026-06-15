package com.sarajevotransit.userservice.mapper;

import com.sarajevotransit.userservice.dto.UserPreferenceResponse;
import com.sarajevotransit.userservice.dto.UserProfileResponse;
import com.sarajevotransit.userservice.model.UserPreference;
import com.sarajevotransit.userservice.model.UserProfile;
import com.sarajevotransit.userservice.service.LoyaltyTierConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileMapper {

    private final LoyaltyTierConfigService tierConfigService;

    public UserProfileResponse toResponse(UserProfile user) {
        if (user == null) {
            return null;
        }

        int lifetimePoints = user.getLoyaltyPointsLifetime();
        var tier = tierConfigService.getTierByLifetimePoints(lifetimePoints);
        String loyaltyTier = tier.getTierName();
        int discountPercent = tier.getDiscountPercent();
        boolean freeRideEligible = tier.isFreeRideEligible();

        UserPreferenceResponse preferenceResponse = null;
        if (user.getPreference() != null) {
            UserPreference pref = user.getPreference();
            preferenceResponse = new UserPreferenceResponse(
                    pref.getLanguageCode(),
                    pref.getThemeMode(),
                    pref.getNotificationChannel(),
                    pref.getHighContrastEnabled(),
                    pref.getLargeTextEnabled(),
                    pref.getScreenReaderEnabled(),
                    pref.getUpdatedAt());
        }

        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getLoyaltyPointsBalance(),
                lifetimePoints,
                loyaltyTier,
                discountPercent,
                freeRideEligible,
                preferenceResponse,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
