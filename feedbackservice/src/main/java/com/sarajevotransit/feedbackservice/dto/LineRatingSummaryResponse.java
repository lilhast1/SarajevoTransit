package com.sarajevotransit.feedbackservice.dto;

public class LineRatingSummaryResponse {

    private String lineCode;
    private Double averageRating;
    private Long totalReviews;

    public LineRatingSummaryResponse() {
    }

    public LineRatingSummaryResponse(String lineCode, Double averageRating, Long totalReviews) {
        this.lineCode = lineCode;
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
    }

    public String getLineCode() {
        return lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
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