package com.example.thirdtool.Card.presentation.dto;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.KeywordCue;
import com.example.thirdtool.Card.domain.model.MainContentType;

import java.time.LocalDateTime;
import java.util.List;

public class CardResponse {

    // ─── 카드 생성 응답 ───────────────────────────────────
    public record Create(
            Long cardId,
            Long deckId,
            MainNoteDto mainNote,
            List<KeywordDto> keywords,
            String summary,
            LocalDateTime createdDate
    ) {
        public static Create of(Card card) {
            return new Create(
                    card.getId(),
                    card.getDeck().getId(),
                    MainNoteDto.of(card),
                    KeywordDto.listOf(card),
                    card.getSummary().getValue(),
                    card.getCreatedDate()
            );
        }
    }

    // ─── 카드 단건 조회 응답- 내부 디테일 ──────────────────────────────
    public record Detail(
            Long cardId,
            Long deckId,
            MainNoteDto mainNote,
            List<KeywordDto> keywords,
            String summary,
            LocalDateTime createdDate,
            LocalDateTime updatedDate
    ) {
        public static Detail of(Card card) {
            return new Detail(
                    card.getId(),
                    card.getDeck().getId(),
                    MainNoteDto.of(card),
                    KeywordDto.listOf(card),
                    card.getSummary().getValue(),
                    card.getCreatedDate(),
                    card.getUpdatedDate()
            );
        }
    }

    // ─── 덱 내 카드 목록 조회 아이템 ─────────────────────
    // mainNote 본문 제외, 요약 구조로 반환
    public record Summary(
            Long cardId,
            List<KeywordDto> keywords,
            String summary,
            MainContentType contentType,
            LocalDateTime createdDate
    ) {
        public static Summary of(Card card) {
            return new Summary(
                    card.getId(),
                    KeywordDto.listOf(card),
                    card.getSummary().getValue(),
                    card.getMainNote().getContentType(),
                    card.getCreatedDate()
            );
        }
    }

    // ─── MainNote 수정 응답 ───────────────────────────────
    public record UpdateMainNote(
            Long cardId,
            MainNoteDto mainNote
    ) {
        public static UpdateMainNote of(Card card) {
            return new UpdateMainNote(card.getId(), MainNoteDto.of(card));
        }
    }

    // ─── Summary 수정 응답 ────────────────────────────────
    public record UpdateSummary(
            Long cardId,
            String summary
    ) {
        public static UpdateSummary of(Card card) {
            return new UpdateSummary(card.getId(), card.getSummary().getValue());
        }
    }

    // ─── Keyword 관련 응답 (전체 교체 / 단건 추가 / 단건 제거 공통) ──
    public record Keywords(
            Long cardId,
            List<KeywordDto> keywords
    ) {
        public static Keywords of(Card card) {
            return new Keywords(card.getId(), KeywordDto.listOf(card));
        }
    }

    // ─── 공통 중첩 DTO ────────────────────────────────────

    public record MainNoteDto(
            String textContent,
            String imageUrl,
            MainContentType contentType
    ) {
        public static MainNoteDto of(Card card) {
            return new MainNoteDto(
                    card.getMainNote().getTextContent(),
                    card.getMainNote().getImageUrl(),
                    card.getMainNote().getContentType()
            );
        }
    }

    public record KeywordDto(
            Long id,
            String value
    ) {
        public static KeywordDto of(KeywordCue kc) {
            return new KeywordDto(kc.getId(), kc.getValue());
        }

        public static List<KeywordDto> listOf(Card card) {
            return card.getKeywordCues()
                       .stream()
                       .map(KeywordDto::of)
                       .toList();
        }
    }
}