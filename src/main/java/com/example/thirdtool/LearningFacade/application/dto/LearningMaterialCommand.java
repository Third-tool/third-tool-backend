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
            List<Long> linkedTopicIds
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
