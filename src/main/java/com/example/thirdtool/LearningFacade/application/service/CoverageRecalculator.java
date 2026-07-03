package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Card.domain.model.CardStatus;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.CoverageStatus;
import com.example.thirdtool.LearningFacade.domain.model.ProficiencyLevel;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoverageRecalculator {

    private final TopicMaterialRepository topicMaterialRepository;
    private final CardRepository cardRepository;

    public CoverageStatus recalculate(AxisTopic topic) {
        CoverageStatus newStatus = calculate(topic.getId());
        topic.updateCoverageStatus(newStatus);
        return newStatus;
    }

    private CoverageStatus calculate(Long topicId) {
        long count = topicMaterialRepository.countByTopicId(topicId);
        if (count == 0) {
            return CoverageStatus.NO_MATERIAL;
        }
        boolean hasMastered = topicMaterialRepository
                .existsByTopicIdAndMaterialProficiencyLevel(topicId, ProficiencyLevel.MASTERED);
        return hasMastered ? CoverageStatus.COVERED : CoverageStatus.PARTIAL;
    }

    /**
     * Story-LT-E4-S4-4 — 축 스코프 Coverage 재계산.
     *
     * <p><b>semantic 주의</b>: 기존 Topic 스코프({@link #recalculate(AxisTopic)})는 자료 기반(topic_material 카운트)
     * = "학습 준비도" 판정. 축 스코프는 카드 상태 기반 = "학습 완료도" 판정. 같은 {@link CoverageStatus} enum을
     * 반환하지만 의미가 다르므로 FE·소비자는 두 스코프를 혼용하지 말 것. M5 이관 시 별도 enum(예: MasteryStatus)
     * 도입 검토 대상.
     *
     * <p>축 스코프 판정 규칙: 축 카드 0개 → NO_MATERIAL, 모두 ARCHIVE → COVERED,
     * 일부 ON_FIELD → PARTIAL. Deck 폐기(M5) 이후 축이 카드 소유의 유일 스코프가 되는 흐름 반영.
     *
     * <p><b>소비처</b>: M4에는 소비처 없음. M5 DailyLearningBatch·Deck 폐기 Story에서 소비.
     * 반환된 CoverageStatus를 LearningAxis 필드로 저장하는 로직은 M5 이관.
     */
    public CoverageStatus recalculateByAxis(Long axisId) {
        if (axisId == null) {
            throw new IllegalArgumentException("CoverageRecalculator: axisId는 null일 수 없습니다.");
        }
        long total = cardRepository.countByAxisIdAndDeletedFalse(axisId);
        if (total == 0) {
            return CoverageStatus.NO_MATERIAL;
        }
        long archived = cardRepository.countByAxisIdAndStatusAndDeletedFalse(axisId, CardStatus.ARCHIVE);
        return archived == total ? CoverageStatus.COVERED : CoverageStatus.PARTIAL;
    }
}
