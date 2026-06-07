package com.sarajevotransit.feedbackservice.repository;

import com.sarajevotransit.feedbackservice.dto.LineRatingSummaryResponse;
import com.sarajevotransit.feedbackservice.model.LineReview;
import com.sarajevotransit.feedbackservice.model.ModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LineReviewRepository extends JpaRepository<LineReview, Long> {

        Page<LineReview> findByLineId(Long lineId, Pageable pageable);

        Page<LineReview> findByModerationStatus(ModerationStatus moderationStatus, Pageable pageable);

        Page<LineReview> findByLineIdAndModerationStatus(Long lineId,
                        ModerationStatus moderationStatus,
                        Pageable pageable);

        Page<LineReview> findByReviewerUserId(Long reviewerUserId, Pageable pageable);

        List<LineReview> findByLineIdOrderByCreatedAtDesc(Long lineId);

        List<LineReview> findByLineIdAndModerationStatusOrderByCreatedAtDesc(Long lineId,
                        ModerationStatus moderationStatus);

        List<LineReview> findByReviewerUserIdOrderByCreatedAtDesc(Long reviewerUserId);

        Optional<LineReview> findFirstByLineIdAndModerationStatusOrderByCreatedAtDesc(
                        Long lineId,
                        ModerationStatus moderationStatus);

        Optional<LineReview> findByReviewerUserIdAndLineId(Long reviewerUserId, Long lineId);

        @Query("SELECT AVG(lr.rating) FROM LineReview lr WHERE lr.moderationStatus = com.sarajevotransit.feedbackservice.model.ModerationStatus.VISIBLE")
        Double globalAverageRating();

        @Query("SELECT COUNT(lr) FROM LineReview lr WHERE lr.moderationStatus = com.sarajevotransit.feedbackservice.model.ModerationStatus.VISIBLE")
        long countVisible();

        @Query("SELECT lr.rating AS rating, COUNT(lr) AS cnt FROM LineReview lr WHERE lr.moderationStatus = com.sarajevotransit.feedbackservice.model.ModerationStatus.VISIBLE GROUP BY lr.rating ORDER BY lr.rating")
        List<Object[]> ratingDistribution();

        @Query(value = """
                SELECT TO_CHAR(created_at, 'YYYY-MM') AS month,
                       AVG(rating)                     AS avgRating,
                       COUNT(*)                        AS cnt
                FROM reviews
                WHERE moderation_status = 'VISIBLE'
                  AND created_at >= NOW() - INTERVAL '6 months'
                GROUP BY TO_CHAR(created_at, 'YYYY-MM')
                ORDER BY month
                """, nativeQuery = true)
        List<Object[]> ratingTrend();

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