package com.sarajevotransit.feedbackservice.repository;

import com.sarajevotransit.feedbackservice.model.ProblemReport;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemReportRepository extends JpaRepository<ProblemReport, Long> {

    List<ProblemReport> findAllByOrderByCreatedAtDesc();

    List<ProblemReport> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<ProblemReport> findByReporterUserIdOrderByCreatedAtDesc(Long reporterUserId);

    List<ProblemReport> findByStatusAndReporterUserIdOrderByCreatedAtDesc(ReportStatus status, Long reporterUserId);
}
