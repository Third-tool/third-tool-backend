package com.example.thirdtool.UserSchedule.infrastructure.persistence;

import com.example.thirdtool.UserSchedule.domain.model.UserScheduleConfigHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserScheduleConfigHistoryRepository
        extends JpaRepository<UserScheduleConfigHistory, Long> {

    @Query("""
            SELECT h FROM UserScheduleConfigHistory h
            WHERE h.userScheduleConfig.id = :configId
            ORDER BY h.changedAt DESC
            """)
    List<UserScheduleConfigHistory> findByUserScheduleConfig_IdOrderByChangedAtDesc(
            @Param("configId") Long userScheduleConfigId,
            Pageable pageable
                                                                                   );

    default List<UserScheduleConfigHistory> findByUserScheduleConfig_IdOrderByChangedAtDesc(
            Long userScheduleConfigId, int limit
                                                                                           ) {
        return findByUserScheduleConfig_IdOrderByChangedAtDesc(
                userScheduleConfigId,
                org.springframework.data.domain.PageRequest.of(0, limit)
                                                              );
    }
}

