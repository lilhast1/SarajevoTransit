package ba.unsa.etf.pnwt.routingservice.repository;

import ba.unsa.etf.pnwt.routingservice.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Integer> {
    List<Station> findByIsActiveTrue();
    List<Station> findByIsActive(boolean isActive);
    List<Station> findByNameContainingIgnoreCase(String name);
    List<Station> findByNameContainingIgnoreCaseAndIsActive(String name, boolean isActive);
    Optional<Station> findByExternalId(Integer externalId);
    List<Station> findByExternalIdIn(List<Integer> externalIds);
    boolean existsByExternalId(Integer externalId);
    boolean existsByExternalIdAndIdNot(Integer externalId, Integer id);

    @Modifying
    @Query("update Station s set s.isActive = false")
    int deactivateAll();
}
