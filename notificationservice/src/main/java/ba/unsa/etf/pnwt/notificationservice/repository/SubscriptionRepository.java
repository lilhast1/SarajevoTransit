package ba.unsa.etf.pnwt.notificationservice.repository;

import ba.unsa.etf.pnwt.notificationservice.model.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUserId(Long userId);
    List<Subscription> findByLineId(Long lineId);
    Page<Subscription> findByLineId(Long lineId, Pageable pageable);
    List<Subscription> findByLineIdAndIsActive(Long lineId, Boolean isActive);
    List<Subscription> findByUserIdAndIsActive(Long userId, Boolean isActive);
    List<Subscription> findByUserFullNameContainingIgnoreCase(String name);
    List<Subscription> findByUserEmailIgnoreCase(String email);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.lineId = :lineId AND s.isActive = true")
    long countActiveByLineId(@Param("lineId") Long lineId);

    @Query("""
            SELECT s FROM Subscription s
            WHERE s.isActive = true AND s.lineId = :lineId
              AND s.startInterval <= :targetTime AND s.endInterval >= :targetTime
              AND s.daysOfWeek LIKE CONCAT('%', :dayAbbr, '%')
            """)
    List<Subscription> findActiveForLineAtTime(@Param("lineId") Long lineId,
                                               @Param("targetTime") LocalTime targetTime,
                                               @Param("dayAbbr") String dayAbbr);
}
