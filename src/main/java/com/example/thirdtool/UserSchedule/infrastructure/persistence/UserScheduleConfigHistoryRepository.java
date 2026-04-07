package com.example.thirdtool.UserSchedule.infrastructure.persistence;

import com.example.thirdtool.UserSchedule.domain.model.UserScheduleConfigHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface UserScheduleConfigHistoryRepository
        extends JpaRepository<UserScheduleConfigHistory, Long> {

    List<UserScheduleConfigHistory> findByUserScheduleConfig_IdOrderByChangedAtAsc(
            Long userScheduleConfigId);
}

