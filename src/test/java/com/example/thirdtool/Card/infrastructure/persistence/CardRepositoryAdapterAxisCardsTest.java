package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.CardStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * fix-deck-axis-visibility (0.0.2v) Story 3 — Adapter 단락 가드.
 *
 * <p>axisIds가 null/빈 입력이면 DB 호출 없이 빈 리스트를 반환한다 (빈 IN 절 SQL 회피).
 */
@DisplayName("CardRepositoryAdapter — findByUserIdAndAxisIdsAndStatus 빈 입력 단락")
class CardRepositoryAdapterAxisCardsTest {

    private final CardJpaRepository jpa = mock(CardJpaRepository.class);
    private final CardRepositoryAdapter adapter = new CardRepositoryAdapter(jpa);

    @Test
    @DisplayName("빈 axisIds — JpaRepository 미호출, 빈 리스트")
    void emptyAxisIds_shortCircuits() {
        assertThat(adapter.findByUserIdAndAxisIdsAndStatus(1L, List.of(), CardStatus.ON_FIELD)).isEmpty();
        verify(jpa, never()).findByUserIdAndAxisIdsAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("null axisIds — JpaRepository 미호출, 빈 리스트")
    void nullAxisIds_shortCircuits() {
        assertThat(adapter.findByUserIdAndAxisIdsAndStatus(1L, null, CardStatus.ON_FIELD)).isEmpty();
        verify(jpa, never()).findByUserIdAndAxisIdsAndStatus(any(), any(), any());
    }
}
