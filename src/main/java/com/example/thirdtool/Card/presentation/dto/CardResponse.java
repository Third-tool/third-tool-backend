package com.example.thirdtool.Card.presentation.dto;

import com.example.thirdtool.Card.domain.model.*;

import java.time.LocalDateTime;
import java.util.List;

public class CardResponse {

    // ─── 1. 카드 생성 응답 ───────────────────────────────────
    public record Create(
            Long cardId,
            Long deckId,
            MainNoteDto mainNote,
            List<KeywordDto> keywords,
            String summary,
            List<TagDto> tags,
            LocalDateTime createdDate
    ) {
        public static Create of(Card card) {
            return new Create(
                    card.getId(),
                    card.getDeck().getId(),
                    MainNoteDto.of(card),
                    KeywordDto.listOf(card),
                    card.getSummary().getValue(),
                    TagDto.listOf(card),
                    card.getCreatedDate()
            );
        }
    }

    // ─── 2. 카드 단건 조회 응답 ───────────────────────────────
    public record Detail(
            Long cardId,
            Long deckId,
            MainNoteDto mainNote,
            List<KeywordDto> keywords,
            String summary,
            List<TagDto> tags,
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
                    TagDto.listOf(card),
                    card.getCreatedDate(),
                    card.getUpdatedDate()
            );
        }
    }

    // ─── 3. 덱 내 카드 목록 아이템 ───────────────────────────
    // mainNote 본문 제외, 요약 구조로 반환
    public record Summary(
            Long cardId,
            List<KeywordDto> keywords,
            String summary,
            List<TagDto> tags,
            MainContentType contentType,
            LocalDateTime createdDate
    ) {
        public static Summary of(Card card) {
            return new Summary(
                    card.getId(),
                    KeywordDto.listOf(card),
                    card.getSummary().getValue(),
                    TagDto.listOf(card),
                    card.getMainNote().getContentType(),
                    card.getCreatedDate()
            );
        }
    }

    // ─── 4. MainNote 수정 응답 ────────────────────────────────
    public record UpdateMainNote(
            Long cardId,
            MainNoteDto mainNote
    ) {
        public static UpdateMainNote of(Card card) {
            return new UpdateMainNote(card.getId(), MainNoteDto.of(card));
        }
    }

    // ─── 5. Summary 수정 응답 ─────────────────────────────────
    public record UpdateSummary(
            Long cardId,
            String summary
    ) {
        public static UpdateSummary of(Card card) {
            return new UpdateSummary(card.getId(), card.getSummary().getValue());
        }
    }

    // ─── 6·7·8. Keyword 관련 응답 (전체 교체 / 단건 추가 / 단건 제거 공통) ──
    public record Keywords(
            Long cardId,
            List<KeywordDto> keywords
    ) {
        public static Keywords of(Card card) {
            return new Keywords(card.getId(), KeywordDto.listOf(card));
        }
    }

    // ─── 9·10·11. 태그 관련 응답 (단건 추가 / 단건 제거 / 전체 교체 공통) ──
    public record Tags(
            Long cardId,
            List<TagDto> tags
    ) {
        public static Tags of(Card card) {
            return new Tags(card.getId(), TagDto.listOf(card));
        }
    }

    // ─── 12. 관련 카드 후보 조회 응답 ─────────────────────────
    public record RelatedCard(
            Long cardId,
            String summary,
            List<SharedTagDto> sharedTags,
            int sharedTagCount,
            MainContentType contentType
    ) {
        public static RelatedCard of(RelatedCardCandidate candidate) {
            return new RelatedCard(
                    candidate.getCard().getId(),
                    candidate.getCard().getSummary().getValue(),
                    candidate.getSharedTags().stream().map(SharedTagDto::of).toList(),
                    candidate.getSharedTagCount(),
                    candidate.getCard().getMainNote().getContentType()
            );
        }
    }

    // ─── 공통 중첩 DTO ────────────────────────────────────────

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
            return card.getKeywordCues().stream()
                       .map(KeywordDto::of)
                       .toList();
        }
    }

    /**
     * 카드에 연결된 태그 응답 DTO.
     */
    public record TagDto(
            Long id,
            String value,
            LocalDateTime linkedAt
    ) {
        public static TagDto of(CardTag cardTag) {
            return new TagDto(
                    cardTag.getTag().getId(),
                    cardTag.getTag().getValue(),
                    cardTag.getLinkedAt()
            );
        }

        public static List<TagDto> listOf(Card card) {
            return card.getCardTags().stream()
                       .map(TagDto::of)
                       .toList();
        }
    }

    /**
     * 관련 카드 후보에서 공유된 태그 응답 DTO.
     */
    public record SharedTagDto(
            Long id,
            String value
    ) {
        public static SharedTagDto of(Tag tag) {
            return new SharedTagDto(tag.getId(), tag.getValue());
        }
    }
}