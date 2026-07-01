package com.example.thirdtool.Deck.application.service;

import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.Deck.presentation.dto.DeckRequest;
import com.example.thirdtool.Deck.presentation.dto.DeckResponse;
import lombok.RequiredArgsConstructor;
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

    // Deck 생성은 LearningAxisCreatedEventHandler를 통한 자동 생성만 가능하다.
    // (Fix — Axis↔Deck 완전 통합, BE-Story 2, 2026-07-01)
    // 이전 create(), createUnderAxis() 메서드는 폐기됨 — 사용자 지시 "Axis가 곧 덱과 의미가
    // 동일하다. Deck 자체는 스스로 만들어지지 않았으면 좋겠다"에 따른 결정.

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
        deck.softDelete();
    }

    /**
     * 축(Axis)에 속한 모든 활성 Deck을 연쇄 소프트 삭제한다.
     * (Fix — Axis↔Deck 완전 통합, 2026-07-01)
     *
     * <p>{@code LearningFacadeCommandService.removeAxis}가 축을 소프트 삭제한 직후 호출한다.
     * 축=덱 정책에 따라 축이 사라지면 그 축의 Deck·Card도 함께 사라져야 하며,
     * 각 Deck의 {@code softDelete()}는 소속 Card를 연쇄 소프트 삭제한다.
     *
     * <p>대상이 0건이면 no-op. 이미 삭제된 Deck은 조회 필터로 제외되므로 재삭제 예외를 만나지 않는다.
     */
    public void softDeleteByAxisId(Long axisId) {
        deckRepository.findByAxisIdInAndDeletedFalse(List.of(axisId))
                      .forEach(Deck::softDelete);
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
}
