package com.example.thirdtool.LearningFacade.presentation.dto;

import com.example.thirdtool.LearningFacade.domain.model.ProficiencyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class LearningMaterialRequest {

    /**
     * 자료 등록 요청.
     * materialType은 String으로 받아 Application Service에서 enum 변환 — 알 수 없는 값에 대해
     * {@code LEARNING_MATERIAL_TYPE_INVALID} 도메인 예외로 응답한다 (api §16 / §22).
     * 부가 속성 5종(author/platform/aiProvider/webSource/memo)은 모두 optional.
     */
    public record CreateMaterial(
            @NotBlank
            String name,

            @NotBlank
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
            // null·미입력이면 false로 처리 (기본값) — 동명 발견 시 409.
            Boolean forceCreateDeck
    ) {}

    public record UpdateMaterialName(
            @NotBlank
            String name
    ) {}

    public record UpdateProficiency(
            @NotNull
            ProficiencyLevel proficiencyLevel
    ) {}

    public record LinkTopic(
            @NotNull
            Long topicId
    ) {}
}
