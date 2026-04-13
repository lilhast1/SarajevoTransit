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
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.fieldErrors.rating").exists())
                                .andExpect(jsonPath("$.fieldErrors.rideDate").exists());
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
                                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
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
}
