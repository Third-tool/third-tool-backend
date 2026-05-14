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
    private LearningMaterialCommandService service;

    private UserEntity user;
    private LearningFacade facade;
    private LearningAxis axis;
    private AxisTopic topic;

    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        facadeRepository = mock(LearningFacadeRepository.class);
        materialRepository = mock(LearningMaterialRepository.class);
        topicMaterialRepository = mock(TopicMaterialRepository.class);
        coverageRecalculator = mock(CoverageRecalculator.class);
        eventPublisher = mock(org.springframework.context.ApplicationEventPublisher.class);

        service = new LearningMaterialCommandService(
                facadeRepository, materialRepository, topicMaterialRepository, coverageRecalculator,
                eventPublisher);

        // 기본 동작: 이벤트 발행 시 핸들러가 Deck을 만들어 setResult로 결과 반환 (정상 시나리오 모사).
        // C2 검증(createdDeckId == null → DECK_AUTO_CREATE_FAILED)을 통과시키기 위함.
        // 특정 테스트에서 null/예외 시나리오는 본문에서 override.
        doAnswer(inv -> {
            com.example.thirdtool.LearningFacade.domain.event.LearningMaterialCreatedEvent ev = inv.getArgument(0);
            ev.setResult(999L, ev.materialName());
            return null;
        }).when(eventPublisher).publishEvent(any(com.example.thirdtool.LearningFacade.domain.event.LearningMaterialCreatedEvent.class));

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        facade = LearningFacade.create(user, "백엔드 개발자");
        axis = facade.addAxis("API 설계");
        topic = axis.addTopic("REST API 설계 원칙", null);
        ReflectionTestUtils.setField(axis, "id", 10L);
        ReflectionTestUtils.setField(topic, "id", 100L);

        when(facadeRepository.findByUserId(user.getId())).thenReturn(Optional.of(facade));
        // save 후 ID를 흉내내기 위해 호출 시 id 주입
        when(materialRepository.save(any(LearningMaterial.class))).thenAnswer(inv -> {
            LearningMaterial m = inv.getArgument(0);
            if (m.getId() == null) {
                ReflectionTestUtils.setField(m, "id", 200L);
            }
            return m;
        });
        when(materialRepository.findById(200L)).thenAnswer(inv -> {
            // 마지막 save된 material을 다시 돌려준다 — 단순 캡처
            ArgumentCaptor<LearningMaterial> captor = ArgumentCaptor.forClass(LearningMaterial.class);
            verify(materialRepository, atLeastOnce()).save(captor.capture());
            return Optional.of(captor.getValue());
        });
    }

    private LearningMaterialCommand.CreateMaterial cmd(String name, String materialType, String url,
                                                       String author, String platform, String aiProvider,
                                                       String webSource, String memo, List<Long> linkedTopicIds) {
        // Story-005-1: deckName=null + forceCreateDeck=false (기본값) — 본 테스트는 Deck 자동 생성과 무관한
        // 자료 등록 자체 검증에 집중하므로 이벤트 핸들러가 Mock된 publisher로 실행되지 않는다.
        return new LearningMaterialCommand.CreateMaterial(
                user.getId(), name, materialType, url, author, platform, aiProvider, webSource, memo, linkedTopicIds,
                null, false);
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
        // 구 enum 값(TOP_DOWN/BOTTOM_UP)도 더 이상 허용되지 않는다 (api §16 §22)
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

    // ─── Story-005-1: 이벤트 발행·결과 통신 ──────────────────────────────────────

    @Test
    @DisplayName("createMaterial_이벤트_발행 — LearningMaterialCreatedEvent 1건 publish")
    void createMaterial_이벤트_발행() {
        service.createMaterial(cmd(
                "도메인 주도 설계", "BOOK", null,
                "에릭 에반스", null, null, null, null,
                List.of(100L)));

        ArgumentCaptor<com.example.thirdtool.LearningFacade.domain.event.LearningMaterialCreatedEvent> captor =
                ArgumentCaptor.forClass(com.example.thirdtool.LearningFacade.domain.event.LearningMaterialCreatedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        com.example.thirdtool.LearningFacade.domain.event.LearningMaterialCreatedEvent event = captor.getValue();
        assertThat(event.userId()).isEqualTo(1L);
        assertThat(event.materialName()).isEqualTo("도메인 주도 설계");
        assertThat(event.axisId()).isEqualTo(10L);  // linkedTopicIds 첫 주제의 축
        assertThat(event.forceCreateDeck()).isFalse();
    }

    @Test
    @DisplayName("createMaterial_linkedTopicIds_없음 — 이벤트의 axisId=null")
    void createMaterial_axisId_null_이벤트() {
        service.createMaterial(cmd(
                "리소스 미연결", "WEB_RESOURCE", null,
                null, null, null, null, null,
                List.of()));

        ArgumentCaptor<com.example.thirdtool.LearningFacade.domain.event.LearningMaterialCreatedEvent> captor =
                ArgumentCaptor.forClass(com.example.thirdtool.LearningFacade.domain.event.LearningMaterialCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        assertThat(captor.getValue().axisId()).isNull();
    }

    @Test
    @DisplayName("createMaterial_핸들러_결과_응답반영 — event.setResult 후 응답에 deckId/deckName + material 필드 함께 검증 (Reviewer C4)")
    void createMaterial_핸들러_결과_응답반영() {
        // 핸들러 동작을 시뮬레이션: eventPublisher.publishEvent 호출 시 setResult로 결과 주입
        doAnswer(inv -> {
            com.example.thirdtool.LearningFacade.domain.event.LearningMaterialCreatedEvent event =
                    inv.getArgument(0);
            event.setResult(999L, "도메인 주도 설계");
            return null;
        }).when(eventPublisher).publishEvent(any(com.example.thirdtool.LearningFacade.domain.event.LearningMaterialCreatedEvent.class));

        CreateMaterial response = service.createMaterial(cmd(
                "도메인 주도 설계", "BOOK", "https://example.com/ddd",
                "에릭 에반스", null, null, null, "DDD 책",
                List.of(100L)));

        // Deck 결과 필드
        assertThat(response.deckCreated()).isTrue();
        assertThat(response.deckId()).isEqualTo(999L);
        assertThat(response.deckName()).isEqualTo("도메인 주도 설계");

        // material 원본 필드도 응답에 정상 포함 (C4 — Response 완전 검증)
        assertThat(response.name()).isEqualTo("도메인 주도 설계");
        assertThat(response.materialType()).isEqualTo("BOOK");
        assertThat(response.url()).isEqualTo("https://example.com/ddd");
        assertThat(response.author()).isEqualTo("에릭 에반스");
        assertThat(response.memo()).isEqualTo("DDD 책");
        assertThat(response.proficiencyLevel()).isEqualTo("UNRATED");
        // linkedTopics는 JPA 양방향 매핑이 mock 환경에서 자동 동기화 안 되므로 단위 테스트에서 size 검증 제외.
        // 통합 테스트 (별도 Story) 영역.
    }

    @Test
    @DisplayName("createMaterial_핸들러_setResult_누락 — createdDeckId null이면 DECK_AUTO_CREATE_FAILED (Reviewer C2)")
    void createMaterial_setResult_누락_예외() {
        // setResult 호출 안 함 — 0개 핸들러 또는 silent fail 시나리오 모사
        doNothing().when(eventPublisher).publishEvent(
                any(com.example.thirdtool.LearningFacade.domain.event.LearningMaterialCreatedEvent.class));

        assertThatThrownBy(() -> service.createMaterial(cmd(
                "도메인 주도 설계", "BOOK", null,
                null, null, null, null, null,
                List.of(100L))))
                .isInstanceOf(com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException.class)
                .matches(e -> ((com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException) e).getErrorCode()
                        == ErrorCode.DECK_AUTO_CREATE_FAILED);
    }

    @Test
    @DisplayName("createMaterial_핸들러_예외전파 — 동명 시 핸들러가 throw하면 응답까지 전파")
    void createMaterial_핸들러_예외전파() {
        doThrow(new com.example.thirdtool.Common.Exception.BusinessException(ErrorCode.DECK_NAME_DUPLICATE))
                .when(eventPublisher).publishEvent(any(com.example.thirdtool.LearningFacade.domain.event.LearningMaterialCreatedEvent.class));

        assertThatThrownBy(() -> service.createMaterial(cmd(
                "도메인 주도 설계", "BOOK", null,
                null, null, null, null, null,
                List.of(100L))))
                .isInstanceOf(com.example.thirdtool.Common.Exception.BusinessException.class)
                .matches(e -> ((com.example.thirdtool.Common.Exception.BusinessException) e).getErrorCode()
                        == ErrorCode.DECK_NAME_DUPLICATE);
    }
}
