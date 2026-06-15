package com.sarajevotransit.moneyman.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AdminStatsResponse(
        BigDecimal totalRevenue,
        long totalTickets,
        long activeTickets,
        Map<String, Long> ticketsByType,
        Map<String, Long> ticketsByStatus,
        List<TimeSeriesPoint> revenueTimeSeries,
        List<HourlyPoint> hourlyDistribution,
        List<TopBuyer> topBuyers
) {
    public record TimeSeriesPoint(String date, BigDecimal revenue, long count) {}
    public record HourlyPoint(int hour, long count) {}
    public record TopBuyer(Long userId, long ticketCount, BigDecimal totalSpent) {}
}
