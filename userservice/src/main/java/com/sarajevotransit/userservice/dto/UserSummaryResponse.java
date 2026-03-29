package com.sarajevotransit.userservice.dto;

import java.util.List;

public record UserSummaryResponse(
                UserProfileResponse profile,
                List<TravelHistoryResponse> travelHistory,
                List<TicketPurchaseResponse> ticketPurchases,
                List<LoyaltyTransactionResponse> loyaltyTransactions,
                List<String> personalizedLineSuggestions) {
}
