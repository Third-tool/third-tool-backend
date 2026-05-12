package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.LearningMaterial;
import com.example.thirdtool.LearningFacade.domain.model.MaterialType;
import com.example.thirdtool.LearningFacade.domain.model.ProficiencyLevel;
import com.example.thirdtool.LearningFacade.domain.model.TopicMaterial;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningMaterialRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningMaterialResponse.UpdateProficiency;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("LearningMaterialCommandService.updateProficiency — 숙련도 자가 평가 (Story-003-4 / Story 4-2)")
class LearningMaterialCommandServiceUpdateProficiencyTest {

    private LearningFacadeRepository facadeRepository;
    private LearningMaterialRepository materialRepository;
    private TopicMaterialRepository topicMaterialRepository;
    private CoverageRecalculator coverageRecalculator;
    private LearningMaterialCommandService service;

    private UserEntity user;
    private LearningFacade facade;
    private LearningAxis axis;
    private AxisTopic topic1;
    private AxisTopic topic2;
    private LearningMaterial material;

    @BeforeEach
    void setUp() {
        facadeRepository = mock(LearningFacadeRepository.class);
        materialRepository = mock(LearningMaterialRepository.class);
        topicMaterialRepository = mock(TopicMaterialRepository.class);
        coverageRecalculator = mock(CoverageRecalculator.class);

        service = new LearningMaterialCommandService(
                facadeRepository, materialRepository, topicMaterialRepository, coverageRecalculator);

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        facade = LearningFacade.create(user, "백엔드 개발자");
        axis = facade.addAxis("API 설계");
        topic1 = axis.addTopic("REST API 설계 원칙", null);
        topic2 = axis.addTopic("OpenAPI 명세 작성", null);
        ReflectionTestUtils.setField(axis, "id", 10L);
        ReflectionTestUtils.setField(topic1, "id", 100L);
        ReflectionTestUtils.setField(topic2, "id", 101L);

        material = LearningMaterial.create(facade, "Real MySQL", MaterialType.BOOK, null);
        ReflectionTestUtils.setField(material, "id", 200L);

        when(materialRepository.findById(200L)).thenReturn(Optional.of(material));
    }

    private void linkMaterialTo(AxisTopic topic) {
        TopicMaterial mapping = TopicMaterial.create(topic, material);
        material.getTopicMappings(); // 컬렉션 접근 (Hibernate proxy 회피용 — 실제 양방향 연결은 도메인 컨벤션 따라)
        ReflectionTestUtils.invokeMethod(material, "getTopicMappings");
        // 도메인이 단방향 add API를 제공하지 않으므로 reflection으로 List에 직접 add
        java.util.List<TopicMaterial> mappings =
                (java.util.List<TopicMaterial>) ReflectionTestUtils.getField(material, "topicMappings");
        mappings.add(mapping);
    }

    @Test
    @DisplayName("updateProficiency_UNFAMILIAR_정상_숙련도_변경")
    void updateProficiency_UNRATED_to_UNFAMILIAR() {
        UpdateProficiency response = service.updateProficiency(
                user.getId(), 200L, ProficiencyLevel.UNFAMILIAR);

        assertThat(material.getProficiencyLevel()).isEqualTo(ProficiencyLevel.UNFAMILIAR);
        assertThat(response.proficiencyLevel()).isEqualTo("UNFAMILIAR");
        assertThat(response.isCardCreationSuggested()).isFalse();
    }

    @Test
    @DisplayName("updateProficiency_MASTERED_isCardCreationSuggested_true_안내_트리거")
    void updateProficiency_MASTERED_card_suggestion_true() {
        UpdateProficiency response = service.updateProficiency(
                user.getId(), 200L, ProficiencyLevel.MASTERED);

        assertThat(material.getProficiencyLevel()).isEqualTo(ProficiencyLevel.MASTERED);
        assertThat(response.isCardCreationSuggested()).isTrue();
    }

    @Test
    @DisplayName("updateProficiency_GETTING_USED_isCardCreationSuggested_false")
    void updateProficiency_GETTING_USED_card_suggestion_false() {
        UpdateProficiency response = service.updateProficiency(
                user.getId(), 200L, ProficiencyLevel.GETTING_USED);

        assertThat(response.isCardCreationSuggested()).isFalse();
    }

    @Test
    @DisplayName("updateProficiency_UNRATED_되돌림_시도_예외")
    void updateProficiency_UNRATED_rejected() {
        assertThatThrownBy(() -> service.updateProficiency(
                user.getId(), 200L, ProficiencyLevel.UNRATED))
                .isInstanceOf(LearningFacadeDomainException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode",
                        ErrorCode.LEARNING_MATERIAL_PROFICIENCY_UNRATED_NOT_ALLOWED);
    }

    @Test
    @DisplayName("updateProficiency_연결된_각_주제마다_커버리지_재계산_트리거")
    void updateProficiency_triggers_coverage_recalc_per_linked_topic() {
        linkMaterialTo(topic1);
        linkMaterialTo(topic2);

        service.updateProficiency(user.getId(), 200L, ProficiencyLevel.MASTERED);

        verify(coverageRecalculator, times(1)).recalculate(topic1);
        verify(coverageRecalculator, times(1)).recalculate(topic2);
    }

    @Test
    @DisplayName("updateProficiency_미연결_자료_updatedCoverages_빈리스트")
    void updateProficiency_no_linked_topics_empty_updatedCoverages() {
        UpdateProficiency response = service.updateProficiency(
                user.getId(), 200L, ProficiencyLevel.UNFAMILIAR);

        assertThat(response.updatedCoverages()).isEmpty();
        verify(coverageRecalculator, never()).recalculate(any(AxisTopic.class));
    }

    @Test
    @DisplayName("updateProficiency_존재하지않는_materialId_LEARNING_MATERIAL_NOT_FOUND_예외")
    void updateProficiency_unknown_materialId_not_found_exception() {
        when(materialRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProficiency(
                user.getId(), 999L, ProficiencyLevel.UNFAMILIAR))
                .isInstanceOf(LearningFacadeDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LEARNING_MATERIAL_NOT_FOUND);
    }

    @Test
    @DisplayName("updateProficiency_본인소유아닌_자료_FACADE_NOT_FOUND_예외_소유권_노출_회피")
    void updateProficiency_not_owner_returns_facade_not_found() {
        // 다른 유저가 소유한 facade에 속한 material로 교체
        UserEntity otherUser = UserEntity.ofLocal("other", "pw", "닉", "other@example.com");
        ReflectionTestUtils.setField(otherUser, "id", 2L);
        LearningFacade otherFacade = LearningFacade.create(otherUser, "다른 컨셉");
        LearningMaterial otherMaterial = LearningMaterial.create(otherFacade, "남의 책", MaterialType.BOOK, null);
        ReflectionTestUtils.setField(otherMaterial, "id", 300L);

        when(materialRepository.findById(300L)).thenReturn(Optional.of(otherMaterial));

        assertThatThrownBy(() -> service.updateProficiency(
                user.getId(), 300L, ProficiencyLevel.UNFAMILIAR))
                .isInstanceOf(LearningFacadeDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LEARNING_FACADE_NOT_FOUND);
    }
}
