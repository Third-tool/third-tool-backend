package com.example.thirdtool.LearningFacade.application.dto;

import com.example.thirdtool.LearningFacade.domain.model.ProficiencyLevel;

import java.util.List;

/**
 * LearningMaterial Command 입력 객체 묶음.
 *
 * <p>presentation 레이어 Request 누수 방지 + Service 메서드 시그니처를 Command record 1개로 통일.
 * {@code materialType}은 String으로 받아 Service에서 enum 변환 — 알 수 없는 값은
 * {@code MATERIAL_TYPE_INVALID} 도메인 예외로 응답 (api §16 / §22).
 */
public final class LearningMaterialCommand {

    private LearningMaterialCommand() {}

    public record CreateMaterial(
            Long userId,
            String name,
            String materialType,
            String url,
            String author,
            String platform,
            String aiProvider,
            String webSource,
            String memo,
            List<Long> linkedTopicIds,
            // Story-005-1: Deck 자동 생성용. null/공백이면 자료명을 Deck 이름으로 사용.
            String deckName,
            // Story-005-1: 동명 Deck 존재 시 자동 suffix `(2)` 부여로 진행할지 여부.
            // false면 동명 발견 시 DECK_NAME_DUPLICATE(409) → 트랜잭션 전체 롤백.
            boolean forceCreateDeck
    ) {}

    public record UpdateMaterialName(
            Long userId,
            Long materialId,
            String name
    ) {}

    public record UpdateProficiency(
            Long userId,
            Long materialId,
            ProficiencyLevel proficiencyLevel
    ) {}

    public record LinkTopic(
            Long userId,
            Long materialId,
            Long topicId
    ) {}

    public record UnlinkTopic(
            Long userId,
            Long materialId,
            Long topicId
    ) {}

    public record DeleteMaterial(
            Long userId,
            Long materialId
    ) {}
}
