package com.sarajevotransit.feedbackservice.repository;

import com.sarajevotransit.feedbackservice.model.ProblemReport;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemReportRepository extends JpaRepository<ProblemReport, Long> {

    @EntityGraph(attributePaths = "photoUrls")
    List<ProblemReport> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "photoUrls")
    List<ProblemReport> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    @EntityGraph(attributePaths = "photoUrls")
    List<ProblemReport> findByReporterUserIdOrderByCreatedAtDesc(Long reporterUserId);

    @EntityGraph(attributePaths = "photoUrls")
    List<ProblemReport> findByLineIdOrderByCreatedAtDesc(Long lineId);

    @EntityGraph(attributePaths = "photoUrls")
    List<ProblemReport> findByStatusAndReporterUserIdOrderByCreatedAtDesc(ReportStatus status, Long reporterUserId);
}
