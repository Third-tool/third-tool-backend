package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.LearningFacade.application.dto.LearningFacadeCommand;
import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.CoverageStatus;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.LearningMaterial;
import com.example.thirdtool.LearningFacade.domain.model.MaterialType;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningMaterialRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.RevisionReasonOptionRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicDeletionRecordRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicRevisionRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse.AddTopic;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("LearningFacadeCommandService.addTopic — 자료 추가 유도 + 연결 후보 (Story-004-2)")
class LearningFacadeCommandServiceAddTopicTest {

    private LearningFacadeRepository facadeRepository;
    private LearningMaterialRepository learningMaterialRepository;
    private TopicMaterialRepository topicMaterialRepository;
    private LearningFacadeCommandService service;

    private UserEntity user;
    private LearningFacade facade;
    private LearningAxis axis;

    @BeforeEach
    void setUp() {
        facadeRepository = mock(LearningFacadeRepository.class);
        TopicRevisionRepository topicRevisionRepository = mock(TopicRevisionRepository.class);
        RevisionReasonOptionRepository revisionReasonOptionRepository = mock(RevisionReasonOptionRepository.class);
        TopicDeletionRecordRepository topicDeletionRecordRepository = mock(TopicDeletionRecordRepository.class);
        learningMaterialRepository = mock(LearningMaterialRepository.class);
        topicMaterialRepository = mock(TopicMaterialRepository.class);

        service = new LearningFacadeCommandService(
                facadeRepository, topicRevisionRepository, revisionReasonOptionRepository,
                topicDeletionRecordRepository, learningMaterialRepository, topicMaterialRepository);

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        facade = LearningFacade.create(user, "백엔드 개발자");
        ReflectionTestUtils.setField(facade, "id", 50L);
        axis = facade.addAxis("API 설계");
        ReflectionTestUtils.setField(axis, "id", 10L);

        when(facadeRepository.findByUserId(user.getId())).thenReturn(Optional.of(facade));
        when(facadeRepository.save(facade)).thenAnswer(inv -> {
            LearningFacade saved = inv.getArgument(0);
            AxisTopic last = saved.getAxes().get(0).getTopics().get(saved.getAxes().get(0).getTopics().size() - 1);
            if (last.getId() == null) ReflectionTestUtils.setField(last, "id", 100L);
            return saved;
        });
    }

    private static LearningMaterial materialWithId(LearningFacade facade, String name, Long id) {
        LearningMaterial m = LearningMaterial.create(
                facade, name, MaterialType.BOOK, null, null, null, null, null, null);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    @Test
    @DisplayName("신규 주제 응답에 needsMaterialPrompt=true가 포함된다 (NO_MATERIAL 초기 상태)")
    void addTopic_신규주제_needsMaterialPrompt_true() {
        when(learningMaterialRepository.findByFacadeId(50L)).thenReturn(List.of());

        AddTopic response = service.addTopic(
                new LearningFacadeCommand.AddTopic(user.getId(), 10L, "REST 설계", null));

        assertThat(response.needsMaterialPrompt()).isTrue();
        assertThat(response.coverageStatus()).isEqualTo(CoverageStatus.NO_MATERIAL.name());
    }

    @Test
    @DisplayName("동일 Facade의 자료 중 신규 주제에 아직 연결되지 않은 것을 linkableMaterials로 반환")
    void addTopic_미연결자료_linkable에포함() {
        LearningMaterial m1 = materialWithId(facade, "RFC 7231", 201L);
        LearningMaterial m2 = materialWithId(facade, "REST 가이드", 202L);
        when(learningMaterialRepository.findByFacadeId(50L)).thenReturn(List.of(m1, m2));
        when(topicMaterialRepository.existsByTopicIdAndMaterialId(anyLong(), anyLong())).thenReturn(false);

        AddTopic response = service.addTopic(
                new LearningFacadeCommand.AddTopic(user.getId(), 10L, "REST 설계", null));

        assertThat(response.linkableMaterials())
                .extracting("materialId")
                .containsExactly(201L, 202L);
    }

    @Test
    @DisplayName("이미 신규 주제에 연결된 자료는 linkableMaterials에서 제외된다")
    void addTopic_이미연결된자료_제외() {
        LearningMaterial m1 = materialWithId(facade, "RFC 7231", 201L);
        LearningMaterial m2 = materialWithId(facade, "REST 가이드", 202L);
        when(learningMaterialRepository.findByFacadeId(50L)).thenReturn(List.of(m1, m2));
        when(topicMaterialRepository.existsByTopicIdAndMaterialId(eq(100L), eq(201L))).thenReturn(true);
        when(topicMaterialRepository.existsByTopicIdAndMaterialId(eq(100L), eq(202L))).thenReturn(false);

        AddTopic response = service.addTopic(
                new LearningFacadeCommand.AddTopic(user.getId(), 10L, "REST 설계", null));

        assertThat(response.linkableMaterials())
                .extracting("materialId")
                .containsExactly(202L);
    }

    @Test
    @DisplayName("Facade에 자료가 0건이면 linkableMaterials는 빈 리스트")
    void addTopic_자료없음_빈리스트() {
        when(learningMaterialRepository.findByFacadeId(50L)).thenReturn(List.of());

        AddTopic response = service.addTopic(
                new LearningFacadeCommand.AddTopic(user.getId(), 10L, "REST 설계", null));

        assertThat(response.linkableMaterials()).isEmpty();
    }
}
