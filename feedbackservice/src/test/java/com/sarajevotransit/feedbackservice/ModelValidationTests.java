package com.sarajevotransit.feedbackservice;

import com.sarajevotransit.feedbackservice.model.LineReview;
import com.sarajevotransit.feedbackservice.model.ProblemReport;
import com.sarajevotransit.feedbackservice.repository.LineReviewRepository;
import com.sarajevotransit.feedbackservice.repository.ProblemReportRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ModelValidationTests {

    @Autowired
    private ProblemReportRepository problemReportRepository;

    @Autowired
    private LineReviewRepository lineReviewRepository;

    @Test
    void shouldRejectInvalidProblemReportModel() {
        ProblemReport report = new ProblemReport();
        report.setReporterUserId(-1L);
        report.setDescription("   ");

        assertThatThrownBy(() -> problemReportRepository.saveAndFlush(report))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldRejectInvalidLineReviewModel() {
        LineReview review = new LineReview();
        review.setReviewerUserId(100L);
        review.setLineId(20L);
        review.setRating(7);

        assertThatThrownBy(() -> lineReviewRepository.saveAndFlush(review))
                .isInstanceOf(ConstraintViolationException.class);
    }
}
