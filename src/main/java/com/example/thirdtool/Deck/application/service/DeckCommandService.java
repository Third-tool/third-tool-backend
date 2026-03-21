package com.example.thirdtool.Deck.application.service;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.Deck.presentation.dto.DeckRequest;
import com.example.thirdtool.Deck.presentation.dto.DeckResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DeckCommandService {

    private final DeckRepository       deckRepository;
    private final DeckQueryService     deckQueryService;
    private final DeckHierarchyService deckHierarchyService;

    /**
     * 덱 생성
     */
    public DeckResponse.Create create(DeckRequest.Create request, UserEntity user) {
        Deck parentDeck = resolveParent(request.parentDeckId());

        Deck deck = Deck.of(
                request.name(),
                parentDeck,
                user);

        deckRepository.save(deck);
        return DeckResponse.Create.of(deck);
    }

    /**
     * 덱 이름 수정
     */
    public DeckResponse.UpdateName updateName(Long deckId, DeckRequest.UpdateName request) {
        Deck deck = deckQueryService.getActiveDeck(deckId);
        deck.updateName(request.name());
        return DeckResponse.UpdateName.of(deck);
    }

    /**
     * 부모 덱 변경
     */
    public DeckResponse.ChangeParent changeParent(Long deckId, DeckRequest.ChangeParent request) {
        Deck deck = deckQueryService.getActiveDeck(deckId);
        deckHierarchyService.changeParent(deck, request.parentDeckId());
        return DeckResponse.ChangeParent.of(deck);
    }

    /**
     * 덱 삭제 (Soft Delete)
     */
    public void delete(Long deckId) {
        Deck deck = deckQueryService.getActiveDeck(deckId);
        deck.delete();
    }

    /**
     * 최근 접근 시각 갱신
     * 덱 학습 화면 진입 시 호출
     */
    public DeckResponse.LastAccessed updateLastAccessed(Long deckId) {
        Deck deck = deckQueryService.getActiveDeck(deckId);
        deck.updateLastAccessed();
        return DeckResponse.LastAccessed.of(deck);
    }

    // ─── private ─────────────────────────────────────────

}
