package com.sarajevotransit.feedbackservice.dto;

import java.util.List;

public record BatchCreateProblemReportsResponse(
        int insertedCount,
        List<ProblemReportResponse> reports) {
}
