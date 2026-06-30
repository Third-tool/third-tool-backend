package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.application.dto.LearningMaterialCommand;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.LearningMaterial;
import com.example.thirdtool.LearningFacade.domain.model.TopicMaterial;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningMaterialRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningMaterialResponse.CreateMaterial;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("LearningMaterialCommandService.createMaterial — 부가 속성 5종 + MaterialType 4종 (Story-003-4)")
class LearningMaterialCommandServiceCreateTest {

    private LearningFacadeRepository facadeRepository;
    private LearningMaterialRepository materialRepository;
    private TopicMaterialRepository topicMaterialRepository;
    private CoverageRecalculator coverageRecalculator;
    private ApplicationEventPublisher eventPublisher;
    private LearningMaterialCommandService service;

    private UserEntity user;
    private LearningFacade facade;
    private LearningAxis axis;
    private AxisTopic topic;

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
        axis = facade.addAxis("API 설계");
        topic = axis.addTopic("REST API 설계 원칙", null);
        ReflectionTestUtils.setField(axis, "id", 10L);
        ReflectionTestUtils.setField(topic, "id", 100L);

        when(facadeRepository.findByUserId(user.getId())).thenReturn(Optional.of(facade));
        when(materialRepository.save(any(LearningMaterial.class))).thenAnswer(inv -> {
            LearningMaterial m = inv.getArgument(0);
            if (m.getId() == null) {
                ReflectionTestUtils.setField(m, "id", 200L);
            }
            return m;
        });
        when(materialRepository.findById(200L)).thenAnswer(inv -> {
            ArgumentCaptor<LearningMaterial> captor = ArgumentCaptor.forClass(LearningMaterial.class);
            verify(materialRepository, atLeastOnce()).save(captor.capture());
            return Optional.of(captor.getValue());
        });
    }

    private LearningMaterialCommand.CreateMaterial cmd(String name, String materialType, String url,
                                                       String author, String platform, String aiProvider,
                                                       String webSource, String memo, List<Long> linkedTopicIds) {
        return new LearningMaterialCommand.CreateMaterial(
                user.getId(), name, materialType, url, author, platform, aiProvider, webSource, memo,
                linkedTopicIds);
    }

    @Test
    @DisplayName("createMaterial_BOOK_부가속성_5종_정상_등록")
    void createMaterial_BOOK_with_all_optional_attributes() {
        CreateMaterial response = service.createMaterial(cmd(
                "Real MySQL 8.0",
                "BOOK",
                "https://example.com/real-mysql",
                "백은빈, 이성욱",
                "인프런",
                "Claude",
                "Notion",
                "메모",
                List.of()));

        assertThat(response.materialType()).isEqualTo("BOOK");
        assertThat(response.author()).isEqualTo("백은빈, 이성욱");
        assertThat(response.platform()).isEqualTo("인프런");
        assertThat(response.aiProvider()).isEqualTo("Claude");
        assertThat(response.webSource()).isEqualTo("Notion");
        assertThat(response.memo()).isEqualTo("메모");
        assertThat(response.proficiencyLevel()).isEqualTo("UNRATED");
    }

    @Test
    @DisplayName("createMaterial_AI_CONVERSATION_정상_등록")
    void createMaterial_AI_CONVERSATION_valid() {
        CreateMaterial response = service.createMaterial(cmd(
                "JPA N+1 디버깅",
                "AI_CONVERSATION",
                "https://claude.ai/chat/...",
                null, null, "Claude", null,
                "N+1 발생 원인 정리",
                null));

        assertThat(response.materialType()).isEqualTo("AI_CONVERSATION");
        assertThat(response.aiProvider()).isEqualTo("Claude");
        assertThat(response.memo()).isEqualTo("N+1 발생 원인 정리");
    }

    @Test
    @DisplayName("createMaterial_알수없는_materialType_MATERIAL_TYPE_INVALID_예외")
    void createMaterial_unknown_materialType_invalid_exception() {
        assertThatThrownBy(() -> service.createMaterial(cmd(
                "이름", "UNKNOWN_TYPE", null,
                null, null, null, null, null, null)))
                .isInstanceOf(LearningFacadeDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LEARNING_MATERIAL_TYPE_INVALID);
    }

    @Test
    @DisplayName("createMaterial_구_TOP_DOWN_값_MATERIAL_TYPE_INVALID_예외")
    void createMaterial_legacy_TOP_DOWN_invalid_exception() {
        assertThatThrownBy(() -> service.createMaterial(cmd(
                "이름", "TOP_DOWN", null,
                null, null, null, null, null, null)))
                .isInstanceOf(LearningFacadeDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LEARNING_MATERIAL_TYPE_INVALID);
    }

    @Test
    @DisplayName("createMaterial_materialType_blank_MATERIAL_TYPE_REQUIRED_예외")
    void createMaterial_blank_materialType_required_exception() {
        assertThatThrownBy(() -> service.createMaterial(cmd(
                "이름", "   ", null,
                null, null, null, null, null, null)))
                .isInstanceOf(LearningFacadeDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LEARNING_MATERIAL_TYPE_REQUIRED);
    }

    @Test
    @DisplayName("createMaterial_materialType_null_MATERIAL_TYPE_REQUIRED_예외")
    void createMaterial_null_materialType_required_exception() {
        assertThatThrownBy(() -> service.createMaterial(cmd(
                "이름", null, null,
                null, null, null, null, null, null)))
                .isInstanceOf(LearningFacadeDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LEARNING_MATERIAL_TYPE_REQUIRED);
    }

    @Test
    @DisplayName("createMaterial_linkedTopicIds_전달_시_TopicMaterial_저장과_커버리지_재계산_트리거")
    void createMaterial_with_linkedTopicIds_triggers_coverage_recalc() {
        service.createMaterial(cmd(
                "이름", "BOOK", null,
                null, null, null, null, null,
                List.of(100L)));

        verify(topicMaterialRepository, times(1)).save(any(TopicMaterial.class));
        verify(coverageRecalculator, times(1)).recalculate(topic);
    }

    @Test
    @DisplayName("createMaterial_linkedTopicIds_빈_리스트는_TopicMaterial_저장_안함")
    void createMaterial_empty_linkedTopicIds_no_link_no_recalc() {
        service.createMaterial(cmd(
                "이름", "WEB_RESOURCE", null,
                null, null, null, null, null,
                List.of()));

        verify(topicMaterialRepository, never()).save(any(TopicMaterial.class));
        verify(coverageRecalculator, never()).recalculate(any(AxisTopic.class));
    }

    @Test
    @DisplayName("createMaterial_이벤트_미발행 — Deck 생성 이벤트가 더 이상 발행되지 않음 (Fix-Story 3)")
    void createMaterial_이벤트_미발행() {
        service.createMaterial(cmd(
                "도메인 주도 설계", "BOOK", null,
                "에릭 에반스", null, null, null, null,
                List.of(100L)));

        verify(eventPublisher, never()).publishEvent(any());
    }
}
