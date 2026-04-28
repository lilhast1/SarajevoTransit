package com.sarajevotransit.feedbackservice.mapper;

import com.sarajevotransit.feedbackservice.dto.CreateProblemReportRequest;
import com.sarajevotransit.feedbackservice.dto.ProblemReportResponse;
import com.sarajevotransit.feedbackservice.model.ProblemReport;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class ProblemReportMapper {

    private final ModelMapper modelMapper;

    public ProblemReportMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public ProblemReport toEntity(CreateProblemReportRequest request) {
        return modelMapper.map(request, ProblemReport.class);
    }

    public ProblemReportResponse toResponse(ProblemReport entity) {
        return modelMapper.map(entity, ProblemReportResponse.class);
    }
}
