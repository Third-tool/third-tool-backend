package com.example.thirdtool.Deck.application.service;


import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.Deck.presentation.dto.DeckRecentResponseDto;
import com.example.thirdtool.Deck.presentation.dto.DeckResponse;
import com.example.thirdtool.Deck.presentation.dto.DeckResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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

    /**
     * 덱 단건 조회
     * 삭제된 덱은 조회 불가
     */
    public DeckResponse.Detail findById(Long deckId) {
        Deck deck = getActiveDeck(deckId);
        return DeckResponse.Detail.of(deck);
    }

    /**
     * 내 루트 덱 목록 조회 (depth = 0)
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
     * 하위 덱 목록 조회
     */
    public DeckResponse.SubDeckList findSubDecks(Long deckId) {
        Deck parent = getActiveDeck(deckId);
        List<DeckResponse.Summary> subDecks = parent.getSubDecks()
                                                    .stream()
                                                    .map(DeckResponse.Summary::of)
                                                    .toList();

        return new DeckResponse.SubDeckList(deckId, subDecks);
    }

    // ─── 내부 공용 메서드 (다른 Service에서도 사용) ────────

    /**
     * 활성 상태 덱 조회 — 삭제 덱 접근 시 예외, 없을 때의 접근
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