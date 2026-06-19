package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.infrastructure.dto.TagSummaryRow;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Card.infrastructure.persistence.TagRepository;
import com.example.thirdtool.Card.presentation.dto.TagResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TagQueryService / TagCommandService — Story 5-1 Mockist 매트릭스.
 */
@DisplayName("Tag 관리 서비스 — Story 5-1")
class TagManagementServiceTest {

    private TagRepository tagRepository;
    private CardRepository cardRepository;
    private TagQueryService queryService;
    private TagCommandService commandService;

    @BeforeEach
    void setUp() {
        tagRepository  = mock(TagRepository.class);
        cardRepository = mock(CardRepository.class);
        queryService   = new TagQueryService(tagRepository);
        commandService = new TagCommandService(cardRepository);
    }

    @Nested
    @DisplayName("TagQueryService.listMyTags")
    class Query {

        @Test
        @DisplayName("Repository 결과를 TagResponse.Item으로 매핑")
        void listMyTags_mapsToItems() {
            when(tagRepository.findTagSummariesByUserId(eq(1L))).thenReturn(List.of(
                    new TagSummaryRow(10L, "alpha", 3),
                    new TagSummaryRow(20L, "beta",  1)
            ));

            List<TagResponse.Item> result = queryService.listMyTags(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).tagId()).isEqualTo(10L);
            assertThat(result.get(0).value()).isEqualTo("alpha");
            assertThat(result.get(0).connectedCardCount()).isEqualTo(3);
            assertThat(result.get(1).tagId()).isEqualTo(20L);
        }

        @Test
        @DisplayName("빈 결과면 빈 리스트 (예외 없음)")
        void listMyTags_emptyResult() {
            when(tagRepository.findTagSummariesByUserId(eq(1L))).thenReturn(List.of());

            assertThat(queryService.listMyTags(1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("TagCommandService.detachTagFromMyCards")
    class Command {

        @Test
        @DisplayName("Repository.detachTagFromUserCards 호출 + 영향 row 수 반환")
        void detach_delegatesAndReturnsCount() {
            when(cardRepository.detachTagFromUserCards(eq(1L), eq(77L))).thenReturn(4);

            int affected = commandService.detachTagFromMyCards(77L, 1L);

            assertThat(affected).isEqualTo(4);
            verify(cardRepository, times(1)).detachTagFromUserCards(1L, 77L);
        }

        @Test
        @DisplayName("영향 row 0건이어도 정상 (예외 없음)")
        void detach_zeroRows_noException() {
            when(cardRepository.detachTagFromUserCards(eq(1L), eq(77L))).thenReturn(0);

            int affected = commandService.detachTagFromMyCards(77L, 1L);

            assertThat(affected).isZero();
        }
    }
}
