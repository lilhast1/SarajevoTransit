package com.sarajevotransit.feedbackservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class BatchCreateProblemReportsRequest {

    @NotEmpty
    @Size(max = 200)
    private List<@Valid CreateProblemReportRequest> reports = new ArrayList<>();

    public List<CreateProblemReportRequest> getReports() {
        return reports;
    }

    public void setReports(List<CreateProblemReportRequest> reports) {
        this.reports = reports;
    }
}
