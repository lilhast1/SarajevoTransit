package com.sarajevotransit.feedbackservice.repository;

import com.sarajevotransit.feedbackservice.model.ProblemReport;
import com.sarajevotransit.feedbackservice.model.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProblemReportRepository extends JpaRepository<ProblemReport, Long> {

    @Override
    @EntityGraph(attributePaths = "photoUrls")
    Page<ProblemReport> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "photoUrls")
    Page<ProblemReport> findByStatus(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "photoUrls")
    Page<ProblemReport> findByReporterUserId(Long reporterUserId, Pageable pageable);

    @EntityGraph(attributePaths = "photoUrls")
    Page<ProblemReport> findByLineId(Long lineId, Pageable pageable);

    @EntityGraph(attributePaths = "photoUrls")
    Page<ProblemReport> findByStatusAndReporterUserId(ReportStatus status, Long reporterUserId, Pageable pageable);

    @EntityGraph(attributePaths = "photoUrls")
    @Query("""
            select pr
            from ProblemReport pr
            where lower(pr.description) like lower(concat('%', :keyword, '%'))
              and (:status is null or pr.status = :status)
            """)
    Page<ProblemReport> searchByDescriptionKeyword(
            @Param("keyword") String keyword,
            @Param("status") ReportStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = "photoUrls")
    List<ProblemReport> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "photoUrls")
    List<ProblemReport> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    @EntityGraph(attributePaths = "photoUrls")
    List<ProblemReport> findByReporterUserIdOrderByCreatedAtDesc(Long reporterUserId);

    @EntityGraph(attributePaths = "photoUrls")
    List<ProblemReport> findByLineIdOrderByCreatedAtDesc(Long lineId);

    long countByLineId(Long lineId);

    @EntityGraph(attributePaths = "photoUrls")
    List<ProblemReport> findByStatusAndReporterUserIdOrderByCreatedAtDesc(ReportStatus status, Long reporterUserId);

    @Query("SELECT pr.category AS cat, COUNT(pr) AS cnt FROM ProblemReport pr GROUP BY pr.category")
    List<Object[]> countByCategory();

    @Query("SELECT pr.status AS status, COUNT(pr) AS cnt FROM ProblemReport pr GROUP BY pr.status")
    List<Object[]> countByReportStatus();

    @Query(value = """
            SELECT line_id AS lineId, COUNT(*) AS cnt
            FROM incident_reports
            WHERE line_id IS NOT NULL
            GROUP BY line_id
            ORDER BY cnt DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Object[]> topReportedLineIds();
}
