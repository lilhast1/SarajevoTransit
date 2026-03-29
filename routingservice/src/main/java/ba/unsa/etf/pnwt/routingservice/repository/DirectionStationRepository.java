package ba.unsa.etf.pnwt.routingservice.repository;

import ba.unsa.etf.pnwt.routingservice.model.DirectionStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DirectionStationRepository extends JpaRepository<DirectionStation, Integer> {
    List<DirectionStation> findByDirection_IdOrderByStopSequenceAsc(Integer directionId);
    List<DirectionStation> findByStation_Id(Integer stationId);
    void deleteByDirection_Id(Integer directionId);

    @Modifying
    @Query("delete from DirectionStation ds where ds.direction.id in :directionIds")
    void deleteByDirection_IdIn(@Param("directionIds") Collection<Integer> directionIds);
}
