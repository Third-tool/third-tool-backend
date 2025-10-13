package com.example.thirdtool.Deck.application.service;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeckHierarchyService {

    private final DeckRepository deckRepository;

    public void changeParent(Long deckId, Long newParentId) {
        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        Deck newParent = (newParentId == null) ? null :
                deckRepository.findById(newParentId)
                              .orElseThrow(() -> new BusinessException(ErrorCode.DECK_PARENT_NOT_FOUND));

        // ✅ 순환 참조 방지
        if (newParent != null && isCycle(deck, newParent)) {
            throw new BusinessException(ErrorCode.DECK_HIERARCHY_CYCLE);
        }

        deck.changeParent(newParent);
        log.info("[DeckHierarchyService] 덱 부모 변경 완료 - deckId={}, newParentId={}",
                deckId, newParentId);
    }

    private boolean isCycle(Deck deck, Deck potentialParent) {
        Deck current = potentialParent;
        while (current != null) {
            if (current.equals(deck)) {
                return true;
            }
            current = current.getParentDeck();
        }
        return false;
    }
}