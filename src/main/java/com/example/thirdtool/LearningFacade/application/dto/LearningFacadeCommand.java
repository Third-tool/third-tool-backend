package com.example.thirdtool.LearningFacade.application.dto;

import com.example.thirdtool.User.domain.model.UserEntity;

import java.util.List;

/**
 * LearningFacade Command 입력 객체 묶음.
 *
 * <p>presentation 레이어의 Request 객체가 application 레이어로 새지 않도록 Controller에서
 * 본 record로 명시적 변환해 Service에 전달한다. 모든 Command는 path/auth 값을 필드로 포함하며,
 * 각 Service public 메서드는 본 record 1개만 인자로 받는다.
 *
 * <p>예외: {@link CreateFacade}는 {@code Long userId} 대신 {@code UserEntity user}를 들고
 * 다닌다 — {@code LearningFacade.create(UserEntity, String)}가 FK 엔티티를 직접 요구하기
 * 때문이며, UserRepository를 LearningFacade BC로 추가 주입하는 결정은 본 refactor scope 밖.
 */
public final class LearningFacadeCommand {

    private LearningFacadeCommand() {}

    public record CreateFacade(
            UserEntity user,
            List<String> concepts
    ) {}

    public record UpdateConcepts(
            Long userId,
            List<String> concepts
    ) {}

    public record AddAxis(
            Long userId,
            String name
    ) {}

    public record UpdateAxisName(
            Long userId,
            Long axisId,
            String name
    ) {}

    public record RemoveAxis(
            Long userId,
            Long axisId
    ) {}

    // CreateAxisDeck 폐기: Fix — Axis↔Deck 완전 통합 (BE-Story 2, 2026-07-01).
    // 수동 축 스코프 Deck 생성 경로가 사라지면서 관련 Command record도 제거됨.

    public record ReorderAxes(
            Long userId,
            List<Long> orderedAxisIds
    ) {}

    // ─── Layer (Story-LT-E2-S4·S5) ────────────────────────

    public record AddLayer(
            Long userId,
            String name
    ) {}

    public record RenameLayer(
            Long userId,
            Long layerId,
            String name
    ) {}

    public record RemoveLayer(
            Long userId,
            Long layerId
    ) {}

    public record ReorderLayers(
            Long userId,
            List<Long> orderedLayerIds
    ) {}

    public record AddTopic(
            Long userId,
            Long axisId,
            String name,
            String description
    ) {}

    /**
     * 주제 부분 수정 Command.
     *
     * <p>{@code namePresent}/{@code descriptionPresent}는 JSON 누락 vs 명시적 null을 구분하기
     * 위한 플래그다. Controller가 Request DTO(presentation 레이어)의 setter 추적 플래그를
     * 본 record로 옮긴다.
     */
    public record UpdateTopic(
            Long userId,
            Long axisId,
            Long topicId,
            String name,
            String description,
            Long revisionReasonOptionId,
            boolean namePresent,
            boolean descriptionPresent
    ) {}

    public record RemoveTopic(
            Long userId,
            Long axisId,
            Long topicId
    ) {}

    public record ReorderTopics(
            Long userId,
            Long axisId,
            List<Long> orderedTopicIds
    ) {}
}
