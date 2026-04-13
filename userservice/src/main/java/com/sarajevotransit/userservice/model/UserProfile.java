package com.sarajevotransit.userservice.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "first_name", nullable = false)
    @NotBlank
    @Size(max = 120)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    @NotNull
    @Size(max = 120)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    @NotBlank
    @Size(max = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @NotNull
    private UserRole role = UserRole.PASSENGER;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Valid
    private DigitalWallet wallet;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Valid
    private UserPreference preference;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TravelHistoryEntry> travelHistoryEntries = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketPurchaseHistoryEntry> ticketPurchases = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoyaltyTransaction> loyaltyTransactions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.role == null) {
            this.role = UserRole.PASSENGER;
        }
        if (this.wallet == null) {
            setWallet(new DigitalWallet());
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void setPreference(UserPreference preference) {
        this.preference = preference;
        if (preference != null) {
            preference.setUser(this);
        }
    }

    public void setWallet(DigitalWallet wallet) {
        this.wallet = wallet;
        if (wallet != null) {
            wallet.setUser(this);
        }
    }

    public void addTravelHistoryEntry(TravelHistoryEntry entry) {
        travelHistoryEntries.add(entry);
        entry.setUser(this);
    }

    public void addTicketPurchase(TicketPurchaseHistoryEntry entry) {
        ticketPurchases.add(entry);
        entry.setUser(this);
    }

    public void addLoyaltyTransaction(LoyaltyTransaction transaction) {
        loyaltyTransactions.add(transaction);
        transaction.setUser(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        String left = this.firstName == null ? "" : this.firstName.trim();
        String right = this.lastName == null ? "" : this.lastName.trim();
        return (left + " " + right).trim();
    }

    public void setFullName(String fullName) {
        String normalized = fullName == null ? "" : fullName.trim();
        if (normalized.isEmpty()) {
            this.firstName = "Unknown";
            this.lastName = "User";
            return;
        }

        String[] tokens = normalized.split("\\s+", 2);
        this.firstName = tokens[0];
        this.lastName = tokens.length > 1 ? tokens[1] : "";
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Integer getLoyaltyPointsBalance() {
        if (wallet == null || wallet.getLoyaltyPointsTotal() == null) {
            return 0;
        }
        return wallet.getLoyaltyPointsTotal();
    }

    public void setLoyaltyPointsBalance(Integer loyaltyPointsBalance) {
        if (this.wallet == null) {
            setWallet(new DigitalWallet());
        }
        this.wallet.setLoyaltyPointsTotal(loyaltyPointsBalance == null ? 0 : loyaltyPointsBalance);
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UserPreference getPreference() {
        return preference;
    }

    public DigitalWallet getWallet() {
        return wallet;
    }

    public List<TravelHistoryEntry> getTravelHistoryEntries() {
        return travelHistoryEntries;
    }

    public void setTravelHistoryEntries(List<TravelHistoryEntry> travelHistoryEntries) {
        this.travelHistoryEntries = travelHistoryEntries;
    }

    public List<TicketPurchaseHistoryEntry> getTicketPurchases() {
        return ticketPurchases;
    }

    public void setTicketPurchases(List<TicketPurchaseHistoryEntry> ticketPurchases) {
        this.ticketPurchases = ticketPurchases;
    }

    public List<LoyaltyTransaction> getLoyaltyTransactions() {
        return loyaltyTransactions;
    }

    public void setLoyaltyTransactions(List<LoyaltyTransaction> loyaltyTransactions) {
        this.loyaltyTransactions = loyaltyTransactions;
    }
}
