package com.sarajevotransit.feedbackservice.dto;

import java.util.List;
import java.util.Map;

public record AdminFeedbackStatsResponse(
        Map<String, Long> reportsByCategory,
        Map<String, Long> reportsByStatus,
        long totalReports,
        Map<Integer, Long> ratingDistribution,
        double averageRating,
        long totalReviews,
        List<RatingTrendPoint> ratingTrend,
        List<LineReportCount> mostReportedLines
) {
    public record RatingTrendPoint(String month, double avgRating, long reviewCount) {}
    public record LineReportCount(Long lineId, long count) {}
}
