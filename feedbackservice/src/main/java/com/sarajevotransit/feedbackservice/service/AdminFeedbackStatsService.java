package com.sarajevotransit.feedbackservice.service;

import com.sarajevotransit.feedbackservice.dto.AdminFeedbackStatsResponse;
import com.sarajevotransit.feedbackservice.repository.LineReviewRepository;
import com.sarajevotransit.feedbackservice.repository.ProblemReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminFeedbackStatsService {

    private final ProblemReportRepository reportRepository;
    private final LineReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public AdminFeedbackStatsResponse getStats() {
        Map<String, Long> byCategory = toStringMap(reportRepository.countByCategory());
        Map<String, Long> byStatus   = toStringMap(reportRepository.countByReportStatus());
        long totalReports = byStatus.values().stream().mapToLong(Long::longValue).sum();

        Double avgRating  = reviewRepository.globalAverageRating();
        long totalReviews = reviewRepository.countVisible();

        Map<Integer, Long> ratingDist = new LinkedHashMap<>();
        for (Object[] r : reviewRepository.ratingDistribution()) {
            ratingDist.put(toInt(r[0]), toLong(r[1]));
        }

        List<AdminFeedbackStatsResponse.RatingTrendPoint> trend = reviewRepository.ratingTrend()
                .stream()
                .map(r -> new AdminFeedbackStatsResponse.RatingTrendPoint(
                        (String) r[0],
                        toDouble(r[1]),
                        toLong(r[2])))
                .toList();

        List<AdminFeedbackStatsResponse.LineReportCount> mostReported = reportRepository.topReportedLineIds()
                .stream()
                .map(r -> new AdminFeedbackStatsResponse.LineReportCount(toLong(r[0]), toLong(r[1])))
                .toList();

        return new AdminFeedbackStatsResponse(
                byCategory, byStatus, totalReports,
                ratingDist, avgRating != null ? avgRating : 0.0,
                totalReviews, trend, mostReported);
    }

    private Map<String, Long> toStringMap(List<Object[]> rows) {
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

    private double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        return Double.parseDouble(o.toString());
    }
}
