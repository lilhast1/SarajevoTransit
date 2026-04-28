package com.sarajevotransit.feedbackservice.controller;

import com.sarajevotransit.feedbackservice.model.LineReview;
import com.sarajevotransit.feedbackservice.model.ModerationStatus;
import com.sarajevotransit.feedbackservice.repository.LineReviewRepository;
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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class LineReviewControllerIntegrationTests {

        private MockMvc mockMvc;

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private LineReviewRepository lineReviewRepository;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
                lineReviewRepository.deleteAll();
        }

        @Test
        void createReview_shouldReturnCreated() throws Exception {
                String payload = """
                                {
                                  "reviewerUserId": 5201,
                                  "lineId": 6,
                                  "rating": 4,
                                  "reviewText": "Ride was stable with medium crowd.",
                                  "rideDate": "2026-04-10"
                                }
                                """;

                mockMvc.perform(post("/api/v1/reviews")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isCreated())
                                .andExpect(header().exists("Location"))
                                .andExpect(jsonPath("$.moderationStatus").value("VISIBLE"))
                                .andExpect(jsonPath("$.rating").value(4))
                                .andExpect(jsonPath("$.reviewerUserId").value(5201));
        }

        @Test
        void createReview_withValidationError_shouldReturnBadRequest() throws Exception {
                String payload = """
                                {
                                  "reviewerUserId": 5201,
                                  "lineId": 6,
                                  "rating": 7,
                                  "rideDate": null
                                }
                                """;

                mockMvc.perform(post("/api/v1/reviews")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("validation_error"))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.path").value("/api/v1/reviews"))
                                .andExpect(jsonPath("$.validationErrors", hasItem(startsWith("rating:"))))
                                .andExpect(jsonPath("$.validationErrors", hasItem(startsWith("rideDate:"))));
        }

        @Test
        void createReview_withMalformedJson_shouldReturnBadRequest() throws Exception {
                String payload = """
                                {
                                  "reviewerUserId": 5201,
                                  "lineId": 6,
                                  "rating": 4,
                                  "rideDate": "2026-04-10"
                                """;

                mockMvc.perform(post("/api/v1/reviews")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("malformed_json"))
                                .andExpect(jsonPath("$.path").value("/api/v1/reviews"))
                                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
        }

        @Test
        void getReviews_shouldReturnOnlyVisibleByDefault() throws Exception {
                LineReview visible = new LineReview();
                visible.setReviewerUserId(7301L);
                visible.setLineId(40L);
                visible.setRating(4);
                visible.setRideDate(LocalDate.now().minusDays(1));
                visible.setModerationStatus(ModerationStatus.VISIBLE);
                lineReviewRepository.save(visible);

                LineReview hidden = new LineReview();
                hidden.setReviewerUserId(7302L);
                hidden.setLineId(40L);
                hidden.setRating(5);
                hidden.setRideDate(LocalDate.now().minusDays(1));
                hidden.setModerationStatus(ModerationStatus.HIDDEN);
                lineReviewRepository.saveAndFlush(hidden);

                mockMvc.perform(get("/api/v1/reviews")
                                .param("lineId", "40")
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalElements").value(1))
                                .andExpect(jsonPath("$.content[0].moderationStatus").value("VISIBLE"));
        }

        @Test
        void getReviews_withIncludeHiddenTrue_shouldReturnAllModerationStatuses() throws Exception {
                LineReview visible = new LineReview();
                visible.setReviewerUserId(7401L);
                visible.setLineId(41L);
                visible.setRating(3);
                visible.setRideDate(LocalDate.now().minusDays(1));
                visible.setModerationStatus(ModerationStatus.VISIBLE);
                lineReviewRepository.save(visible);

                LineReview hidden = new LineReview();
                hidden.setReviewerUserId(7402L);
                hidden.setLineId(41L);
                hidden.setRating(2);
                hidden.setRideDate(LocalDate.now().minusDays(2));
                hidden.setModerationStatus(ModerationStatus.HIDDEN);
                lineReviewRepository.saveAndFlush(hidden);

                mockMvc.perform(get("/api/v1/reviews")
                                .param("lineId", "41")
                                .param("includeHidden", "true")
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        void getReviews_withInvalidLineId_shouldReturnBadRequest() throws Exception {
                mockMvc.perform(get("/api/v1/reviews")
                                .param("lineId", "0"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("bad_request"));
        }

        @Test
        void getReview_shouldReturnSingleReview() throws Exception {
                LineReview review = new LineReview();
                review.setReviewerUserId(7501L);
                review.setLineId(51L);
                review.setRating(4);
                review.setReviewText("Single review");
                review.setRideDate(LocalDate.now().minusDays(1));
                review.setModerationStatus(ModerationStatus.VISIBLE);
                review = lineReviewRepository.saveAndFlush(review);

                mockMvc.perform(get("/api/v1/reviews/{id}", review.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(review.getId()))
                                .andExpect(jsonPath("$.lineId").value(51))
                                .andExpect(jsonPath("$.reviewText").value("Single review"));
        }

        @Test
        void updateModerationStatus_withInvalidEnum_shouldReturnBadRequest() throws Exception {
                LineReview review = new LineReview();
                review.setReviewerUserId(9001L);
                review.setLineId(12L);
                review.setRating(4);
                review.setRideDate(LocalDate.now().minusDays(1));
                review.setModerationStatus(ModerationStatus.VISIBLE);
                review = lineReviewRepository.saveAndFlush(review);

                String payload = """
                                {
                                  "moderationStatus": "DOES_NOT_EXIST"
                                }
                                """;

                mockMvc.perform(patch("/api/v1/reviews/{id}/moderation-status", review.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void getReview_withInvalidPathVariable_shouldReturnBadRequest() throws Exception {
                mockMvc.perform(get("/api/v1/reviews/0"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void getReviewsByReviewer_shouldReturnPagedList() throws Exception {
                LineReview review1 = new LineReview();
                review1.setReviewerUserId(7001L);
                review1.setLineId(12L);
                review1.setRating(4);
                review1.setRideDate(LocalDate.now().minusDays(1));
                review1.setModerationStatus(ModerationStatus.VISIBLE);
                lineReviewRepository.save(review1);

                LineReview review2 = new LineReview();
                review2.setReviewerUserId(7001L);
                review2.setLineId(13L);
                review2.setRating(5);
                review2.setRideDate(LocalDate.now().minusDays(2));
                review2.setModerationStatus(ModerationStatus.VISIBLE);
                lineReviewRepository.saveAndFlush(review2);

                mockMvc.perform(get("/api/v1/reviews/reviewer/7001")
                                .param("page", "0")
                                .param("size", "1")
                                .param("sort", "createdAt,desc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(1))
                                .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        void getReviewsByReviewer_withInvalidPathVariable_shouldReturnBadRequest() throws Exception {
                mockMvc.perform(get("/api/v1/reviews/reviewer/0"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("bad_request"));
        }

        @Test
        void getLatestVisibleReviewByLine_shouldReturnLatestVisibleOnly() throws Exception {
                LineReview hiddenReview = new LineReview();
                hiddenReview.setReviewerUserId(7101L);
                hiddenReview.setLineId(21L);
                hiddenReview.setRating(2);
                hiddenReview.setReviewText("Hidden review");
                hiddenReview.setRideDate(LocalDate.now().minusDays(1));
                hiddenReview.setModerationStatus(ModerationStatus.HIDDEN);
                lineReviewRepository.save(hiddenReview);

                LineReview visibleOlder = new LineReview();
                visibleOlder.setReviewerUserId(7102L);
                visibleOlder.setLineId(21L);
                visibleOlder.setRating(3);
                visibleOlder.setReviewText("Visible older");
                visibleOlder.setRideDate(LocalDate.now().minusDays(2));
                visibleOlder.setModerationStatus(ModerationStatus.VISIBLE);
                lineReviewRepository.save(visibleOlder);

                LineReview visibleLatest = new LineReview();
                visibleLatest.setReviewerUserId(7103L);
                visibleLatest.setLineId(21L);
                visibleLatest.setRating(5);
                visibleLatest.setReviewText("Visible latest");
                visibleLatest.setRideDate(LocalDate.now().minusDays(1));
                visibleLatest.setModerationStatus(ModerationStatus.VISIBLE);
                lineReviewRepository.saveAndFlush(visibleLatest);

                mockMvc.perform(get("/api/v1/reviews/line/21/latest"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.lineId").value(21))
                                .andExpect(jsonPath("$.moderationStatus").value("VISIBLE"))
                                .andExpect(jsonPath("$.reviewText").value("Visible latest"));
        }

        @Test
        void getLatestVisibleReviewByLine_whenNoVisibleExists_shouldReturnNotFound() throws Exception {
                LineReview hidden = new LineReview();
                hidden.setReviewerUserId(7111L);
                hidden.setLineId(22L);
                hidden.setRating(2);
                hidden.setReviewText("Only hidden");
                hidden.setRideDate(LocalDate.now().minusDays(1));
                hidden.setModerationStatus(ModerationStatus.HIDDEN);
                lineReviewRepository.saveAndFlush(hidden);

                mockMvc.perform(get("/api/v1/reviews/line/22/latest"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("not_found"));
        }

        @Test
        void updateModerationStatus_shouldReturnUpdatedReview() throws Exception {
                LineReview review = new LineReview();
                review.setReviewerUserId(7601L);
                review.setLineId(61L);
                review.setRating(4);
                review.setReviewText("Moderation target");
                review.setRideDate(LocalDate.now().minusDays(1));
                review.setModerationStatus(ModerationStatus.VISIBLE);
                review = lineReviewRepository.saveAndFlush(review);

                String payload = """
                                {
                                  "moderationStatus": "HIDDEN"
                                }
                                """;

                mockMvc.perform(patch("/api/v1/reviews/{id}/moderation-status", review.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(review.getId()))
                                .andExpect(jsonPath("$.moderationStatus").value("HIDDEN"));
        }

        @Test
        void updateModerationStatus_withMissingStatus_shouldReturnBadRequest() throws Exception {
                LineReview review = new LineReview();
                review.setReviewerUserId(7602L);
                review.setLineId(62L);
                review.setRating(4);
                review.setRideDate(LocalDate.now().minusDays(1));
                review.setModerationStatus(ModerationStatus.VISIBLE);
                review = lineReviewRepository.saveAndFlush(review);

                mockMvc.perform(patch("/api/v1/reviews/{id}/moderation-status", review.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("bad_request"));
        }

        @Test
        void deleteReview_shouldReturnNoContentAndRemoveEntity() throws Exception {
                LineReview review = new LineReview();
                review.setReviewerUserId(7201L);
                review.setLineId(31L);
                review.setRating(4);
                review.setReviewText("Delete this review");
                review.setRideDate(LocalDate.now().minusDays(1));
                review.setModerationStatus(ModerationStatus.VISIBLE);
                review = lineReviewRepository.saveAndFlush(review);

                mockMvc.perform(delete("/api/v1/reviews/{id}", review.getId()))
                                .andExpect(status().isNoContent());

                mockMvc.perform(get("/api/v1/reviews/{id}", review.getId()))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("not_found"));
        }

        @Test
        void deleteReview_whenEntityMissing_shouldReturnNotFound() throws Exception {
                mockMvc.perform(delete("/api/v1/reviews/{id}", 999999L))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("not_found"));
        }

        @Test
        void getVisibleSummaries_shouldReturnVisibleAggregatesOnly() throws Exception {
                LineReview line41a = new LineReview();
                line41a.setReviewerUserId(7701L);
                line41a.setLineId(41L);
                line41a.setRating(4);
                line41a.setRideDate(LocalDate.now().minusDays(1));
                line41a.setModerationStatus(ModerationStatus.VISIBLE);
                lineReviewRepository.save(line41a);

                LineReview line41b = new LineReview();
                line41b.setReviewerUserId(7702L);
                line41b.setLineId(41L);
                line41b.setRating(2);
                line41b.setRideDate(LocalDate.now().minusDays(2));
                line41b.setModerationStatus(ModerationStatus.VISIBLE);
                lineReviewRepository.save(line41b);

                LineReview line41hidden = new LineReview();
                line41hidden.setReviewerUserId(7703L);
                line41hidden.setLineId(41L);
                line41hidden.setRating(5);
                line41hidden.setRideDate(LocalDate.now().minusDays(2));
                line41hidden.setModerationStatus(ModerationStatus.HIDDEN);
                lineReviewRepository.save(line41hidden);

                LineReview line42 = new LineReview();
                line42.setReviewerUserId(7704L);
                line42.setLineId(42L);
                line42.setRating(5);
                line42.setRideDate(LocalDate.now().minusDays(1));
                line42.setModerationStatus(ModerationStatus.VISIBLE);
                lineReviewRepository.saveAndFlush(line42);

                mockMvc.perform(get("/api/v1/reviews/summary"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].lineId").value(41))
                                .andExpect(jsonPath("$[0].averageRating").value(3.0))
                                .andExpect(jsonPath("$[0].totalReviews").value(2))
                                .andExpect(jsonPath("$[1].lineId").value(42))
                                .andExpect(jsonPath("$[1].averageRating").value(5.0))
                                .andExpect(jsonPath("$[1].totalReviews").value(1));
        }

        @Test
        void getVisibleSummaryByLine_shouldReturnAggregate() throws Exception {
                LineReview first = new LineReview();
                first.setReviewerUserId(7801L);
                first.setLineId(51L);
                first.setRating(5);
                first.setRideDate(LocalDate.now().minusDays(1));
                first.setModerationStatus(ModerationStatus.VISIBLE);
                lineReviewRepository.save(first);

                LineReview second = new LineReview();
                second.setReviewerUserId(7802L);
                second.setLineId(51L);
                second.setRating(3);
                second.setRideDate(LocalDate.now().minusDays(2));
                second.setModerationStatus(ModerationStatus.VISIBLE);
                lineReviewRepository.saveAndFlush(second);

                mockMvc.perform(get("/api/v1/reviews/summary/51"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.lineId").value(51))
                                .andExpect(jsonPath("$.averageRating").value(4.0))
                                .andExpect(jsonPath("$.totalReviews").value(2));
        }

        @Test
        void getVisibleSummaryByLine_whenNoVisibleReviews_shouldReturnZeroSummary() throws Exception {
                mockMvc.perform(get("/api/v1/reviews/summary/999"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.lineId").value(999))
                                .andExpect(jsonPath("$.averageRating").value(0.0))
                                .andExpect(jsonPath("$.totalReviews").value(0));
        }

        @Test
        void getVisibleSummaryByLine_withInvalidPathVariable_shouldReturnBadRequest() throws Exception {
                mockMvc.perform(get("/api/v1/reviews/summary/0"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("bad_request"));
        }
}
