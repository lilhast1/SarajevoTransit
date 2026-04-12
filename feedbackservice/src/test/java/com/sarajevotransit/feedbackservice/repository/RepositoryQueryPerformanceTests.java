package com.sarajevotransit.feedbackservice.repository;

import com.sarajevotransit.feedbackservice.dto.LineReviewResponse;
import com.sarajevotransit.feedbackservice.dto.ProblemReportResponse;
import com.sarajevotransit.feedbackservice.model.LineReview;
import com.sarajevotransit.feedbackservice.model.ModerationStatus;
import com.sarajevotransit.feedbackservice.model.ProblemCategory;
import com.sarajevotransit.feedbackservice.model.ProblemReport;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import com.sarajevotransit.feedbackservice.service.LineReviewService;
import com.sarajevotransit.feedbackservice.service.ProblemReportService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RepositoryQueryPerformanceTests {

    @Autowired
    private ProblemReportRepository problemReportRepository;

    @Autowired
    private LineReviewRepository lineReviewRepository;

    @Autowired
    private ProblemReportService problemReportService;

    @Autowired
    private LineReviewService lineReviewService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        lineReviewRepository.deleteAll();
        problemReportRepository.deleteAll();

        seedReports();
        seedReviews();
    }

    @Test
    void getReports_shouldNotHaveNPlusOneOnPhotoUrls() {
        statistics.clear();

        List<ProblemReportResponse> reports = problemReportService.getReports(null, null);

        assertThat(reports).hasSize(3);
        assertThat(reports).allSatisfy(report -> assertThat(report.getPhotoUrls()).isNotNull());
        assertThat(statistics.getPrepareStatementCount())
                .as("Expected single select with fetch graph (or at most one extra statement)")
                .isLessThanOrEqualTo(2);
    }

    @Test
    void getReviewsByLine_shouldNotHaveNPlusOne() {
        statistics.clear();

        List<LineReviewResponse> reviews = lineReviewService.getReviewsByLine(33L, false);

        assertThat(reviews).hasSize(2);
        assertThat(statistics.getPrepareStatementCount())
                .as("Line reviews query should be resolved in a single statement")
                .isLessThanOrEqualTo(1);
    }

    private void seedReports() {
        for (int i = 0; i < 3; i++) {
            ProblemReport report = new ProblemReport();
            report.setReporterUserId(1000L + i);
            report.setLineId(33L);
            report.setStationId(200L + i);
            report.setCategory(ProblemCategory.DELAY);
            report.setDescription("Delay report " + i);
            report.setPhotoUrls(List.of(
                    "https://example.com/report/" + i + "/1.png",
                    "https://example.com/report/" + i + "/2.png"));
            report.setStatus(ReportStatus.RECEIVED);
            problemReportRepository.save(report);
        }
        problemReportRepository.flush();
    }

    private void seedReviews() {
        for (int i = 0; i < 2; i++) {
            LineReview review = new LineReview();
            review.setReviewerUserId(3000L + i);
            review.setLineId(33L);
            review.setRating(4);
            review.setReviewText("Review " + i);
            review.setRideDate(LocalDate.now().minusDays(1 + i));
            review.setModerationStatus(ModerationStatus.VISIBLE);
            lineReviewRepository.save(review);
        }
        lineReviewRepository.flush();
    }
}
