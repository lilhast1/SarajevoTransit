package com.sarajevotransit.userservice.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private Integer loyaltyPointsBalance = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
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
        if (this.loyaltyPointsBalance == null) {
            this.loyaltyPointsBalance = 0;
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
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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
        return loyaltyPointsBalance;
    }

    public void setLoyaltyPointsBalance(Integer loyaltyPointsBalance) {
        this.loyaltyPointsBalance = loyaltyPointsBalance;
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
