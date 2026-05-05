package com.sarajevotransit.userservice.service;

import com.sarajevotransit.userservice.dto.AddTicketPurchaseRequest;
import com.sarajevotransit.userservice.dto.AddTravelHistoryRequest;
import com.sarajevotransit.userservice.dto.LoyaltyTransactionResponse;
import com.sarajevotransit.userservice.dto.PatchUserProfileRequest;
import com.sarajevotransit.userservice.dto.TicketPurchaseResponse;
import com.sarajevotransit.userservice.dto.TicketPurchaseStatsResponse;
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
import com.sarajevotransit.userservice.mapper.LoyaltyTransactionMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.sarajevotransit.userservice.mapper.TicketPurchaseMapper;
import com.sarajevotransit.userservice.mapper.TravelHistoryMapper;
import com.sarajevotransit.userservice.mapper.UserPreferenceMapper;
import com.sarajevotransit.userservice.mapper.UserProfileMapper;
import com.sarajevotransit.userservice.model.LoyaltyTransaction;
import com.sarajevotransit.userservice.model.NotificationChannel;
import com.sarajevotransit.userservice.model.ThemeMode;
import com.sarajevotransit.userservice.model.LanguageCode;
import com.sarajevotransit.userservice.model.DigitalWallet;
import com.sarajevotransit.userservice.model.TicketPurchaseHistoryEntry;
import com.sarajevotransit.userservice.model.TravelHistoryEntry;
import com.sarajevotransit.userservice.model.UserPreference;
import com.sarajevotransit.userservice.model.UserProfile;
import com.sarajevotransit.userservice.repository.LoyaltyTransactionRepository;
import com.sarajevotransit.userservice.repository.TicketPurchaseHistoryRepository;
import com.sarajevotransit.userservice.repository.TravelHistoryRepository;
import com.sarajevotransit.userservice.repository.UserProfileRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository userProfileRepository;
    private final TravelHistoryRepository travelHistoryRepository;
    private final TicketPurchaseHistoryRepository ticketPurchaseHistoryRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final UserProfileMapper userProfileMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final TravelHistoryMapper travelHistoryMapper;
    private final TicketPurchaseMapper ticketPurchaseMapper;
    private final LoyaltyTransactionMapper loyaltyTransactionMapper;
    private final Validator validator;
    private final PasswordEncoder passwordEncoder;
    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

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
        DigitalWallet wallet = new DigitalWallet();
        wallet.setLoyaltyPointsTotal(0);
        user.setWallet(wallet);

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
        return userProfileMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return userProfileRepository.findAllWithWalletAndPreference()
                .stream()
                .map(userProfileMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllUsers(int page, int size, String sort) {
        Pageable pageable = PaginationUtils.buildPageable(
                page,
                size,
                sort,
                "createdAt",
                Sort.Direction.DESC,
                Set.of("id", "firstName", "lastName", "email", "createdAt", "updatedAt"));

        return userProfileRepository.findAllWithWalletAndPreference(pageable)
                .map(userProfileMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUser(Long userId) {
        UserProfile user = findUserById(userId);
        return userProfileMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserPreferenceResponse getPreference(Long userId) {
        UserProfile user = findUserById(userId);
        return userPreferenceMapper.toResponse(user.getPreference());
    }

    @Transactional(readOnly = true)
    public List<TravelHistoryResponse> getTravelHistory(Long userId) {
        findUserById(userId);
        return travelHistoryRepository.findByUserIdOrderByTraveledAtDesc(userId)
                .stream()
                .map(travelHistoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TravelHistoryResponse> getTravelHistory(Long userId, int page, int size, String sort) {
        findUserById(userId);
        Pageable pageable = PaginationUtils.buildPageable(
                page,
                size,
                sort,
                "traveledAt",
                Sort.Direction.DESC,
                Set.of("id", "lineCode", "fromStop", "toStop", "traveledAt", "durationMinutes"));

        return travelHistoryRepository.findByUserId(userId, pageable)
                .map(travelHistoryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TicketPurchaseResponse> getTicketPurchases(Long userId) {
        findUserById(userId);
        return ticketPurchaseHistoryRepository.findByUserIdOrderByPurchasedAtDesc(userId)
                .stream()
                .map(ticketPurchaseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TicketPurchaseResponse> getTicketPurchases(Long userId, int page, int size, String sort) {
        findUserById(userId);
        Pageable pageable = PaginationUtils.buildPageable(
                page,
                size,
                sort,
                "purchasedAt",
                Sort.Direction.DESC,
                Set.of("id", "ticketType", "amount", "paymentMethod", "externalTransactionId", "lineCode",
                        "purchasedAt"));

        return ticketPurchaseHistoryRepository.findByUserId(userId, pageable)
                .map(ticketPurchaseMapper::toResponse);
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
        return userProfileMapper.toResponse(userProfileRepository.save(user));
    }

    @Transactional
    public UserProfileResponse patchUserProfile(Long userId, String patchDocument) {
        if (patchDocument == null || patchDocument.isBlank()) {
            throw new IllegalArgumentException("Patch document is required.");
        }

        UserProfile user = findUserById(userId);
        PatchUserProfileRequest current = new PatchUserProfileRequest(user.getFullName(), user.getEmail());
        PatchUserProfileRequest patched = applyPatch(current, patchDocument);

        Set<ConstraintViolation<PatchUserProfileRequest>> violations = validator.validate(patched);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        String normalizedEmail = patched.getEmail().trim().toLowerCase();
        if (userProfileRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, userId)) {
            throw new DuplicateResourceException("Email is already used by another user.");
        }

        user.setFullName(patched.getFullName().trim());
        user.setEmail(normalizedEmail);
        return userProfileMapper.toResponse(userProfileRepository.save(user));
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
        return userPreferenceMapper.toResponse(preference);
    }

    @Transactional
    public TravelHistoryResponse addTravelHistory(Long userId, AddTravelHistoryRequest request) {
        UserProfile user = findUserById(userId);

        TravelHistoryEntry entry = buildTravelHistoryEntry(request);

        user.addTravelHistoryEntry(entry);
        travelHistoryRepository.save(entry);
        return travelHistoryMapper.toResponse(entry);
    }

    @Transactional
    public List<TravelHistoryResponse> addTravelHistoryBatch(Long userId, List<AddTravelHistoryRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Batch payload must contain at least one travel history entry.");
        }

        UserProfile user = findUserById(userId);
        List<TravelHistoryEntry> entries = requests.stream()
                .map(this::buildTravelHistoryEntry)
                .toList();

        entries.forEach(user::addTravelHistoryEntry);
        travelHistoryRepository.saveAll(entries);

        return entries.stream()
                .map(travelHistoryMapper::toResponse)
                .toList();
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
        return ticketPurchaseMapper.toResponse(entry);
    }

    @Transactional(readOnly = true)
    public List<TicketPurchaseStatsResponse> getTicketPurchaseStats(Long userId) {
        findUserById(userId);
        return ticketPurchaseHistoryRepository.findTicketPurchaseStatsByUserId(userId);
    }

    @Transactional
    public void deleteTravelHistoryEntry(Long userId, Long entryId) {
        findUserById(userId);
        TravelHistoryEntry entry = travelHistoryRepository.findEntryForUser(userId, entryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Travel history entry with id " + entryId + " not found for user " + userId + "."));
        travelHistoryRepository.delete(entry);
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
                .map(travelHistoryMapper::toResponse)
                .toList();

        List<TicketPurchaseResponse> purchases = ticketPurchaseHistoryRepository
                .findByUserIdOrderByPurchasedAtDesc(userId)
                .stream()
                .map(ticketPurchaseMapper::toResponse)
                .toList();

        List<LoyaltyTransactionResponse> transactions = loyaltyTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(loyaltyTransactionMapper::toResponse)
                .toList();

        List<String> suggestions = getPersonalizedLineSuggestions(userId, 3);

        return new UserSummaryResponse(
                userProfileMapper.toResponse(user),
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
        return passwordEncoder.encode(rawPassword);
    }

    private TravelHistoryEntry buildTravelHistoryEntry(AddTravelHistoryRequest request) {
        TravelHistoryEntry entry = new TravelHistoryEntry();
        entry.setLineCode(request.lineCode().trim());
        entry.setFromStop(request.fromStop().trim());
        entry.setToStop(request.toStop().trim());
        entry.setDurationMinutes(request.durationMinutes());
        entry.setTraveledAt(request.traveledAt() != null ? request.traveledAt() : LocalDateTime.now());
        return entry;
    }

    private PatchUserProfileRequest applyPatch(PatchUserProfileRequest source, String patchDocument) {
        List<Object> operations;
        try {
            operations = jsonParser.parseList(patchDocument);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid JSON patch document.", ex);
        }

        if (operations.isEmpty()) {
            throw new IllegalArgumentException("Patch document must contain at least one operation.");
        }

        PatchUserProfileRequest patched = new PatchUserProfileRequest(source.getFullName(), source.getEmail());
        for (Object operation : operations) {
            if (!(operation instanceof Map<?, ?> operationMap)) {
                throw new IllegalArgumentException("Each patch operation must be a JSON object.");
            }
            applyPatchOperation(patched, operationMap);
        }
        return patched;
    }

    private void applyPatchOperation(PatchUserProfileRequest target, Map<?, ?> operationNode) {
        String op = getRequiredTextValue(operationNode, "op");
        String path = getRequiredTextValue(operationNode, "path");

        if (!"/fullName".equals(path) && !"/email".equals(path)) {
            throw new IllegalArgumentException("Unsupported patch path: " + path);
        }

        switch (op) {
            case "replace", "add" -> {
                Object valueNode = operationNode.get("value");
                if (!(valueNode instanceof String value)) {
                    throw new IllegalArgumentException("Patch value for path '" + path + "' must be a string.");
                }
                setPatchedField(target, path, value);
            }
            case "remove" -> setPatchedField(target, path, null);
            default -> throw new IllegalArgumentException("Unsupported patch operation: " + op);
        }
    }

    private String getRequiredTextValue(Map<?, ?> node, String fieldName) {
        Object valueNode = node.get(fieldName);
        if (!(valueNode instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException("Patch operation must contain textual field '" + fieldName + "'.");
        }
        return value;
    }

    private void setPatchedField(PatchUserProfileRequest target, String path, String value) {
        if ("/fullName".equals(path)) {
            target.setFullName(value);
            return;
        }
        if ("/email".equals(path)) {
            target.setEmail(value);
            return;
        }
        throw new IllegalArgumentException("Unsupported patch path: " + path);
    }
}
