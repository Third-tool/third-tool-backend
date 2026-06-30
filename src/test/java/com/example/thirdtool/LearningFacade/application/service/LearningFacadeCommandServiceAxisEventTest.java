package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.LearningFacade.application.dto.LearningFacadeCommand;
import com.example.thirdtool.LearningFacade.domain.event.LearningAxisCreatedEvent;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningMaterialRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.RevisionReasonOptionRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicDeletionRecordRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicRevisionRepository;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("LearningFacadeCommandService.addAxis() — LearningAxisCreatedEvent 발행 (Fix-Story 2)")
class LearningFacadeCommandServiceAxisEventTest {

    private LearningFacadeRepository facadeRepository;
    private ApplicationEventPublisher eventPublisher;
    private LearningFacadeCommandService service;

    private UserEntity user;
    private LearningFacade facade;

    @BeforeEach
    void setUp() {
        facadeRepository = mock(LearningFacadeRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        service = new LearningFacadeCommandService(
                facadeRepository,
                mock(TopicRevisionRepository.class),
                mock(RevisionReasonOptionRepository.class),
                mock(TopicDeletionRecordRepository.class),
                mock(LearningMaterialRepository.class),
                mock(TopicMaterialRepository.class),
                eventPublisher);

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        facade = LearningFacade.create(user, "백엔드 개발자");
        ReflectionTestUtils.setField(facade, "id", 50L);

        when(facadeRepository.findByUserId(1L)).thenReturn(Optional.of(facade));
        when(facadeRepository.save(any(LearningFacade.class))).thenAnswer(inv -> {
            LearningFacade saved = inv.getArgument(0);
            // JPA cascade 모사 — 새로 추가된 마지막 axis에 id 부여
            saved.getAxes().stream()
                    .filter(a -> a.getId() == null)
                    .forEach(a -> ReflectionTestUtils.setField(a, "id", 100L));
            return saved;
        });
    }

    // ─── 해피 ────────────────────────────────────────────────────

    @Test
    @DisplayName("addAxis_정상_이벤트발행 — LearningAxisCreatedEvent에 userId·axisId·axisName 포함")
    void addAxis_정상_이벤트발행() {
        service.addAxis(new LearningFacadeCommand.AddAxis(1L, "Java 심화"));

        ArgumentCaptor<LearningAxisCreatedEvent> captor =
                ArgumentCaptor.forClass(LearningAxisCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        LearningAxisCreatedEvent event = captor.getValue();
        assertThat(event.userId()).isEqualTo(1L);
        assertThat(event.axisId()).isEqualTo(100L);
        assertThat(event.axisName()).isEqualTo("Java 심화");
    }

    // ─── 엣지 ────────────────────────────────────────────────────

    @Test
    @DisplayName("addAxis_이벤트는_save_이후_발행 — save 호출 후 publish 순서 보장")
    void addAxis_이벤트는_save_이후_발행() {
        var order = inOrder(facadeRepository, eventPublisher);

        service.addAxis(new LearningFacadeCommand.AddAxis(1L, "Java 심화"));

        order.verify(facadeRepository).save(any(LearningFacade.class));
        order.verify(eventPublisher).publishEvent(any(LearningAxisCreatedEvent.class));
    }

    // ─── 예외 ────────────────────────────────────────────────────

    @Test
    @DisplayName("addAxis_facade없음_이벤트미발행 — LEARNING_FACADE_NOT_FOUND 예외 시 이벤트 발행 없음")
    void addAxis_facade없음_이벤트미발행() {
        when(facadeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        try {
            service.addAxis(new LearningFacadeCommand.AddAxis(1L, "Java 심화"));
        } catch (Exception ignored) {}

        verify(eventPublisher, never()).publishEvent(any());
    }
}
