package ba.unsa.etf.pnwt.routingservice.repository;

import ba.unsa.etf.pnwt.routingservice.model.Line;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LineRepository extends JpaRepository<Line, Integer> {
    @EntityGraph(attributePaths = "vehicleType")
    List<Line> findByIsActiveTrue();

    Optional<Line> findByExternalId(Integer externalId);
    List<Line> findByExternalIdIn(List<Integer> externalIds);
    boolean existsByExternalId(Integer externalId);
    boolean existsByExternalIdAndIdNot(Integer externalId, Integer id);

    @EntityGraph(attributePaths = "vehicleType")
    List<Line> findByVehicleType_Id(Short vehicleTypeId);

    @EntityGraph(attributePaths = "vehicleType")
    List<Line> findByVehicleType_IdAndIsActiveTrue(Short vehicleTypeId);

    @Modifying
    @Query("update Line l set l.isActive = false")
    int deactivateAll();
}
