package com.sarajevotransit.feedbackservice.repository;

import com.sarajevotransit.feedbackservice.dto.LineRatingSummaryResponse;
import com.sarajevotransit.feedbackservice.model.LineReview;
import com.sarajevotransit.feedbackservice.model.ModerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LineReviewRepository extends JpaRepository<LineReview, Long> {

    List<LineReview> findByLineCodeOrderByCreatedAtDesc(String lineCode);

    List<LineReview> findByLineCodeAndModerationStatusOrderByCreatedAtDesc(String lineCode,
            ModerationStatus moderationStatus);

    @Query("""
            select new com.sarajevotransit.feedbackservice.dto.LineRatingSummaryResponse(
                lr.lineCode,
                avg(lr.rating),
                count(lr.id)
            )
            from LineReview lr
            where lr.moderationStatus = com.sarajevotransit.feedbackservice.model.ModerationStatus.VISIBLE
            group by lr.lineCode
            order by lr.lineCode
            """)
    List<LineRatingSummaryResponse> fetchVisibleLineRatingSummaries();

    @Query("""
            select new com.sarajevotransit.feedbackservice.dto.LineRatingSummaryResponse(
                lr.lineCode,
                avg(lr.rating),
                count(lr.id)
            )
            from LineReview lr
            where lr.lineCode = :lineCode
              and lr.moderationStatus = com.sarajevotransit.feedbackservice.model.ModerationStatus.VISIBLE
            group by lr.lineCode
            """)
    List<LineRatingSummaryResponse> fetchVisibleSummaryByLineCode(@Param("lineCode") String lineCode);
}