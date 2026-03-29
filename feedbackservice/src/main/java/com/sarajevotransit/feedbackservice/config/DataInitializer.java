package com.sarajevotransit.feedbackservice.config;

import com.sarajevotransit.feedbackservice.model.LineReview;
import com.sarajevotransit.feedbackservice.model.ModerationStatus;
import com.sarajevotransit.feedbackservice.model.ProblemCategory;
import com.sarajevotransit.feedbackservice.model.ProblemReport;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import com.sarajevotransit.feedbackservice.repository.LineReviewRepository;
import com.sarajevotransit.feedbackservice.repository.ProblemReportRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedFeedbackData(ProblemReportRepository problemReportRepository,
            LineReviewRepository lineReviewRepository) {
        return args -> {
            if (problemReportRepository.count() == 0) {
                ProblemReport report1 = new ProblemReport();
                report1.setReporterUserId(1001L);
                report1.setLineId(3L);
                report1.setVehicleId(304L);
                report1.setVehicleRegistrationNumber("A12-E-345");
                report1.setVehicleInternalId("304");
                report1.setVehicleType("TRAM");
                report1.setCategory(ProblemCategory.CROWDING);
                report1.setDescription("Large crowd during morning rush hour, difficult to board.");
                report1.setPhotoUrls(List.of("https://example.com/photos/report-1.jpg"));
                report1.setStatus(ReportStatus.RECEIVED);

                ProblemReport report2 = new ProblemReport();
                report2.setReporterUserId(1002L);
                report2.setLineId(31L);
                report2.setStationId(10L);
                report2.setCategory(ProblemCategory.HYGIENE);
                report2.setDescription("Bus stop area was unclean and had overflowing trash bins.");
                report2.setStatus(ReportStatus.IN_PROGRESS);

                problemReportRepository.saveAll(List.of(report1, report2));
            }

            if (lineReviewRepository.count() == 0) {
                LineReview review1 = new LineReview();
                review1.setReviewerUserId(2001L);
                review1.setLineId(3L);
                review1.setRating(5);
                review1.setReviewText("Very reliable line and good frequency in peak hours.");
                review1.setRideDate(LocalDate.now().minusDays(3));
                review1.setModerationStatus(ModerationStatus.VISIBLE);

                LineReview review2 = new LineReview();
                review2.setReviewerUserId(2002L);
                review2.setLineId(31L);
                review2.setRating(3);
                review2.setReviewText("Service was acceptable, but delay was around 10 minutes.");
                review2.setRideDate(LocalDate.now().minusDays(7));
                review2.setModerationStatus(ModerationStatus.VISIBLE);

                lineReviewRepository.saveAll(List.of(review1, review2));
            }
        };
    }
}