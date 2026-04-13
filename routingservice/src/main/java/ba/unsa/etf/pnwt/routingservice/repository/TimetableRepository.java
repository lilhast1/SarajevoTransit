package ba.unsa.etf.pnwt.routingservice.repository;

import ba.unsa.etf.pnwt.routingservice.model.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, Integer> {
    List<Timetable> findByIsActiveTrueOrderByDepartureTimeAsc();
    List<Timetable> findByLine_IdAndIsActiveTrueOrderByDepartureTimeAsc(Integer lineId);
    List<Timetable> findByDirection_IdAndIsActiveTrueOrderByDepartureTimeAsc(Integer directionId);
    Optional<Timetable> findByExternalId(Integer externalId);
    List<Timetable> findByExternalIdIn(List<Integer> externalIds);
    boolean existsByExternalId(Integer externalId);
    boolean existsByExternalIdAndIdNot(Integer externalId, Integer id);
    List<Timetable> findByDepartureTimeBetweenAndIsActiveTrueOrderByDepartureTimeAsc(LocalTime from, LocalTime to);
    List<Timetable> findByValidFromLessThanEqualAndValidToGreaterThanEqualAndIsActiveTrue(LocalDate date1, LocalDate date2);
    void deleteByLine_Id(Integer lineId);

    @Modifying
    @Query("delete from Timetable t where t.line.id in :lineIds")
    void deleteByLine_IdIn(@Param("lineIds") Collection<Integer> lineIds);

    @Modifying
    @Query("update Timetable t set t.isActive = false")
    int deactivateAll();
}
