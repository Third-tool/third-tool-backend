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
     * 덱 생성 (고아 덱 경로).
     * 본 경로는 항상 axisId=null인 고아 Deck을 만든다. axis 결합 Deck 생성은
     * Fix-Story 2의 신규 경로(POST /api/v1/learning-facade/axes/{axisId}/decks)를 사용한다.
     */
    public DeckResponse.Create create(DeckRequest.Create request, UserEntity user) {
        Deck parentDeck = resolveParent(request.parentDeckId());

        Deck deck = Deck.of(
                request.name(),
                parentDeck,
                user);

        deckRepository.save(deck);
        return DeckResponse.Create.of(deck, null);
    }

    /**
     * Axis 스코프 Deck 생성 (Fix-Story 2).
     * {@link com.example.thirdtool.LearningFacade.application.service.LearningFacadeCommandService}가
     * axis 소유권을 검증한 후 호출한다. 본 메서드는 axis 검증을 다시 수행하지 않는다.
     *
     * <p>동일 (userId, name) 중복은 도메인 메시지로 친절히 차단 — DB UNIQUE 제약 위반에 의존하지 않음.
     *
     * @param user      소유 사용자 (검증된 facade.user)
     * @param axisId    축 ID (검증된 axis.id)
     * @param name      Deck 이름 (사용자 입력, trim 전 원본)
     * @param axisName  검증된 axis.name — 응답 DTO에 동봉
     */
    public DeckResponse.Create createUnderAxis(UserEntity user, Long axisId, String name, String axisName) {
        if (name != null && deckRepository.existsByUserIdAndNameAndDeletedFalse(user.getId(), name.trim())) {
            throw new BusinessException(ErrorCode.DECK_NAME_DUPLICATE);
        }

        Deck deck = Deck.createUnderAxis(user, axisId, name);
        deckRepository.save(deck);
        return DeckResponse.Create.of(deck, axisName);
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
        deck.softDelete();
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
    private Deck resolveParent(Long parentDeckId) {
        if (parentDeckId == null) return null;
        return deckQueryService.getActiveDeck(parentDeckId);
    }

}
