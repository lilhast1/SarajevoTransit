package com.sarajevotransit.moneyman.service;

import com.sarajevotransit.moneyman.dto.AdminStatsResponse;
import com.sarajevotransit.moneyman.repository.TicketRepository;
import com.sarajevotransit.moneyman.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final TicketRepository ticketRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats(String period) {
        LocalDateTime[] range = periodRange(period);
        LocalDateTime from = range[0];
        LocalDateTime to   = range[1];

        BigDecimal totalRevenue = transactionRepository.sumCompletedRevenue(from, to);
        long totalTickets = transactionRepository.countInPeriod(from, to);
        long activeTickets = ticketRepository.countActive();

        Map<String, Long> byType = toMap(ticketRepository.countByTypeInPeriod(from, to));
        Map<String, Long> byStatus = toMap(ticketRepository.countByStatusInPeriod(from, to));

        List<AdminStatsResponse.TimeSeriesPoint> timeSeries = ticketRepository.revenueTimeSeries(from, to)
                .stream()
                .map(r -> new AdminStatsResponse.TimeSeriesPoint(
                        r[0] != null ? r[0].toString() : "",
                        toBigDecimal(r[2]),
                        toLong(r[1])))
                .toList();

        List<AdminStatsResponse.HourlyPoint> hourly = ticketRepository.hourlyDistribution(from, to)
                .stream()
                .map(r -> new AdminStatsResponse.HourlyPoint(toInt(r[0]), toLong(r[1])))
                .toList();

        List<AdminStatsResponse.TopBuyer> topBuyers = ticketRepository.topBuyers(from, to)
                .stream()
                .map(r -> new AdminStatsResponse.TopBuyer(toLong(r[0]), toLong(r[1]), toBigDecimal(r[2])))
                .toList();

        return new AdminStatsResponse(totalRevenue, totalTickets, activeTickets,
                byType, byStatus, timeSeries, hourly, topBuyers);
    }

    private LocalDateTime[] periodRange(String period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period == null ? "MONTH" : period.toUpperCase()) {
            case "TODAY" -> new LocalDateTime[]{now.with(LocalTime.MIDNIGHT), now};
            case "WEEK"  -> new LocalDateTime[]{now.minusWeeks(1), now};
            case "ALL"   -> new LocalDateTime[]{LocalDateTime.of(2000, 1, 1, 0, 0), now};
            default      -> new LocalDateTime[]{now.minusMonths(1), now};
        };
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] r : rows) {
            map.put(r[0].toString(), toLong(r[1]));
        }
        return map;
    }

    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(o.toString());
    }

    private int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(o.toString());
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(o.toString());
    }
}
