package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.LearningFacade.application.dto.LearningMaterialCommand;
import com.example.thirdtool.LearningFacade.domain.event.LearningMaterialDeletedEvent;
import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.LearningMaterial;
import com.example.thirdtool.LearningFacade.domain.model.MaterialType;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningMaterialRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("LearningMaterialCommandService.deleteMaterial — LearningMaterialDeletedEvent 발행 (Story-005-2)")
class LearningMaterialCommandServiceDeleteTest {

    private LearningFacadeRepository facadeRepository;
    private LearningMaterialRepository materialRepository;
    private TopicMaterialRepository topicMaterialRepository;
    private CoverageRecalculator coverageRecalculator;
    private ApplicationEventPublisher eventPublisher;
    private LearningMaterialCommandService service;

    private UserEntity user;
    private LearningFacade facade;
    private LearningMaterial material;

    @BeforeEach
    void setUp() {
        facadeRepository = mock(LearningFacadeRepository.class);
        materialRepository = mock(LearningMaterialRepository.class);
        topicMaterialRepository = mock(TopicMaterialRepository.class);
        coverageRecalculator = mock(CoverageRecalculator.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        service = new LearningMaterialCommandService(
                facadeRepository, materialRepository, topicMaterialRepository, coverageRecalculator,
                eventPublisher);

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        facade = LearningFacade.create(user, "백엔드 개발자");
        LearningAxis axis = facade.addAxis("API 설계");
        AxisTopic topic = axis.addTopic("REST 원칙", null);
        ReflectionTestUtils.setField(facade, "id", 1L);
        ReflectionTestUtils.setField(axis, "id", 10L);
        ReflectionTestUtils.setField(topic, "id", 100L);

        material = LearningMaterial.create(facade, "DDD 책", MaterialType.BOOK, null);
        ReflectionTestUtils.setField(material, "id", 200L);

        when(facadeRepository.findByUserId(user.getId())).thenReturn(Optional.of(facade));
        when(materialRepository.findById(200L)).thenReturn(Optional.of(material));
    }

    @Test
    @DisplayName("deleteMaterial_정상 — LearningMaterialDeletedEvent(userId, materialId) 발행")
    void deleteMaterial_publishes_event() {
        service.deleteMaterial(new LearningMaterialCommand.DeleteMaterial(user.getId(), 200L));

        ArgumentCaptor<LearningMaterialDeletedEvent> captor =
                ArgumentCaptor.forClass(LearningMaterialDeletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        LearningMaterialDeletedEvent event = captor.getValue();
        assertThat(event.userId()).isEqualTo(1L);
        assertThat(event.materialId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("deleteMaterial — 이벤트 발행이 materialRepository.delete 호출보다 먼저 (FK 안전)")
    void event_publish_before_delete() {
        service.deleteMaterial(new LearningMaterialCommand.DeleteMaterial(user.getId(), 200L));

        InOrder order = inOrder(eventPublisher, materialRepository);
        order.verify(eventPublisher).publishEvent(any(LearningMaterialDeletedEvent.class));
        order.verify(materialRepository).delete(material);
    }
}
