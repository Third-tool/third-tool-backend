package com.example.thirdtool.Deck.application.service;


import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.Deck.presentation.dto.DeckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeckQueryService {

    private final DeckRepository deckRepository;

    // ─── 외부 API ─────────────────────────────────────────

    /**
     * 덱 단건 조회.
     * 삭제된 덱 접근 시 DECK_ALREADY_DELETED 예외.
     */
    public DeckResponse.Detail findById(Long deckId) {
        return DeckResponse.Detail.of(getActiveDeck(deckId));
    }

    /**
     * 내 루트 덱 목록 조회 (depth = 0, 논리 삭제 제외).
     * Repository 레벨에서 deleted = false 필터링.
     */
    public DeckResponse.Page findRootDecks(Long userId, Pageable pageable) {
        Page<Deck> page = deckRepository.findRootDecksByUserId(userId, pageable);

        List<DeckResponse.Summary> content = page.getContent()
                                                 .stream()
                                                 .map(DeckResponse.Summary::of)
                                                 .toList();

        return new DeckResponse.Page(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
    }

    /**
     * 하위 덱 목록 조회.
     * 부모 덱의 subDecks 중 논리 삭제된 덱은 제외하여 반환.
     */
    public DeckResponse.SubDeckList findSubDecks(Long deckId) {
        Deck parent = getActiveDeck(deckId);

        List<DeckResponse.Summary> subDecks = parent.getSubDecks()
                                                    .stream()
                                                    .filter(sub -> !sub.isDeleted())
                                                    .map(DeckResponse.Summary::of)
                                                    .toList();

        return new DeckResponse.SubDeckList(deckId, subDecks);
    }

    // ─── 내부 공용 메서드 ─────────────────────────────────

    /**
     * 활성 덱 조회.
     * 존재하지 않거나 논리 삭제된 덱이면 예외.
     * DeckCommandService / DeckHierarchyService에서 공통 사용.
     */
    public Deck getActiveDeck(Long deckId) {
        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        if (deck.isDeleted()) {
            throw new BusinessException(ErrorCode.DECK_ALREADY_DELETED);
        }
        return deck;
    }
}