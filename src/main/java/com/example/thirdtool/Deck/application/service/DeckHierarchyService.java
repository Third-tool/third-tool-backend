package com.example.thirdtool.Deck.application.service;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeckHierarchyService {

    private final DeckQueryService deckQueryService;

    /**
     * 부모 덱 변경.
     * - newParentDeckId == null → 루트 덱으로 이동
     * - 순환 참조 감지 시 DECK_HIERARCHY_CYCLE 예외
     *
     * @param deck            이동할 대상 덱 (이미 getActiveDeck()으로 검증된 상태)
     * @param newParentDeckId 새 부모 덱 ID (null 허용)
     */
    @Transactional
    public void changeParent(Deck deck, Long newParentDeckId) {
        if (newParentDeckId == null) {
            deck.changeParent(null);
            return;
        }

        Deck newParent = deckQueryService.getActiveDeck(newParentDeckId);
        validateNoCycle(deck, newParent);
        deck.changeParent(newParent);
    }

    // ─── private ──────────────────────────────────────────

    /**
     * 순환 참조 검증.
     * newParent의 조상을 루트까지 따라 올라가며
     * deck 자신이 포함되어 있으면 순환으로 판단.
     *
     * <p>예: A → B → C 구조에서 A의 부모를 C로 변경하려 하면
     * C의 조상(B → A)에 A가 포함되므로 순환 감지.
     */
    private void validateNoCycle(Deck deck, Deck newParent) {
        Deck cursor = newParent;
        while (cursor != null) {
            if (cursor.getId().equals(deck.getId())) {
                throw new BusinessException(ErrorCode.DECK_HIERARCHY_CYCLE);
            }
            cursor = cursor.getParentDeck();
        }
    }
}