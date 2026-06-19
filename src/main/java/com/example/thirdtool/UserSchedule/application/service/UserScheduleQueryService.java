package com.example.thirdtool.UserSchedule.application.service;

import com.example.thirdtool.Card.domain.model.OnFieldBudget;
import com.example.thirdtool.Card.domain.model.SoftScheduleTemplate;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.UserSchedule.domain.exception.UserScheduleDomainException;
import com.example.thirdtool.UserSchedule.domain.model.LearningModeMappingPolicy;
import com.example.thirdtool.UserSchedule.domain.model.UserScheduleConfig;
import com.example.thirdtool.UserSchedule.infrastructure.persistence.UserScheduleConfigHistoryRepository;
import com.example.thirdtool.UserSchedule.infrastructure.persistence.UserScheduleConfigRepository;
import com.example.thirdtool.UserSchedule.presentation.dto.UserScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserScheduleQueryService {

    // ─── 이력 조회 제한 ─────────────────────────────────────────
    private static final int DEFAULT_HISTORY_LIMIT = 20;
    private static final int MAX_HISTORY_LIMIT      = 50;

    private final UserScheduleConfigRepository        configRepository;
    private final UserScheduleConfigHistoryRepository historyRepository;
    private final LearningModeMappingPolicy           mappingPolicy;

    public UserScheduleResponse.Get getSchedule(Long userId) {
        UserScheduleConfig config = configRepository.findByUserId(userId)
                                                    .orElseGet(() -> initDefault(userId));
        return UserScheduleResponse.Get.of(config);
    }

    /**
     * Cross-BC inbound — Card BC {@link OnFieldBudget}을 사용자별 설정에서 파생해 반환한다.
     *
     * <p>Review BC가 매 노출(`recordView`)·세션 진입 시점에 호출한다. 설정 미보유 유저는
     * 첫 호출 시 기본 모드(MODE_10D)로 자동 초기화 — Story 4-2 "최초 설정 미완료 유저는
     * 기본값(10일 모드) 자동 초기화".
     *
     * <p>{@code UserScheduleConfig} 엔티티 자체는 BC 경계 밖으로 노출하지 않는다.
     * 호출자가 필요한 것은 {@link OnFieldBudget} VO뿐이다.
     */
    public OnFieldBudget resolveOnFieldBudget(Long userId) {
        UserScheduleConfig config = configRepository.findByUserId(userId)
                                                    .orElseGet(() -> initDefault(userId));
        return config.resolveOnFieldBudget();
    }

    /**
     * Cross-BC inbound — Card BC {@link SoftScheduleTemplate}을 사용자별 설정에서 파생해 반환한다.
     *
     * <p>Review BC가 오늘 학습 후보 수집 시 사용자별 간격 단계(1·3·7일 / 1·3·7·14일 / 1·3·7·14·21일)
     * 를 가져오기 위해 호출한다. 설정 미보유 유저는 첫 호출 시 기본 모드(MODE_10D)로 자동 초기화.
     */
    public SoftScheduleTemplate resolveSoftScheduleTemplate(Long userId) {
        UserScheduleConfig config = configRepository.findByUserId(userId)
                                                    .orElseGet(() -> initDefault(userId));
        return config.resolveSoftScheduleTemplate();
    }

    @Transactional(readOnly = true)
    public List<UserScheduleResponse.HistoryItem> getHistory(Long userId, Integer limit) {
        int resolvedLimit = resolveLimit(limit);

        UserScheduleConfig config = configRepository.findByUserId(userId)
                                                    .orElseThrow(() -> UserScheduleDomainException.of(
                                                            ErrorCode.SCHEDULE_NOT_FOUND));

        return historyRepository
                .findByUserScheduleConfig_IdOrderByChangedAtDesc(config.getId(), resolvedLimit)
                .stream()
                .map(UserScheduleResponse.HistoryItem::of)
                .toList();
    }

    // ─── 내부 유틸 ───────────────────────────────────────────────

    private UserScheduleConfig initDefault(Long userId) {
        UserScheduleConfig config = UserScheduleConfig.createDefault(userId, mappingPolicy);
        configRepository.save(config);
        return config;
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_HISTORY_LIMIT;
        }
        if (limit > MAX_HISTORY_LIMIT) {
            throw UserScheduleDomainException.of(
                    ErrorCode.SCHEDULE_HISTORY_LIMIT_EXCEEDED,
                    "한 번에 최대 " + MAX_HISTORY_LIMIT + "건까지 조회할 수 있습니다.");
        }
        return limit;
    }
}