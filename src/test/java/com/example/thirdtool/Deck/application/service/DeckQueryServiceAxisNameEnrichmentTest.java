package com.example.thirdtool.Deck.application.service;

import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.Deck.presentation.dto.DeckResponse;
import com.example.thirdtool.LearningFacade.application.service.LearningFacadeQueryService;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * DeckQueryService 단위 테스트 — Fix-Story 1: axisName enrichment.
 *
 * Mockist 전략 — Repository와 LearningFacadeQueryService(cross-BC) Mock,
 * 도메인(Deck) + 응답 DTO 매핑은 실제 객체로 검증.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeckQueryService — Fix-Story 1 axisName enrichment")
class DeckQueryServiceAxisNameEnrichmentTest {

    @Mock
    DeckRepository deckRepository;

    @Mock
    LearningFacadeQueryService learningFacadeQueryService;

    @InjectMocks
    DeckQueryService sut;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserEntity.ofLocal("tester-1", "encoded-pw", "닉네임", "tester1@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    private Deck axisDeck(Long deckId, Long axisId, String name) {
        Deck deck = Deck.createFromAxis(user, axisId, name);
        ReflectionTestUtils.setField(deck, "id", deckId);
        return deck;
    }

    private Deck orphanDeck(Long deckId, String name) {
        Deck deck = Deck.of(name, null, user);
        ReflectionTestUtils.setField(deck, "id", deckId);
        return deck;
    }

    // ─── findById ────────────────────────────────────────────

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("axis 결합 덱 → axisId + axisName 모두 응답에 포함")
        void axis_결합덱_axisName포함() {
            Deck deck = axisDeck(100L, 10L, "백엔드 덱");
            given(deckRepository.findById(100L)).willReturn(Optional.of(deck));
            given(learningFacadeQueryService.findAxisNamesByIds(List.of(10L)))
                    .willReturn(Map.of(10L, "백엔드"));

            DeckResponse.Detail result = sut.findById(100L);

            assertThat(result.deckId()).isEqualTo(100L);
            assertThat(result.axisId()).isEqualTo(10L);
            assertThat(result.axisName()).isEqualTo("백엔드");
        }

        @Test
        @DisplayName("고아 덱 → axisId / axisName 모두 null (LearningFacade 호출 회피)")
        void 고아덱_axisName_null() {
            Deck deck = orphanDeck(100L, "고아 덱");
            given(deckRepository.findById(100L)).willReturn(Optional.of(deck));

            DeckResponse.Detail result = sut.findById(100L);

            assertThat(result.axisId()).isNull();
            assertThat(result.axisName()).isNull();
            verify(learningFacadeQueryService, never()).findAxisNamesByIds(anyCollection());
        }
    }

    // ─── findRootDecks ───────────────────────────────────────

    @Nested
    @DisplayName("findRootDecks")
    class FindRootDecks {

        @Test
        @DisplayName("axis 결합 덱과 고아 덱 혼재 — 결합 덱만 lookup 대상 (N+1 회피)")
        void 혼재시_고아덱_lookup제외() {
            Deck axisD = axisDeck(101L, 10L, "축 결합 덱");
            Deck orphan = orphanDeck(102L, "고아 덱");
            Pageable pageable = PageRequest.of(0, 20);
            given(deckRepository.findRootDecksByUserId(1L, pageable))
                    .willReturn(new PageImpl<>(List.of(axisD, orphan), pageable, 2));
            given(learningFacadeQueryService.findAxisNamesByIds(Set.of(10L)))
                    .willReturn(Map.of(10L, "백엔드"));

            DeckResponse.Page result = sut.findRootDecks(1L, pageable);

            assertThat(result.content()).hasSize(2);
            assertThat(result.content().get(0).axisId()).isEqualTo(10L);
            assertThat(result.content().get(0).axisName()).isEqualTo("백엔드");
            assertThat(result.content().get(1).axisId()).isNull();
            assertThat(result.content().get(1).axisName()).isNull();
        }

        @Test
        @DisplayName("동일 axis 다중 덱 — lookup 1회 (Set으로 중복 제거)")
        void 동일axis_다중덱_lookup1회() {
            Deck d1 = axisDeck(101L, 10L, "덱1");
            Deck d2 = axisDeck(102L, 10L, "덱2");
            Pageable pageable = PageRequest.of(0, 20);
            given(deckRepository.findRootDecksByUserId(1L, pageable))
                    .willReturn(new PageImpl<>(List.of(d1, d2), pageable, 2));
            given(learningFacadeQueryService.findAxisNamesByIds(Set.of(10L)))
                    .willReturn(Map.of(10L, "백엔드"));

            DeckResponse.Page result = sut.findRootDecks(1L, pageable);

            assertThat(result.content()).extracting(DeckResponse.Summary::axisName)
                    .containsExactly("백엔드", "백엔드");
            verify(learningFacadeQueryService).findAxisNamesByIds(Set.of(10L));
        }

        @Test
        @DisplayName("빈 페이지 — LearningFacade 호출 회피 (빈 Set 인자)")
        void 빈페이지() {
            Pageable pageable = PageRequest.of(0, 20);
            given(deckRepository.findRootDecksByUserId(1L, pageable))
                    .willReturn(new PageImpl<>(List.of(), pageable, 0));
            given(learningFacadeQueryService.findAxisNamesByIds(Set.of()))
                    .willReturn(Map.of());

            DeckResponse.Page result = sut.findRootDecks(1L, pageable);

            assertThat(result.content()).isEmpty();
        }
    }
}
