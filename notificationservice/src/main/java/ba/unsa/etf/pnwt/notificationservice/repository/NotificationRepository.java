package ba.unsa.etf.pnwt.notificationservice.repository;

import ba.unsa.etf.pnwt.notificationservice.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserId(Long userId);
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    List<Notification> findByUserIdAndIsRead(Long userId, Boolean isRead);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.sentAt BETWEEN :from AND :to ORDER BY n.sentAt DESC")
    List<Notification> findByUserIdAndSentAtBetween(@Param("userId") Long userId,
                                                    @Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);
}
