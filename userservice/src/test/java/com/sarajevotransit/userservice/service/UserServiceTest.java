package com.sarajevotransit.userservice.service;

import com.sarajevotransit.userservice.dto.CreateUserRequest;
import com.sarajevotransit.userservice.dto.UserProfileResponse;
import com.sarajevotransit.userservice.config.ModelMapperConfig;
import com.sarajevotransit.userservice.exception.DuplicateResourceException;
import com.sarajevotransit.userservice.exception.ResourceNotFoundException;
import com.sarajevotransit.userservice.mapper.LoyaltyTransactionMapper;
import com.sarajevotransit.userservice.mapper.TicketPurchaseMapper;
import com.sarajevotransit.userservice.mapper.TravelHistoryMapper;
import com.sarajevotransit.userservice.mapper.UserPreferenceMapper;
import com.sarajevotransit.userservice.mapper.UserProfileMapper;
import com.sarajevotransit.userservice.model.DigitalWallet;
import com.sarajevotransit.userservice.model.LanguageCode;
import com.sarajevotransit.userservice.model.NotificationChannel;
import com.sarajevotransit.userservice.model.ThemeMode;
import com.sarajevotransit.userservice.model.TravelHistoryEntry;
import com.sarajevotransit.userservice.model.UserPreference;
import com.sarajevotransit.userservice.model.UserProfile;
import com.sarajevotransit.userservice.repository.LoyaltyTransactionRepository;
import com.sarajevotransit.userservice.repository.TicketPurchaseHistoryRepository;
import com.sarajevotransit.userservice.repository.TravelHistoryRepository;
import com.sarajevotransit.userservice.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private final ModelMapper modelMapper = new ModelMapperConfig().modelMapper();

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private TravelHistoryRepository travelHistoryRepository;

    @Mock
    private TicketPurchaseHistoryRepository ticketPurchaseHistoryRepository;

    @Mock
    private LoyaltyTransactionRepository loyaltyTransactionRepository;

    @Spy
    private UserPreferenceMapper userPreferenceMapper = new UserPreferenceMapper(modelMapper);

    @Spy
    private UserProfileMapper userProfileMapper = new UserProfileMapper(modelMapper);

    @Spy
    private TravelHistoryMapper travelHistoryMapper = new TravelHistoryMapper(modelMapper);

    @Spy
    private TicketPurchaseMapper ticketPurchaseMapper = new TicketPurchaseMapper(modelMapper);

    @Spy
    private LoyaltyTransactionMapper loyaltyTransactionMapper = new LoyaltyTransactionMapper(modelMapper);

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_shouldThrowWhenEmailAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest(
                "Lejla Music",
                "Lejla.Music@sarajevotransit.ba",
                "StrongPass123",
                LanguageCode.BS,
                ThemeMode.SYSTEM,
                NotificationChannel.PUSH);

        when(userProfileRepository.existsByEmailIgnoreCase("lejla.music@sarajevotransit.ba")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void createUser_shouldNormalizeEmailAndInitializeDefaults() {
        CreateUserRequest request = new CreateUserRequest(
                "Lejla Music",
                "LEJLA.MUSIC@SARAJEVOTRANSIT.BA",
                "StrongPass123",
                null,
                null,
                null);

        when(userProfileRepository.existsByEmailIgnoreCase("lejla.music@sarajevotransit.ba")).thenReturn(false);
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile user = invocation.getArgument(0);
            user.setId(55L);
            return user;
        });

        UserProfileResponse response = userService.createUser(request);

        assertThat(response.id()).isEqualTo(55L);
        assertThat(response.email()).isEqualTo("lejla.music@sarajevotransit.ba");
        assertThat(response.loyaltyPointsBalance()).isEqualTo(0);
        assertThat(response.preference()).isNotNull();
        assertThat(response.preference().languageCode()).isEqualTo(LanguageCode.BS);
        assertThat(response.preference().themeMode()).isEqualTo(ThemeMode.SYSTEM);
        assertThat(response.preference().notificationChannel()).isEqualTo(NotificationChannel.PUSH);

        ArgumentCaptor<UserProfile> userCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(userCaptor.capture());
        UserProfile saved = userCaptor.getValue();
        assertThat(saved.getFirstName()).isEqualTo("Lejla");
        assertThat(saved.getLastName()).isEqualTo("Music");
        assertThat(saved.getPasswordHash()).matches("[0-9a-f]{64}");
    }

    @Test
    void getAllUsers_shouldUseFetchOptimizedRepositoryQuery() {
        UserProfile user = buildUser(1L, "mina.alic@sarajevotransit.ba", 12);
        when(userProfileRepository.findAllWithWalletAndPreference()).thenReturn(List.of(user));

        List<UserProfileResponse> responses = userService.getAllUsers();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().email()).isEqualTo("mina.alic@sarajevotransit.ba");
        verify(userProfileRepository).findAllWithWalletAndPreference();
        verify(userProfileRepository, never()).findAll();
    }

    @Test
    void getPersonalizedLineSuggestions_shouldRankByUsageStats() {
        UserProfile user = buildUser(9L, "amina.hadzic@sarajevotransit.ba", 0);
        when(userProfileRepository.findById(9L)).thenReturn(Optional.of(user));
        when(travelHistoryRepository.findLineUsageStats(9L)).thenReturn(List.of(
                usage("TRAM-3", 2),
                usage("BUS-31E", 5),
                usage("TROL-103", 1)));

        List<String> suggestions = userService.getPersonalizedLineSuggestions(9L, 3);

        assertThat(suggestions).containsExactly("BUS-31E", "TRAM-3", "TROL-103");
        verify(travelHistoryRepository, never()).findByUserIdOrderByTraveledAtDesc(9L);
    }

    @Test
    void getPersonalizedLineSuggestions_shouldFallbackToRecentDistinctLinesWhenStatsMissing() {
        UserProfile user = buildUser(10L, "tarik.kovac@sarajevotransit.ba", 0);
        when(userProfileRepository.findById(10L)).thenReturn(Optional.of(user));
        when(travelHistoryRepository.findLineUsageStats(10L)).thenReturn(List.of());
        when(travelHistoryRepository.findByUserIdOrderByTraveledAtDesc(10L)).thenReturn(List.of(
                travel("TRAM-3"),
                travel("TRAM-3"),
                travel("BUS-31E"),
                travel("TROL-103")));

        List<String> suggestions = userService.getPersonalizedLineSuggestions(10L, 2);

        assertThat(suggestions).containsExactly("TRAM-3", "BUS-31E");
    }

    @Test
    void findUserById_shouldThrowWhenMissing() {
        when(userProfileRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findUserById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    private UserProfile buildUser(Long id, String email, int points) {
        UserProfile user = new UserProfile();
        user.setId(id);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPasswordHash("hash");

        DigitalWallet wallet = new DigitalWallet();
        wallet.setLoyaltyPointsTotal(points);
        user.setWallet(wallet);

        UserPreference preference = new UserPreference();
        preference.setLanguageCode(LanguageCode.BS);
        preference.setThemeMode(ThemeMode.SYSTEM);
        preference.setNotificationChannel(NotificationChannel.PUSH);
        preference.setHighContrastEnabled(false);
        preference.setLargeTextEnabled(false);
        preference.setScreenReaderEnabled(false);
        user.setPreference(preference);

        return user;
    }

    private TravelHistoryEntry travel(String lineCode) {
        TravelHistoryEntry entry = new TravelHistoryEntry();
        entry.setLineCode(lineCode);
        return entry;
    }

    private TravelHistoryRepository.LineUsageView usage(String lineCode, long count) {
        return new TravelHistoryRepository.LineUsageView() {
            @Override
            public String getLineCode() {
                return lineCode;
            }

            @Override
            public Long getUsageCount() {
                return count;
            }
        };
    }
}
