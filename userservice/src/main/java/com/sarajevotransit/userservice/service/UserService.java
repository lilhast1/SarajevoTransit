package com.sarajevotransit.userservice.service;

import com.sarajevotransit.userservice.dto.AddTicketPurchaseRequest;
import com.sarajevotransit.userservice.dto.AddTravelHistoryRequest;
import com.sarajevotransit.userservice.dto.LoyaltyTransactionResponse;
import com.sarajevotransit.userservice.dto.TicketPurchaseResponse;
import com.sarajevotransit.userservice.dto.TravelHistoryResponse;
import com.sarajevotransit.userservice.dto.UpdatePasswordRequest;
import com.sarajevotransit.userservice.dto.UpdateUserPreferenceRequest;
import com.sarajevotransit.userservice.dto.UpdateUserProfileRequest;
import com.sarajevotransit.userservice.dto.UserPreferenceResponse;
import com.sarajevotransit.userservice.dto.UserProfileResponse;
import com.sarajevotransit.userservice.dto.UserSummaryResponse;
import com.sarajevotransit.userservice.dto.CreateUserRequest;
import com.sarajevotransit.userservice.exception.DuplicateResourceException;
import com.sarajevotransit.userservice.exception.ResourceNotFoundException;
import com.sarajevotransit.userservice.model.LoyaltyTransaction;
import com.sarajevotransit.userservice.model.NotificationChannel;
import com.sarajevotransit.userservice.model.ThemeMode;
import com.sarajevotransit.userservice.model.LanguageCode;
import com.sarajevotransit.userservice.model.TicketPurchaseHistoryEntry;
import com.sarajevotransit.userservice.model.TravelHistoryEntry;
import com.sarajevotransit.userservice.model.UserPreference;
import com.sarajevotransit.userservice.model.UserProfile;
import com.sarajevotransit.userservice.repository.LoyaltyTransactionRepository;
import com.sarajevotransit.userservice.repository.TicketPurchaseHistoryRepository;
import com.sarajevotransit.userservice.repository.TravelHistoryRepository;
import com.sarajevotransit.userservice.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserProfileRepository userProfileRepository;
    private final TravelHistoryRepository travelHistoryRepository;
    private final TicketPurchaseHistoryRepository ticketPurchaseHistoryRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;

    public UserService(
            UserProfileRepository userProfileRepository,
            TravelHistoryRepository travelHistoryRepository,
            TicketPurchaseHistoryRepository ticketPurchaseHistoryRepository,
            LoyaltyTransactionRepository loyaltyTransactionRepository) {
        this.userProfileRepository = userProfileRepository;
        this.travelHistoryRepository = travelHistoryRepository;
        this.ticketPurchaseHistoryRepository = ticketPurchaseHistoryRepository;
        this.loyaltyTransactionRepository = loyaltyTransactionRepository;
    }

    @Transactional
    public UserProfileResponse createUser(CreateUserRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userProfileRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("A user with this email already exists.");
        }

        UserProfile user = new UserProfile();
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(hashPassword(request.password()));
        user.setLoyaltyPointsBalance(0);

        UserPreference preference = new UserPreference();
        preference.setLanguageCode(request.languageCode() != null ? request.languageCode() : LanguageCode.BS);
        preference.setThemeMode(request.themeMode() != null ? request.themeMode() : ThemeMode.SYSTEM);
        preference.setNotificationChannel(
                request.notificationChannel() != null ? request.notificationChannel() : NotificationChannel.PUSH);
        preference.setHighContrastEnabled(false);
        preference.setLargeTextEnabled(false);
        preference.setScreenReaderEnabled(false);
        user.setPreference(preference);

        UserProfile saved = userProfileRepository.save(user);
        return toUserProfileResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return userProfileRepository.findAll()
                .stream()
                .map(this::toUserProfileResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUser(Long userId) {
        UserProfile user = findUserById(userId);
        return toUserProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        UserProfile user = findUserById(userId);
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userProfileRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, userId)) {
            throw new DuplicateResourceException("Email is already used by another user.");
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        return toUserProfileResponse(userProfileRepository.save(user));
    }

    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        UserProfile user = findUserById(userId);
        user.setPasswordHash(hashPassword(request.newPassword()));
        userProfileRepository.save(user);
    }

    @Transactional
    public UserPreferenceResponse updatePreference(Long userId, UpdateUserPreferenceRequest request) {
        UserProfile user = findUserById(userId);
        UserPreference preference = user.getPreference();
        if (preference == null) {
            preference = new UserPreference();
            user.setPreference(preference);
        }

        preference.setLanguageCode(request.languageCode());
        preference.setThemeMode(request.themeMode());
        preference.setNotificationChannel(request.notificationChannel());
        preference.setHighContrastEnabled(request.highContrastEnabled());
        preference.setLargeTextEnabled(request.largeTextEnabled());
        preference.setScreenReaderEnabled(request.screenReaderEnabled());

        userProfileRepository.save(user);
        return toUserPreferenceResponse(preference);
    }

    @Transactional
    public TravelHistoryResponse addTravelHistory(Long userId, AddTravelHistoryRequest request) {
        UserProfile user = findUserById(userId);

        TravelHistoryEntry entry = new TravelHistoryEntry();
        entry.setLineCode(request.lineCode().trim());
        entry.setFromStop(request.fromStop().trim());
        entry.setToStop(request.toStop().trim());
        entry.setDurationMinutes(request.durationMinutes());
        entry.setTraveledAt(request.traveledAt() != null ? request.traveledAt() : LocalDateTime.now());

        user.addTravelHistoryEntry(entry);
        travelHistoryRepository.save(entry);
        return toTravelHistoryResponse(entry);
    }

    @Transactional
    public TicketPurchaseResponse addTicketPurchase(Long userId, AddTicketPurchaseRequest request) {
        UserProfile user = findUserById(userId);

        TicketPurchaseHistoryEntry entry = new TicketPurchaseHistoryEntry();
        entry.setTicketType(request.ticketType());
        entry.setAmount(request.amount());
        entry.setPaymentMethod(request.paymentMethod().trim());
        entry.setExternalTransactionId(request.externalTransactionId().trim());
        entry.setLineCode(request.lineCode() != null ? request.lineCode().trim() : null);
        entry.setPurchasedAt(request.purchasedAt() != null ? request.purchasedAt() : LocalDateTime.now());

        user.addTicketPurchase(entry);
        ticketPurchaseHistoryRepository.save(entry);
        return toTicketPurchaseResponse(entry);
    }

    @Transactional(readOnly = true)
    public List<String> getPersonalizedLineSuggestions(Long userId, int limit) {
        findUserById(userId);
        int normalizedLimit = Math.max(1, Math.min(limit, 10));

        Map<String, Integer> scoreByLine = new LinkedHashMap<>();

        travelHistoryRepository.findLineUsageStats(userId)
                .forEach(stat -> scoreByLine.merge(stat.getLineCode(), stat.getUsageCount().intValue() * 2,
                        Integer::sum));

        if (scoreByLine.isEmpty()) {
            return travelHistoryRepository.findByUserIdOrderByTraveledAtDesc(userId)
                    .stream()
                    .map(TravelHistoryEntry::getLineCode)
                    .distinct()
                    .limit(normalizedLimit)
                    .toList();
        }

        return scoreByLine.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .limit(normalizedLimit)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getUserSummary(Long userId) {
        UserProfile user = findUserById(userId);

        List<TravelHistoryResponse> travelHistory = travelHistoryRepository.findByUserIdOrderByTraveledAtDesc(userId)
                .stream()
                .map(this::toTravelHistoryResponse)
                .toList();

        List<TicketPurchaseResponse> purchases = ticketPurchaseHistoryRepository
                .findByUserIdOrderByPurchasedAtDesc(userId)
                .stream()
                .map(this::toTicketPurchaseResponse)
                .toList();

        List<LoyaltyTransactionResponse> transactions = loyaltyTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toLoyaltyTransactionResponse)
                .toList();

        List<String> suggestions = getPersonalizedLineSuggestions(userId, 3);

        return new UserSummaryResponse(
                toUserProfileResponse(user),
                travelHistory,
                purchases,
                transactions,
                suggestions);
    }

    @Transactional(readOnly = true)
    public UserProfile findUserById(Long userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found."));
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Password hashing algorithm is not available.", ex);
        }
    }

    public UserProfileResponse toUserProfileResponse(UserProfile user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getLoyaltyPointsBalance(),
                toUserPreferenceResponse(user.getPreference()),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public UserPreferenceResponse toUserPreferenceResponse(UserPreference preference) {
        if (preference == null) {
            return null;
        }

        return new UserPreferenceResponse(
                preference.getLanguageCode(),
                preference.getThemeMode(),
                preference.getNotificationChannel(),
                Boolean.TRUE.equals(preference.getHighContrastEnabled()),
                Boolean.TRUE.equals(preference.getLargeTextEnabled()),
                Boolean.TRUE.equals(preference.getScreenReaderEnabled()),
                preference.getUpdatedAt());
    }

    public TravelHistoryResponse toTravelHistoryResponse(TravelHistoryEntry entry) {
        return new TravelHistoryResponse(
                entry.getId(),
                entry.getLineCode(),
                entry.getFromStop(),
                entry.getToStop(),
                entry.getTraveledAt(),
                entry.getDurationMinutes());
    }

    public TicketPurchaseResponse toTicketPurchaseResponse(TicketPurchaseHistoryEntry entry) {
        return new TicketPurchaseResponse(
                entry.getId(),
                entry.getTicketType(),
                entry.getAmount(),
                entry.getPaymentMethod(),
                entry.getExternalTransactionId(),
                entry.getLineCode(),
                entry.getPurchasedAt());
    }

    public LoyaltyTransactionResponse toLoyaltyTransactionResponse(LoyaltyTransaction transaction) {
        return new LoyaltyTransactionResponse(
                transaction.getId(),
                transaction.getTransactionType(),
                transaction.getPoints(),
                transaction.getDescription(),
                transaction.getReferenceType(),
                transaction.getCreatedAt());
    }
}
