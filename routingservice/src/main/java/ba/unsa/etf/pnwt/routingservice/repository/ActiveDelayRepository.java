package ba.unsa.etf.pnwt.routingservice.repository;

import ba.unsa.etf.pnwt.routingservice.model.ActiveDelay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActiveDelayRepository extends JpaRepository<ActiveDelay, ActiveDelay.PK> {
}
