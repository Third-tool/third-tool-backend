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
            String concept
    ) {}

    public record UpdateConcept(
            Long userId,
            String concept
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

    public record ReorderAxes(
            Long userId,
            List<Long> orderedAxisIds
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
