package com.sarajevotransit.feedbackservice.service;

import com.sarajevotransit.feedbackservice.dto.LineModerationResponse;
import com.sarajevotransit.feedbackservice.client.NotificationServiceClient;
import com.sarajevotransit.feedbackservice.client.RoutingServiceClient;
import com.sarajevotransit.feedbackservice.exception.NotFoundException;
import com.sarajevotransit.feedbackservice.model.LineReview;
import com.sarajevotransit.feedbackservice.model.ModerationStatus;
import com.sarajevotransit.feedbackservice.model.ProblemReport;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import com.sarajevotransit.feedbackservice.repository.LineReviewRepository;
import com.sarajevotransit.feedbackservice.repository.ProblemReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackWorkflowService {

    private final ProblemReportRepository problemReportRepository;
    private final LineReviewRepository lineReviewRepository;
    private final RoutingServiceClient routingServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    @Transactional
    public LineModerationResponse moderateLineFeedback(
            Long lineId,
            ReportStatus targetReportStatus,
            ModerationStatus targetModerationStatus) {
        routingServiceClient.validateLine(lineId);

        List<ProblemReport> reports = problemReportRepository.findByLineIdOrderByCreatedAtDesc(lineId);
        List<LineReview> reviews = lineReviewRepository.findByLineIdOrderByCreatedAtDesc(lineId);

        if (reports.isEmpty() && reviews.isEmpty()) {
            throw new NotFoundException("No feedback entries found for lineId=" + lineId);
        }

        int updatedReports = 0;
        for (ProblemReport report : reports) {
            if (report.getStatus() != targetReportStatus) {
                report.setStatus(targetReportStatus);
                updatedReports++;
            }
        }

        int updatedReviews = 0;
        for (LineReview review : reviews) {
            if (review.getModerationStatus() != targetModerationStatus) {
                review.setModerationStatus(targetModerationStatus);
                updatedReviews++;
            }
        }

        if (!reports.isEmpty()) {
            problemReportRepository.saveAll(reports);
        }
        if (!reviews.isEmpty()) {
            lineReviewRepository.saveAll(reviews);
        }

        reports.stream()
            .filter(report -> report.getReporterUserId() != null)
            .forEach(report -> notificationServiceClient.notifyReportStatusChange(
                report.getId(),
                lineId,
                report.getReporterUserId(),
                report.getCategory() != null ? report.getCategory().name() : null,
                targetReportStatus.name()));

        reviews.stream()
            .filter(review -> review.getReviewerUserId() != null)
            .forEach(review -> notificationServiceClient.notifyModerationFlag(
                review.getId(),
                lineId,
                review.getReviewerUserId()));

        return new LineModerationResponse(
                lineId,
                targetReportStatus,
                targetModerationStatus,
                updatedReports,
                updatedReviews);
    }
}
