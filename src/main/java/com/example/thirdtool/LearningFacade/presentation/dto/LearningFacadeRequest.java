package com.example.thirdtool.LearningFacade.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class LearningFacadeRequest {

    // ─── Facade ───────────────────────────────────────────

    public record CreateFacade(
            @NotBlank
            String concept
    ) {}

    public record UpdateConcept(
            @NotBlank
            String concept
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
     * </ul>
     */
    @Getter
    @NoArgsConstructor
    public static class UpdateTopic {
        private String name;
        private String description;
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
    }

    public record ReorderTopics(
            @NotNull
            List<Long> orderedTopicIds
    ) {}
}
