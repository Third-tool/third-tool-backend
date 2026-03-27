package com.example.thirdtool.Deck.infrastructure.dto;

import com.example.thirdtool.Deck.domain.model.DeckMode;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeckSearchCondition {

    /** 필수 — 본인 덱만 조회 */
    private final Long userId;

    /** 선택 — 이름 부분 검색 (LIKE %keyword%) */
    private final String keyword;

    /** 선택 — 특정 부모 덱 하위만 조회 */
    private final Long parentDeckId;

    /**
     * 선택 — 최상위 덱만 조회 여부.
     * true이면 parentDeck IS NULL 조건이 추가된다.
     * parentDeckId와 동시에 true로 설정하지 않는다.
     */
    @Builder.Default
    private final boolean rootOnly = false;

    /** 선택 — DeckMode 필터 */
    private final DeckMode mode;
}
