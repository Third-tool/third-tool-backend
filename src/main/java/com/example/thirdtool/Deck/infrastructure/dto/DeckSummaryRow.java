package com.example.thirdtool.Deck.infrastructure.dto;

import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DeckSummaryRow {

    private final Long          deckId;
    private final String        name;
    private final DeckMode mode;
    private final int           depth;
    private final LocalDateTime lastAccessed;
    private final int           cardCount;
    private final int           subDeckCount;
    // fix-deck-axis-visibility (0.0.2v) Story 1 잔여 — 검색 결과에도 축 참조를 노출.
    // Deck 엔티티와 동일하게 raw axisId(Long, FK 없음)만 보유한다. 고아 덱은 null.
    // axisName은 BC 경계 유지를 위해 조회 서비스에서 LearningFacadeQueryService 배치로 보강한다
    // (findRootDecks와 동일 패턴) — QueryDSL 단계에서 LearningAxis를 조인하지 않는다.
    private final Long          axisId;

    @QueryProjection
    public DeckSummaryRow(
            Long deckId,
            String name,
            DeckMode mode,
            int depth,
            LocalDateTime lastAccessed,
            int cardCount,
            int subDeckCount,
            Long axisId
                         ) {
        this.deckId       = deckId;
        this.name         = name;
        this.mode         = mode;
        this.depth        = depth;
        this.lastAccessed = lastAccessed;
        this.cardCount    = cardCount;
        this.subDeckCount = subDeckCount;
        this.axisId       = axisId;
    }
}
