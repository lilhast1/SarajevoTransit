package com.sarajevotransit.feedbackservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BatchCreateProblemReportsRequest {

    @NotEmpty
    @Size(max = 200)
    private List<@Valid CreateProblemReportRequest> reports = new ArrayList<>();
}
