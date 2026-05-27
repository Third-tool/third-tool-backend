package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.application.service.DeckQueryService;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.LearningMaterial;
import com.example.thirdtool.LearningFacade.domain.model.MaterialType;
import com.example.thirdtool.LearningFacade.domain.model.TopicMaterial;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse.AxisItem;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse.DeckItem;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse.FacadeDetail;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse.TopicItem;
import com.example.thirdtool.LearningFacade.presentation.dto.MaterialBreakdown;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("LearningFacadeQueryService.getFacade — 자료 비중 응답 (Story-003-4 / Story 4-3)")
class LearningFacadeQueryServiceTest {

    private LearningFacadeRepository facadeRepository;
    private TopicMaterialRepository topicMaterialRepository;
    private DeckQueryService deckQueryService;
    private LearningFacadeQueryService service;

    private UserEntity user;
    private LearningFacade facade;
    private LearningAxis axis;
    private AxisTopic topicWithMix;       // 4종 혼합
    private AxisTopic topicWithStaticOnly; // BOOK 1건
    private AxisTopic topicEmpty;          // 자료 0건
    private LearningMaterial book;
    private LearningMaterial course;
    private LearningMaterial ai;
    private LearningMaterial web;
    private LearningMaterial book2;

    @BeforeEach
    void setUp() {
        facadeRepository = mock(LearningFacadeRepository.class);
        topicMaterialRepository = mock(TopicMaterialRepository.class);
        deckQueryService = mock(DeckQueryService.class);
        when(deckQueryService.findByAxisIds(anyCollection())).thenReturn(List.of());
        service = new LearningFacadeQueryService(facadeRepository, topicMaterialRepository, deckQueryService);

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        facade = LearningFacade.create(user, "백엔드 개발자");
        axis = facade.addAxis("API 설계");
        topicWithMix = axis.addTopic("주제-혼합", null);
        topicWithStaticOnly = axis.addTopic("주제-정적만", null);
        topicEmpty = axis.addTopic("주제-자료없음", null);
        ReflectionTestUtils.setField(facade, "id", 1L);
        ReflectionTestUtils.setField(axis, "id", 10L);
        ReflectionTestUtils.setField(topicWithMix, "id", 100L);
        ReflectionTestUtils.setField(topicWithStaticOnly, "id", 101L);
        ReflectionTestUtils.setField(topicEmpty, "id", 102L);

        // 자료 4종 + 1개 추가 (BOOK 2건으로 byType 카운트 검증)
        book = LearningMaterial.create(facade, "책1", MaterialType.BOOK, null);
        book2 = LearningMaterial.create(facade, "책2", MaterialType.BOOK, null);
        course = LearningMaterial.create(facade, "강의1", MaterialType.COURSE, null);
        ai = LearningMaterial.create(facade, "AI 대화1", MaterialType.AI_CONVERSATION, null);
        web = LearningMaterial.create(facade, "웹1", MaterialType.WEB_RESOURCE, null);

        when(facadeRepository.findByUserId(user.getId())).thenReturn(Optional.of(facade));
    }

    private TopicMaterial mapping(AxisTopic topic, LearningMaterial material) {
        return TopicMaterial.create(topic, material);
    }

    @Test
    @DisplayName("getFacade_각_주제별_materialBreakdown이_정확히_매핑된다")
    void getFacade_breakdown_per_topic_correct() {
        // topicWithMix: BOOK 2, COURSE 1, AI 1, WEB 1
        // topicWithStaticOnly: BOOK 1
        // topicEmpty: 없음
        when(topicMaterialRepository.findByTopicIdIn(List.of(100L, 101L, 102L)))
                .thenReturn(List.of(
                        mapping(topicWithMix, book),
                        mapping(topicWithMix, book2),
                        mapping(topicWithMix, course),
                        mapping(topicWithMix, ai),
                        mapping(topicWithMix, web),
                        mapping(topicWithStaticOnly, book)
                ));

        FacadeDetail detail = service.getFacade(new com.example.thirdtool.LearningFacade.application.dto.LearningFacadeQuery.GetFacade(user.getId()));

        List<TopicItem> topics = detail.axes().get(0).topics();
        TopicItem mixItem = topics.stream().filter(t -> t.topicId() == 100L).findFirst().orElseThrow();
        TopicItem staticItem = topics.stream().filter(t -> t.topicId() == 101L).findFirst().orElseThrow();
        TopicItem emptyItem = topics.stream().filter(t -> t.topicId() == 102L).findFirst().orElseThrow();

        // 혼합 주제
        MaterialBreakdown mix = mixItem.materialBreakdown();
        assertThat(mix.byType().get(MaterialType.BOOK)).isEqualTo(2L);
        assertThat(mix.byType().get(MaterialType.COURSE)).isEqualTo(1L);
        assertThat(mix.byType().get(MaterialType.AI_CONVERSATION)).isEqualTo(1L);
        assertThat(mix.byType().get(MaterialType.WEB_RESOURCE)).isEqualTo(1L);
        assertThat(mix.staticCount()).isEqualTo(3L);
        assertThat(mix.dynamicCount()).isEqualTo(2L);
        assertThat(mix.totalCount()).isEqualTo(5L);

        // 정적만 주제
        MaterialBreakdown staticOnly = staticItem.materialBreakdown();
        assertThat(staticOnly.staticCount()).isEqualTo(1L);
        assertThat(staticOnly.dynamicCount()).isZero();
        assertThat(staticOnly.totalCount()).isEqualTo(1L);

        // 자료 없는 주제 — 모든 타입 0
        MaterialBreakdown empty = emptyItem.materialBreakdown();
        assertThat(empty.totalCount()).isZero();
        assertThat(empty.byType()).hasSize(4);
    }

    @Test
    @DisplayName("getFacade_자료_없는_주제도_materialBreakdown_빈집계_채워진다")
    void getFacade_topic_without_materials_still_has_empty_breakdown() {
        when(topicMaterialRepository.findByTopicIdIn(List.of(100L, 101L, 102L)))
                .thenReturn(List.of());

        FacadeDetail detail = service.getFacade(new com.example.thirdtool.LearningFacade.application.dto.LearningFacadeQuery.GetFacade(user.getId()));

        for (TopicItem item : detail.axes().get(0).topics()) {
            assertThat(item.materialBreakdown()).isNotNull();
            assertThat(item.materialBreakdown().totalCount()).isZero();
            assertThat(item.materialBreakdown().byType()).hasSize(4); // 4종 키 누락 없음
        }
    }

    @Test
    @DisplayName("getFacade_주제가_없으면_findByTopicIdIn_호출_없이_빈_응답")
    void getFacade_no_topics_no_query() {
        // 새 facade로 axes 비우기
        LearningFacade emptyFacade = LearningFacade.create(user, "빈 컨셉");
        ReflectionTestUtils.setField(emptyFacade, "id", 2L);
        when(facadeRepository.findByUserId(user.getId())).thenReturn(Optional.of(emptyFacade));

        FacadeDetail detail = service.getFacade(new com.example.thirdtool.LearningFacade.application.dto.LearningFacadeQuery.GetFacade(user.getId()));

        assertThat(detail.axes()).isEmpty();
        verify(topicMaterialRepository, never()).findByTopicIdIn(anyList());
    }

    @Test
    @DisplayName("getFacade_Facade_미보유_LEARNING_FACADE_NOT_FOUND_예외")
    void getFacade_no_facade_not_found_exception() {
        when(facadeRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFacade(new com.example.thirdtool.LearningFacade.application.dto.LearningFacadeQuery.GetFacade(999L)))
                .isInstanceOf(LearningFacadeDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LEARNING_FACADE_NOT_FOUND);
        verify(topicMaterialRepository, never()).findByTopicIdIn(anyList());
    }

    // ─── Story-005-2: 축별 linkedDecks 매핑 ──────────────────

    @Test
    @DisplayName("getFacade_연결된_Deck_없음 — 모든 축 linkedDecks 빈 리스트")
    void getFacade_no_linked_decks_axes_have_empty_list() {
        when(topicMaterialRepository.findByTopicIdIn(List.of(100L, 101L, 102L)))
                .thenReturn(List.of());
        when(deckQueryService.findByAxisIds(List.of(10L))).thenReturn(List.of());

        FacadeDetail detail = service.getFacade(new com.example.thirdtool.LearningFacade.application.dto.LearningFacadeQuery.GetFacade(user.getId()));

        AxisItem axisItem = detail.axes().get(0);
        assertThat(axisItem.linkedDecks()).isEmpty();
    }

    @Test
    @DisplayName("getFacade_연결된_Deck_여러건 — axisId 기준 그룹핑되어 매핑된다")
    void getFacade_linked_decks_grouped_by_axisId() {
        when(topicMaterialRepository.findByTopicIdIn(List.of(100L, 101L, 102L)))
                .thenReturn(List.of());

        Deck deck1 = Deck.createFromLearningMaterial(user, 10L, 200L, "DDD");
        Deck deck2 = Deck.createFromLearningMaterial(user, 10L, 201L, "Clean Architecture");
        ReflectionTestUtils.setField(deck1, "id", 500L);
        ReflectionTestUtils.setField(deck2, "id", 501L);
        when(deckQueryService.findByAxisIds(List.of(10L))).thenReturn(List.of(deck1, deck2));

        FacadeDetail detail = service.getFacade(new com.example.thirdtool.LearningFacade.application.dto.LearningFacadeQuery.GetFacade(user.getId()));

        AxisItem axisItem = detail.axes().get(0);
        assertThat(axisItem.linkedDecks()).hasSize(2);
        assertThat(axisItem.linkedDecks().stream().map(DeckItem::deckId)).containsExactlyInAnyOrder(500L, 501L);
        assertThat(axisItem.linkedDecks().stream().map(DeckItem::deckName))
                .containsExactlyInAnyOrder("DDD", "Clean Architecture");
        assertThat(axisItem.linkedDecks()).allSatisfy(d ->
                assertThat(d.progressStatus()).isEqualTo("NOT_STARTED"));
        assertThat(axisItem.linkedDecks()).allSatisfy(d ->
                assertThat(d.isMaterialUnlinked()).isFalse());
    }

    @Test
    @DisplayName("getFacade_자료_미연결_Deck — isMaterialUnlinked=true 노출")
    void getFacade_material_unlinked_deck_flag_true() {
        when(topicMaterialRepository.findByTopicIdIn(List.of(100L, 101L, 102L)))
                .thenReturn(List.of());

        Deck orphan = Deck.createFromLearningMaterial(user, 10L, 999L, "고아 Deck");
        orphan.markMaterialDeleted();
        ReflectionTestUtils.setField(orphan, "id", 600L);
        when(deckQueryService.findByAxisIds(List.of(10L))).thenReturn(List.of(orphan));

        FacadeDetail detail = service.getFacade(new com.example.thirdtool.LearningFacade.application.dto.LearningFacadeQuery.GetFacade(user.getId()));

        AxisItem axisItem = detail.axes().get(0);
        assertThat(axisItem.linkedDecks()).hasSize(1);
        assertThat(axisItem.linkedDecks().get(0).isMaterialUnlinked()).isTrue();
    }
}
