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
                                .andExpect(jsonPath("$.error").value("validation_error"))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.path").value("/api/v1/reports"))
                                .andExpect(jsonPath("$.validationErrors", hasItem(startsWith("reporterUserId:"))))
                                .andExpect(jsonPath("$.validationErrors", hasItem(startsWith("description:"))))
                                .andExpect(jsonPath("$.validationErrors", hasItem(startsWith("category:"))));
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
                                .andExpect(jsonPath("$.error").value("malformed_json"))
                                .andExpect(jsonPath("$.path").value("/api/v1/reports"))
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

        @Test
        void patchReport_withJsonPatch_shouldApplyPartialUpdate() throws Exception {
                ProblemReport report = new ProblemReport();
                report.setReporterUserId(7001L);
                report.setLineId(44L);
                report.setStationId(101L);
                report.setCategory(ProblemCategory.DELAY);
                report.setDescription("Initial description");
                report.setPhotoUrls(List.of("https://example.com/report-initial.png"));
                report.setStatus(ReportStatus.RECEIVED);
                report = problemReportRepository.saveAndFlush(report);

                String patchPayload = """
                                [
                                        { "op": "replace", "path": "/description", "value": "Updated by JSON Patch" },
                                        { "op": "replace", "path": "/status", "value": "IN_PROGRESS" }
                                ]
                                """;

                mockMvc.perform(patch("/api/v1/reports/{id}", report.getId())
                                .contentType("application/json-patch+json")
                                .content(patchPayload))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.description").value("Updated by JSON Patch"))
                                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        void createReportsBatch_shouldReturnCreated() throws Exception {
                String payload = """
                                {
                                        "reports": [
                                                {
                                                        "reporterUserId": 8101,
                                                        "lineId": 6,
                                                        "stationId": 18,
                                                        "category": "DELAY",
                                                        "description": "Batch report one."
                                                },
                                                {
                                                        "reporterUserId": 8102,
                                                        "lineId": 6,
                                                        "vehicleId": 902,
                                                        "category": "CROWDING",
                                                        "description": "Batch report two."
                                                }
                                        ]
                                }
                                """;

                mockMvc.perform(post("/api/v1/reports/batch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.insertedCount").value(2))
                                .andExpect(jsonPath("$.reports.length()").value(2));
        }

        @Test
        void createReportsBatch_withEmptyReports_shouldReturnBadRequest() throws Exception {
                String payload = """
                                {
                                        "reports": []
                                }
                                """;

                mockMvc.perform(post("/api/v1/reports/batch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("validation_error"));
        }

        @Test
        void getReports_shouldApplyStatusAndReporterFilters() throws Exception {
                ProblemReport matching = new ProblemReport();
                matching.setReporterUserId(8801L);
                matching.setLineId(6L);
                matching.setStationId(611L);
                matching.setCategory(ProblemCategory.DELAY);
                matching.setDescription("Matching report");
                matching.setStatus(ReportStatus.IN_PROGRESS);
                matching.setPhotoUrls(List.of());
                problemReportRepository.save(matching);

                ProblemReport wrongStatus = new ProblemReport();
                wrongStatus.setReporterUserId(8801L);
                wrongStatus.setLineId(6L);
                wrongStatus.setStationId(612L);
                wrongStatus.setCategory(ProblemCategory.DELAY);
                wrongStatus.setDescription("Wrong status");
                wrongStatus.setStatus(ReportStatus.RECEIVED);
                wrongStatus.setPhotoUrls(List.of());
                problemReportRepository.save(wrongStatus);

                ProblemReport wrongReporter = new ProblemReport();
                wrongReporter.setReporterUserId(8802L);
                wrongReporter.setLineId(6L);
                wrongReporter.setStationId(613L);
                wrongReporter.setCategory(ProblemCategory.CROWDING);
                wrongReporter.setDescription("Wrong reporter");
                wrongReporter.setStatus(ReportStatus.IN_PROGRESS);
                wrongReporter.setPhotoUrls(List.of());
                problemReportRepository.saveAndFlush(wrongReporter);

                mockMvc.perform(get("/api/v1/reports")
                                .param("status", "IN_PROGRESS")
                                .param("reporterUserId", "8801")
                                .param("page", "0")
                                .param("size", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalElements").value(1))
                                .andExpect(jsonPath("$.content[0].reporterUserId").value(8801))
                                .andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"));
        }

        @Test
        void searchReports_shouldReturnPagedMatches() throws Exception {
                ProblemReport first = new ProblemReport();
                first.setReporterUserId(9201L);
                first.setLineId(33L);
                first.setStationId(301L);
                first.setCategory(ProblemCategory.DELAY);
                first.setDescription("Tram delay at station A");
                first.setStatus(ReportStatus.RECEIVED);
                first.setPhotoUrls(List.of());
                problemReportRepository.save(first);

                ProblemReport second = new ProblemReport();
                second.setReporterUserId(9202L);
                second.setLineId(33L);
                second.setStationId(302L);
                second.setCategory(ProblemCategory.CROWDING);
                second.setDescription("Crowded tram in peak hour");
                second.setStatus(ReportStatus.RECEIVED);
                second.setPhotoUrls(List.of());
                problemReportRepository.save(second);

                ProblemReport third = new ProblemReport();
                third.setReporterUserId(9203L);
                third.setLineId(33L);
                third.setStationId(303L);
                third.setCategory(ProblemCategory.OTHER);
                third.setDescription("Bus line feedback unrelated");
                third.setStatus(ReportStatus.RECEIVED);
                third.setPhotoUrls(List.of());
                problemReportRepository.saveAndFlush(third);

                mockMvc.perform(get("/api/v1/reports/search")
                                .param("q", "tram")
                                .param("page", "0")
                                .param("size", "1")
                                .param("sort", "createdAt,desc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalElements").value(2))
                                .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        void searchReports_withBlankKeyword_shouldReturnBadRequest() throws Exception {
                mockMvc.perform(get("/api/v1/reports/search")
                                .param("q", "   "))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("bad_request"))
                                .andExpect(jsonPath("$.message").value("Search keyword must not be blank."));
        }

        @Test
        void patchReport_withInvalidPayload_shouldReturnBadRequest() throws Exception {
                ProblemReport report = new ProblemReport();
                report.setReporterUserId(9501L);
                report.setLineId(44L);
                report.setStationId(111L);
                report.setCategory(ProblemCategory.DELAY);
                report.setDescription("Patch target");
                report.setPhotoUrls(List.of());
                report.setStatus(ReportStatus.RECEIVED);
                report = problemReportRepository.saveAndFlush(report);

                mockMvc.perform(patch("/api/v1/reports/{id}", report.getId())
                                .contentType("application/json-patch+json")
                                .content("{}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("bad_request"))
                                .andExpect(jsonPath("$.message")
                                                .value("JSON Patch payload must be an array of operations."));
        }

        @Test
        void getReport_shouldReturnSingleEntity() throws Exception {
                ProblemReport report = new ProblemReport();
                report.setReporterUserId(9601L);
                report.setLineId(67L);
                report.setStationId(701L);
                report.setCategory(ProblemCategory.OTHER);
                report.setDescription("Single report fetch");
                report.setPhotoUrls(List.of("https://example.com/single.png"));
                report.setStatus(ReportStatus.RECEIVED);
                report = problemReportRepository.saveAndFlush(report);

                mockMvc.perform(get("/api/v1/reports/{id}", report.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(report.getId()))
                                .andExpect(jsonPath("$.reporterUserId").value(9601))
                                .andExpect(jsonPath("$.description").value("Single report fetch"));
        }

        @Test
        void getReport_withInvalidPathVariable_shouldReturnBadRequest() throws Exception {
                mockMvc.perform(get("/api/v1/reports/0"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("bad_request"));
        }

        @Test
        void countReportsByLine_shouldReturnTotal() throws Exception {
                ProblemReport first = new ProblemReport();
                first.setReporterUserId(9301L);
                first.setLineId(55L);
                first.setStationId(401L);
                first.setCategory(ProblemCategory.DELAY);
                first.setDescription("First count report");
                first.setStatus(ReportStatus.RECEIVED);
                first.setPhotoUrls(List.of());
                problemReportRepository.save(first);

                ProblemReport second = new ProblemReport();
                second.setReporterUserId(9302L);
                second.setLineId(55L);
                second.setStationId(402L);
                second.setCategory(ProblemCategory.CROWDING);
                second.setDescription("Second count report");
                second.setStatus(ReportStatus.RECEIVED);
                second.setPhotoUrls(List.of());
                problemReportRepository.save(second);

                ProblemReport otherLine = new ProblemReport();
                otherLine.setReporterUserId(9303L);
                otherLine.setLineId(77L);
                otherLine.setStationId(403L);
                otherLine.setCategory(ProblemCategory.OTHER);
                otherLine.setDescription("Other line report");
                otherLine.setStatus(ReportStatus.RECEIVED);
                otherLine.setPhotoUrls(List.of());
                problemReportRepository.saveAndFlush(otherLine);

                mockMvc.perform(get("/api/v1/reports/line/55/count"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.lineId").value(55))
                                .andExpect(jsonPath("$.totalReports").value(2));
        }

        @Test
        void countReportsByLine_withInvalidPathVariable_shouldReturnBadRequest() throws Exception {
                mockMvc.perform(get("/api/v1/reports/line/0/count"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("bad_request"));
        }

        @Test
        void deleteReport_shouldRemoveEntityAndReturnNoContent() throws Exception {
                ProblemReport report = new ProblemReport();
                report.setReporterUserId(9401L);
                report.setLineId(66L);
                report.setStationId(501L);
                report.setCategory(ProblemCategory.DELAY);
                report.setDescription("Delete me");
                report.setStatus(ReportStatus.RECEIVED);
                report.setPhotoUrls(List.of());
                report = problemReportRepository.saveAndFlush(report);

                mockMvc.perform(delete("/api/v1/reports/{id}", report.getId()))
                                .andExpect(status().isNoContent());

                mockMvc.perform(get("/api/v1/reports/{id}", report.getId()))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("not_found"));
        }

        @Test
        void deleteReport_whenEntityMissing_shouldReturnNotFound() throws Exception {
                mockMvc.perform(delete("/api/v1/reports/{id}", 999999L))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("not_found"));
        }

        @Test
        void updateStatus_shouldReturnUpdatedReport() throws Exception {
                ProblemReport report = new ProblemReport();
                report.setReporterUserId(9701L);
                report.setLineId(78L);
                report.setStationId(801L);
                report.setCategory(ProblemCategory.DELAY);
                report.setDescription("Status update target");
                report.setPhotoUrls(List.of());
                report.setStatus(ReportStatus.RECEIVED);
                report = problemReportRepository.saveAndFlush(report);

                String payload = """
                                {
                                  "status": "RESOLVED"
                                }
                                """;

                mockMvc.perform(patch("/api/v1/reports/{id}/status", report.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(report.getId()))
                                .andExpect(jsonPath("$.status").value("RESOLVED"));
        }

        @Test
        void updateStatus_withValidationError_shouldReturnBadRequest() throws Exception {
                ProblemReport report = new ProblemReport();
                report.setReporterUserId(9801L);
                report.setLineId(79L);
                report.setStationId(901L);
                report.setCategory(ProblemCategory.OTHER);
                report.setDescription("Validation target");
                report.setPhotoUrls(List.of());
                report.setStatus(ReportStatus.RECEIVED);
                report = problemReportRepository.saveAndFlush(report);

                mockMvc.perform(patch("/api/v1/reports/{id}/status", report.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("bad_request"));
        }
}
