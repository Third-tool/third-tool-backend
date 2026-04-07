
package com.example.thirdtool.UserSchedule.infrastructure.persistence;

import com.example.thirdtool.UserSchedule.domain.model.UserScheduleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserScheduleConfigRepository extends JpaRepository<UserScheduleConfig, Long> {

    Optional<UserScheduleConfig> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}