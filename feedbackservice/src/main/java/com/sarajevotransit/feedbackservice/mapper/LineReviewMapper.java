package com.sarajevotransit.feedbackservice.mapper;

import com.sarajevotransit.feedbackservice.dto.CreateLineReviewRequest;
import com.sarajevotransit.feedbackservice.dto.LineReviewResponse;
import com.sarajevotransit.feedbackservice.model.LineReview;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class LineReviewMapper {

    private final ModelMapper modelMapper;

    public LineReviewMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public LineReview toEntity(CreateLineReviewRequest request) {
        return modelMapper.map(request, LineReview.class);
    }

    public LineReviewResponse toResponse(LineReview entity) {
        return modelMapper.map(entity, LineReviewResponse.class);
    }
}
