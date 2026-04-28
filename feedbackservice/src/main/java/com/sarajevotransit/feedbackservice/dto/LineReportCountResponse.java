package com.sarajevotransit.feedbackservice.dto;

public record LineReportCountResponse(
        Long lineId,
        long totalReports) {
}
