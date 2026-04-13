package ba.unsa.etf.pnwt.notificationservice.repository;

import ba.unsa.etf.pnwt.notificationservice.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByUserId(UUID userId);
    List<Subscription> findByLineId(UUID lineId);
    List<Subscription> findByUserIdAndIsActive(UUID userId, Boolean isActive);
    List<Subscription> findByUserFullNameContainingIgnoreCase(String name);
    List<Subscription> findByUserEmailIgnoreCase(String email);
}
