package ba.unsa.etf.pnwt.routingservice.repository;

import ba.unsa.etf.pnwt.routingservice.model.RoutePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RoutePointRepository extends JpaRepository<RoutePoint, Integer> {
    List<RoutePoint> findByDirection_IdOrderBySequenceOrderAsc(Integer directionId);
    void deleteByDirection_Id(Integer directionId);

    @Modifying
    @Query("delete from RoutePoint rp where rp.direction.id in :directionIds")
    void deleteByDirection_IdIn(@Param("directionIds") Collection<Integer> directionIds);
}
