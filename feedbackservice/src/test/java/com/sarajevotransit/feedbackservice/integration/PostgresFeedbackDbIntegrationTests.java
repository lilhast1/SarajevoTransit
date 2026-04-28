package com.sarajevotransit.feedbackservice.integration;

import com.sarajevotransit.feedbackservice.dto.CreateLineReviewRequest;
import com.sarajevotransit.feedbackservice.dto.LineReviewResponse;
import com.sarajevotransit.feedbackservice.dto.ProblemReportResponse;
import com.sarajevotransit.feedbackservice.model.ProblemCategory;
import com.sarajevotransit.feedbackservice.model.ProblemReport;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import com.sarajevotransit.feedbackservice.repository.ProblemReportRepository;
import com.sarajevotransit.feedbackservice.service.LineReviewService;
import com.sarajevotransit.feedbackservice.service.ProblemReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PostgresFeedbackDbIntegrationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ProblemReportRepository problemReportRepository;

    @Autowired
    private ProblemReportService problemReportService;

    @Autowired
    private LineReviewService lineReviewService;

    @Test
    void shouldPersistAndReadProblemReportUsingRealPostgresFeedbackDb() throws SQLException {
        assertPostgresFeedbackDb();

        ProblemReport report = new ProblemReport();
        report.setReporterUserId(990001L);
        report.setLineId(880101L);
        report.setStationId(770101L);
        report.setCategory(ProblemCategory.DELAY);
        report.setDescription("Postgres integration test report");
        report.setPhotoUrls(List.of("https://example.com/postgres/report.png"));
        report.setStatus(ReportStatus.RECEIVED);

        ProblemReport saved = problemReportRepository.saveAndFlush(report);

        ProblemReport loaded = problemReportRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getReporterUserId()).isEqualTo(990001L);
        assertThat(loaded.getDescription()).isEqualTo("Postgres integration test report");
        assertThat(loaded.getCategory()).isEqualTo(ProblemCategory.DELAY);
    }

    @Test
    void shouldPageProblemReportsFromRealPostgresFeedbackDb() throws SQLException {
        assertPostgresFeedbackDb();

        long uniqueLineId = Math.abs(System.nanoTime());

        for (int i = 0; i < 3; i++) {
            ProblemReport report = new ProblemReport();
            report.setReporterUserId(991000L + i);
            report.setLineId(uniqueLineId);
            report.setStationId(772000L + i);
            report.setCategory(ProblemCategory.CROWDING);
            report.setDescription("Paged postgres report " + i);
            report.setPhotoUrls(List.of("https://example.com/postgres/page-" + i + ".png"));
            report.setStatus(ReportStatus.RECEIVED);
            problemReportRepository.save(report);
        }
        problemReportRepository.flush();

        Page<ProblemReportResponse> page = problemReportService.getReportsByLineId(
                uniqueLineId,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allSatisfy(item -> assertThat(item.getLineId()).isEqualTo(uniqueLineId));
    }

    @Test
    void shouldCreateAndReadLineReviewUsingRealPostgresFeedbackDb() throws SQLException {
        assertPostgresFeedbackDb();

        long uniqueReviewerId = Math.abs(System.nanoTime() % 1_000_000_000L) + 5_000_000_000L;
        long uniqueLineId = Math.abs(System.nanoTime() % 1_000_000L) + 8_000_000L;

        CreateLineReviewRequest request = new CreateLineReviewRequest();
        request.setReviewerUserId(uniqueReviewerId);
        request.setLineId(uniqueLineId);
        request.setRating(5);
        request.setReviewText("Postgres-backed review test.");
        request.setRideDate(LocalDate.now().minusDays(1));

        LineReviewResponse created = lineReviewService.createReview(request);
        LineReviewResponse loaded = lineReviewService.getReview(created.getId());

        assertThat(loaded.getId()).isEqualTo(created.getId());
        assertThat(loaded.getReviewerUserId()).isEqualTo(uniqueReviewerId);
        assertThat(loaded.getLineId()).isEqualTo(uniqueLineId);
        assertThat(loaded.getRating()).isEqualTo(5);
    }

    private void assertPostgresFeedbackDb() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertThat(url).startsWith("jdbc:postgresql:");
            assertThat(url).contains("/feedbackdb");
        }
    }
}