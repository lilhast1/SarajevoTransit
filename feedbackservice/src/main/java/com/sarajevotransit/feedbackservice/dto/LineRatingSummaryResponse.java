package com.sarajevotransit.feedbackservice.dto;

public class LineRatingSummaryResponse {

    private Long lineId;
    private Double averageRating;
    private Long totalReviews;

    public LineRatingSummaryResponse() {
    }

    public LineRatingSummaryResponse(Long lineId, Double averageRating, Long totalReviews) {
        this.lineId = lineId;
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
    }

    public Long getLineId() {
        return lineId;
    }

    public void setLineId(Long lineId) {
        this.lineId = lineId;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Long totalReviews) {
        this.totalReviews = totalReviews;
    }
}