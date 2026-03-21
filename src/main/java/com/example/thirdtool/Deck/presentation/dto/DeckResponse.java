package com.example.thirdtool.Deck.presentation.dto;

import com.example.thirdtool.Deck.domain.model.Deck;

import java.time.LocalDateTime;
import java.util.List;

public class DeckResponse {

    // ─── 덱 생성 응답 ─────────────────────────────────────
    public record Create(
            Long deckId,
            String name,
            Long parentDeckId,
            int depth,
            boolean onLibrary,
            LocalDateTime publishedAt,
            LocalDateTime lastAccessed,
            LocalDateTime createdDate
    ) {
        public static Create of(Deck deck) {
            return new Create(
                    deck.getId(),
                    deck.getName(),
                    deck.getParentDeck() != null ? deck.getParentDeck().getId() : null,
                    deck.getDepth(),
                    deck.isOnLibrary(),
                    deck.getPublishedAt(),
                    deck.getLastAccessed(),
                    deck.getCreatedDate()
            );
        }
    }

    // ─── 덱 단건 조회 응답 ────────────────────────────────
    public record Detail(
            Long deckId,
            String name,
            Long parentDeckId,
            int depth,
            boolean onLibrary,
            LocalDateTime publishedAt,
            LocalDateTime lastAccessed,
            int cardCount,
            int subDeckCount,
            LocalDateTime createdDate,
            LocalDateTime updatedDate
    ) {
        public static Detail of(Deck deck) {
            return new Detail(
                    deck.getId(),
                    deck.getName(),
                    deck.getParentDeck() != null ? deck.getParentDeck().getId() : null,
                    deck.getDepth(),
                    deck.isOnLibrary(),
                    deck.getPublishedAt(),
                    deck.getLastAccessed(),
                    deck.getCards().size(),
                    deck.getSubDecks().size(),
                    deck.getCreatedDate(),
                    deck.getUpdatedDate()
            );
        }
    }

    // ─── 목록 조회 아이템 ─────────────────────────────────
    public record Summary(
            Long deckId,
            String name,
            int depth,
            boolean onLibrary,
            LocalDateTime lastAccessed,
            int cardCount,
            int subDeckCount
    ) {
        public static Summary of(Deck deck) {
            return new Summary(
                    deck.getId(),
                    deck.getName(),
                    deck.getDepth(),
                    deck.isOnLibrary(),
                    deck.getLastAccessed(),
                    deck.getCards().size(),
                    deck.getSubDecks().size()
            );
        }
    }

    // ─── 페이징 목록 응답 ─────────────────────────────────
    public record Page(
            List<Summary> content,
            long totalElements,
            int totalPages,
            int page,
            int size
    ) {}

    // ─── 하위 덱 목록 응답 ────────────────────────────────
    public record SubDeckList(
            Long parentDeckId,
            List<Summary> subDecks
    ) {}

    // ─── 이름 수정 응답 ───────────────────────────────────
    public record UpdateName(
            Long deckId,
            String name,
            LocalDateTime updatedDate
    ) {
        public static UpdateName of(Deck deck) {
            return new UpdateName(deck.getId(), deck.getName(), deck.getUpdatedDate());
        }
    }

    // ─── 부모 변경 응답 ───────────────────────────────────
    public record ChangeParent(
            Long deckId,
            String name,
            Long parentDeckId,
            int depth,
            LocalDateTime updatedDate
    ) {
        public static ChangeParent of(Deck deck) {
            return new ChangeParent(
                    deck.getId(),
                    deck.getName(),
                    deck.getParentDeck() != null ? deck.getParentDeck().getId() : null,
                    deck.getDepth(),
                    deck.getUpdatedDate()
            );
        }
    }

    // ─── 최근 접근 시각 갱신 응답 ─────────────────────────
    public record LastAccessed(
            Long deckId,
            LocalDateTime lastAccessed
    ) {
        public static LastAccessed of(Deck deck) {
            return new LastAccessed(deck.getId(), deck.getLastAccessed());
        }
    }
}

