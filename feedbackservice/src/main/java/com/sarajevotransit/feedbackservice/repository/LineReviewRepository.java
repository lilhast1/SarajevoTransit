package com.sarajevotransit.feedbackservice.repository;

import com.sarajevotransit.feedbackservice.dto.LineRatingSummaryResponse;
import com.sarajevotransit.feedbackservice.model.LineReview;
import com.sarajevotransit.feedbackservice.model.ModerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LineReviewRepository extends JpaRepository<LineReview, Long> {

        List<LineReview> findByLineIdOrderByCreatedAtDesc(Long lineId);

        List<LineReview> findByLineIdAndModerationStatusOrderByCreatedAtDesc(Long lineId,
                        ModerationStatus moderationStatus);

        List<LineReview> findByReviewerUserIdOrderByCreatedAtDesc(Long reviewerUserId);

        @Query("""
                        select new com.sarajevotransit.feedbackservice.dto.LineRatingSummaryResponse(
                            lr.lineId,
                            avg(lr.rating),
                            count(lr.id)
                        )
                        from LineReview lr
                        where lr.moderationStatus = com.sarajevotransit.feedbackservice.model.ModerationStatus.VISIBLE
                        group by lr.lineId
                        order by lr.lineId
                        """)
        List<LineRatingSummaryResponse> fetchVisibleLineRatingSummaries();

        @Query("""
                        select new com.sarajevotransit.feedbackservice.dto.LineRatingSummaryResponse(
                                            lr.lineId,
                            avg(lr.rating),
                            count(lr.id)
                        )
                        from LineReview lr
                                    where lr.lineId = :lineId
                          and lr.moderationStatus = com.sarajevotransit.feedbackservice.model.ModerationStatus.VISIBLE
                                    group by lr.lineId
                        """)
        List<LineRatingSummaryResponse> fetchVisibleSummaryByLineId(@Param("lineId") Long lineId);
}