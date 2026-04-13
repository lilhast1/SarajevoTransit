package ba.unsa.etf.pnwt.routingservice.repository;

import ba.unsa.etf.pnwt.routingservice.model.Direction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DirectionRepository extends JpaRepository<Direction, Integer> {
    @EntityGraph(attributePaths = "line")
    List<Direction> findByLine_IdAndIsActiveTrue(Integer lineId);

    @EntityGraph(attributePaths = "line")
    List<Direction> findByLine_Id(Integer lineId);

    @EntityGraph(attributePaths = "line")
    Optional<Direction> findByIdAndIsActiveTrue(Integer id);

    Optional<Direction> findByExternalId(Integer externalId);
    List<Direction> findByExternalIdIn(List<Integer> externalIds);
    boolean existsByExternalId(Integer externalId);
    boolean existsByExternalIdAndIdNot(Integer externalId, Integer id);

    @EntityGraph(attributePaths = "line")
    List<Direction> findByIsActiveTrue();

    @Modifying
    @Query("update Direction d set d.isActive = false")
    int deactivateAll();
}
