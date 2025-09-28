package com.example.thirdtool.Stats.application.service;

import com.example.thirdtool.Stats.domain.repository.UserCardProgressRepository;
import com.example.thirdtool.Stats.presentation.dto.TagStudyCountDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StatsService {

    private final UserCardProgressRepository progressRepository;

    public List<TagStudyCountDto> getThisMonthTagRatio(Long userId) {
        // ✅ '이번 달 1일 00:00:00'을 시작 날짜로 계산
        LocalDateTime startOfMonth = LocalDate.now()
            .withDayOfMonth(1)
            .atStartOfDay();

        // 리포지토리를 호출하여 데이터를 가져옵니다.
        return progressRepository.findTagStudyCounts(userId, startOfMonth);
    }
}
