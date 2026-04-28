package com.sarajevotransit.feedbackservice.controller;

import com.sarajevotransit.feedbackservice.model.LineReview;
import com.sarajevotransit.feedbackservice.model.ModerationStatus;
import com.sarajevotransit.feedbackservice.model.ProblemCategory;
import com.sarajevotransit.feedbackservice.model.ProblemReport;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import com.sarajevotransit.feedbackservice.repository.LineReviewRepository;
import com.sarajevotransit.feedbackservice.repository.ProblemReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class FeedbackWorkflowControllerIntegrationTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProblemReportRepository problemReportRepository;

    @Autowired
    private LineReviewRepository lineReviewRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        lineReviewRepository.deleteAll();
        problemReportRepository.deleteAll();
    }

    @Test
    void moderateLineFeedback_shouldUpdateReportsAndReviews() throws Exception {
        ProblemReport report = new ProblemReport();
        report.setReporterUserId(5001L);
        report.setLineId(88L);
        report.setStationId(901L);
        report.setCategory(ProblemCategory.DELAY);
        report.setDescription("Workflow report");
        report.setPhotoUrls(List.of("https://example.com/workflow-report.png"));
        report.setStatus(ReportStatus.RECEIVED);
        problemReportRepository.saveAndFlush(report);

        LineReview review = new LineReview();
        review.setReviewerUserId(6001L);
        review.setLineId(88L);
        review.setRating(3);
        review.setReviewText("Workflow review");
        review.setRideDate(LocalDate.now().minusDays(1));
        review.setModerationStatus(ModerationStatus.VISIBLE);
        lineReviewRepository.saveAndFlush(review);

        String payload = """
                {
                  "reportStatus": "RESOLVED",
                  "moderationStatus": "HIDDEN"
                }
                """;

        mockMvc.perform(post("/api/v1/workflows/lines/88/moderation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineId").value(88))
                .andExpect(jsonPath("$.reportStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.moderationStatus").value("HIDDEN"))
                .andExpect(jsonPath("$.updatedReports").value(1))
                .andExpect(jsonPath("$.updatedReviews").value(1));

        ProblemReport updatedReport = problemReportRepository.findByLineIdOrderByCreatedAtDesc(88L).getFirst();
        LineReview updatedReview = lineReviewRepository.findByLineIdOrderByCreatedAtDesc(88L).getFirst();

        assertThat(updatedReport.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(updatedReview.getModerationStatus()).isEqualTo(ModerationStatus.HIDDEN);
    }

    @Test
    void moderateLineFeedback_whenNoFeedbackExists_shouldReturnNotFound() throws Exception {
        String payload = """
                {
                  "reportStatus": "RESOLVED",
                  "moderationStatus": "HIDDEN"
                }
                """;

        mockMvc.perform(post("/api/v1/workflows/lines/999/moderation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void moderateLineFeedback_withMissingBodyFields_shouldReturnBadRequest() throws Exception {
        String payload = """
                {
                }
                """;

        mockMvc.perform(post("/api/v1/workflows/lines/88/moderation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }

    @Test
    void moderateLineFeedback_withInvalidPathVariable_shouldReturnBadRequest() throws Exception {
        String payload = """
                {
                  "reportStatus": "RESOLVED",
                  "moderationStatus": "HIDDEN"
                }
                """;

        mockMvc.perform(post("/api/v1/workflows/lines/0/moderation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }
}
