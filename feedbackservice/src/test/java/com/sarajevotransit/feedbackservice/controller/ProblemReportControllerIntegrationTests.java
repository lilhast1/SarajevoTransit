package com.sarajevotransit.feedbackservice.controller;

import com.sarajevotransit.feedbackservice.model.ProblemCategory;
import com.sarajevotransit.feedbackservice.model.ProblemReport;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class ProblemReportControllerIntegrationTests {

        private MockMvc mockMvc;

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private ProblemReportRepository problemReportRepository;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
                problemReportRepository.deleteAll();
        }

        @Test
        void createReport_shouldReturnCreated() throws Exception {
                String payload = """
                                {
                                  "reporterUserId": 5101,
                                  "lineId": 6,
                                  "stationId": 18,
                                  "category": "DELAY",
                                  "description": "Delay around 8 minutes at evening peak.",
                                  "photoUrls": ["https://example.com/evidence/report-ok.png"]
                                }
                                """;

                mockMvc.perform(post("/api/v1/reports")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isCreated())
                                .andExpect(header().exists("Location"))
                                .andExpect(jsonPath("$.status").value("RECEIVED"))
                                .andExpect(jsonPath("$.reporterUserId").value(5101))
                                .andExpect(jsonPath("$.category").value("DELAY"));
        }

        @Test
        void createReport_withValidationError_shouldReturnBadRequest() throws Exception {
                String payload = """
                                {
                                  "reporterUserId": -1,
                                  "lineId": 6,
                                  "description": ""
                                }
                                """;

                mockMvc.perform(post("/api/v1/reports")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.fieldErrors.reporterUserId").exists())
                                .andExpect(jsonPath("$.fieldErrors.description").exists())
                                .andExpect(jsonPath("$.fieldErrors.category").exists());
        }

        @Test
        void createReport_withInvalidEnum_shouldReturnBadRequest() throws Exception {
                String payload = """
                                {
                                  "reporterUserId": 5101,
                                  "lineId": 6,
                                  "stationId": 18,
                                  "category": "NOT_A_REAL_CATEGORY",
                                  "description": "Enum parse should fail"
                                }
                                """;

                mockMvc.perform(post("/api/v1/reports")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void createReport_withMalformedJson_shouldReturnBadRequest() throws Exception {
                String payload = """
                                {
                                  "reporterUserId": 5101,
                                  "lineId": 6,
                                  "stationId": 18,
                                  "category": "DELAY",
                                  "description": "Broken json"
                                """;

                mockMvc.perform(post("/api/v1/reports")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
        }

        @Test
        void getReportsByLine_shouldReturnFilteredList() throws Exception {
                ProblemReport report = new ProblemReport();
                report.setReporterUserId(7001L);
                report.setLineId(33L);
                report.setStationId(99L);
                report.setCategory(ProblemCategory.CROWDING);
                report.setDescription("Crowded platform.");
                report.setPhotoUrls(List.of("https://example.com/img1.png"));
                report.setStatus(ReportStatus.RECEIVED);
                problemReportRepository.saveAndFlush(report);

                mockMvc.perform(get("/api/v1/reports/line/33")
                                .param("page", "0")
                                .param("size", "5")
                                .param("sort", "createdAt,desc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].lineId").value(33))
                                .andExpect(jsonPath("$.totalElements").value(1));
        }
}
