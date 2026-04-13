package com.sarajevotransit.userservice.repository;

import com.sarajevotransit.userservice.model.TravelHistoryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TravelHistoryRepository extends JpaRepository<TravelHistoryEntry, Long> {

    List<TravelHistoryEntry> findByUserIdOrderByTraveledAtDesc(Long userId);

    Page<TravelHistoryEntry> findByUserId(Long userId, Pageable pageable);

    @Query("""
            select t.lineCode as lineCode, count(t.id) as usageCount
            from TravelHistoryEntry t
            where t.user.id = :userId
            group by t.lineCode
            order by count(t.id) desc
            """)
    List<LineUsageView> findLineUsageStats(Long userId);

    interface LineUsageView {
        String getLineCode();

        Long getUsageCount();
    }
}
