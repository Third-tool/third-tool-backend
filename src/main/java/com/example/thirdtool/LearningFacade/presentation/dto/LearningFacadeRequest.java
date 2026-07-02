package com.example.thirdtool.LearningFacade.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class LearningFacadeRequest {

    // ─── Facade ───────────────────────────────────────────

    /**
     * LearningFacade 생성 요청.
     * concepts 배열은 필수 · 1~5개 · 각 값은 도메인에서 trim/blank/길이/중복 검증.
     */
    public record CreateFacade(
            @NotNull
            @jakarta.validation.constraints.Size(min = 1, max = 5,
                    message = "컨셉은 1~5개 이내여야 합니다.")
            List<String> concepts
    ) {}

    /**
     * LearningFacade.concepts 통째 교체 요청.
     * 도메인 {@code updateConcepts()}가 부분성공 불허 · size/blank/중복 검증.
     */
    public record UpdateConcepts(
            @NotNull
            @jakarta.validation.constraints.Size(min = 1, max = 5,
                    message = "컨셉은 1~5개 이내여야 합니다.")
            List<String> concepts
    ) {}

    // ─── Axis ─────────────────────────────────────────────

    public record AddAxis(
            @NotBlank
            String name
    ) {}

    public record UpdateAxisName(
            @NotBlank
            String name
    ) {}

    public record ReorderAxes(
            @NotEmpty
            List<Long> orderedAxisIds
    ) {}

    // CreateAxisDeck 폐기: Fix — Axis↔Deck 완전 통합 (BE-Story 2, 2026-07-01).
    // 수동 축 스코프 Deck 생성 경로가 사라지면서 관련 Request record도 제거됨.

    // ─── Topic ────────────────────────────────────────────

    public record AddTopic(
            @NotBlank
            String name,
            String description
    ) {}

    /**
     * 주제 부분 수정.
     *
     * <p>JSON에서 누락된 필드와 명시적 {@code null} 전달을 구분해야 한다.
     * Jackson은 누락 필드의 setter를 호출하지 않으므로 setter 호출 여부로
     * 필드 존재 여부를 추적한다.
     *
     * <ul>
     *   <li>{@code name} 필드 누락 → 기존 이름 유지</li>
     *   <li>{@code description} 필드 누락 → 기존 설명 유지</li>
     *   <li>{@code description: null} 명시 → 부연 설명 제거</li>
     *   <li>{@code revisionReasonOptionId} — 이름 변경 시 선택. 미입력 또는 null이면
     *       이유 없는 이력으로 저장 (이름 변경 자체는 허용)</li>
     * </ul>
     */
    @Getter
    @NoArgsConstructor
    public static class UpdateTopic {
        private String name;
        private String description;
        private Long revisionReasonOptionId;
        private boolean namePresent;
        private boolean descriptionPresent;

        public void setName(String name) {
            this.name = name;
            this.namePresent = true;
        }

        public void setDescription(String description) {
            this.description = description;
            this.descriptionPresent = true;
        }

        public void setRevisionReasonOptionId(Long revisionReasonOptionId) {
            this.revisionReasonOptionId = revisionReasonOptionId;
        }
    }

    public record ReorderTopics(
            @NotNull
            List<Long> orderedTopicIds
    ) {}
}
