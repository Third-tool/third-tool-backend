package com.example.thirdtool.Review.presentation.dto;

import com.example.thirdtool.Card.domain.model.KeywordCue;
import com.example.thirdtool.Card.domain.model.MainContentType;
import com.example.thirdtool.Review.domain.model.CardReview;
import com.example.thirdtool.Review.domain.model.CardVisibleContent;
import com.example.thirdtool.Review.domain.model.ReviewSession;
import com.example.thirdtool.Review.domain.model.ReviewStep;
import com.example.thirdtool.Review.infrastructure.dto.ReviewSessionSummaryRow;

import java.time.LocalDateTime;
import java.util.List;

public class ReviewResponse {

    // ─── 1. 리뷰 세션 시작 응답 (201) ────────────────────────────────────────
    public record StartSession(
            Long sessionId,
            Long deckId,
            String deckName,
            int totalCardCount,
            int currentIndex,
            LocalDateTime startedAt,
            boolean isFinished,
            CardReviewDto currentCard   // 첫 카드. 항상 RECALLING 상태.
    ) {
        public static StartSession of(ReviewSession session, boolean isLastView) {
            return new StartSession(
                    session.getId(),
                    session.getDeck().getId(),
                    session.getDeck().getName(),
                    session.getCardReviews().size(),
                    session.getCurrentIndex(),
                    session.getStartedAt(),
                    session.isFinished(),
                    CardReviewDto.of(session.currentCardReview(), isLastView)
            );
        }
    }

    // ─── 2. 세션 단건 조회 응답 (200) ────────────────────────────────────────
    public record SessionDetail(
            Long sessionId,
            Long deckId,
            String deckName,
            int totalCardCount,
            int currentIndex,
            LocalDateTime startedAt,
            boolean isFinished,
            CardReviewDto currentCard   // isFinished == true이면 null
    ) {
        public static SessionDetail of(ReviewSession session, boolean isLastView) {
            CardReviewDto currentCard = session.isFinished()
                    ? null
                    : CardReviewDto.of(session.currentCardReview(), isLastView);

            return new SessionDetail(
                    session.getId(),
                    session.getDeck().getId(),
                    session.getDeck().getName(),
                    session.getCardReviews().size(),
                    session.getCurrentIndex(),
                    session.getStartedAt(),
                    session.isFinished(),
                    currentCard
            );
        }
    }

    // ─── 3. 현재 카드 COMPARING 전환 응답 (200) ──────────────────────────────
    // CardReviewDto를 직접 반환한다.

    // ─── 4. 다음 카드 이동 응답 (200) ────────────────────────────────────────
    public record NextCard(
            Long sessionId,
            int currentIndex,
            boolean isFinished,
            CardReviewDto currentCard   // isFinished == true이면 null
    ) {
        public static NextCard of(ReviewSession session, boolean isLastView) {
            CardReviewDto currentCard = session.isFinished()
                    ? null
                    : CardReviewDto.of(session.currentCardReview(), isLastView);

            return new NextCard(
                    session.getId(),
                    session.getCurrentIndex(),
                    session.isFinished(),
                    currentCard
            );
        }
    }

    // ─── 5. 세션 목록 아이템 응답 (200) ──────────────────────────────────────
    public record SessionSummary(
            Long sessionId,
            Long deckId,
            String deckName,
            int totalCardCount,
            LocalDateTime startedAt
    ) {
        public static SessionSummary of(ReviewSessionSummaryRow row) {
            return new SessionSummary(
                    row.getSessionId(),
                    row.getDeckId(),
                    row.getDeckName(),
                    row.getTotalCardCount(),
                    row.getStartedAt()
            );
        }
    }

    // ─── 공통 중첩 DTO ────────────────────────────────────────────────────────
    public record CardReviewDto(
            Long cardReviewId,
            Long cardId,
            int cardOrder,
            ReviewStep reviewStep,
            boolean isLastView,
            MainNoteDto mainNote,
            List<KeywordDto> keywordCues,   // RECALLING이면 null
            String summary,                  // RECALLING이면 null
            LocalDateTime comparingStartedAt
    ) {
        public static CardReviewDto of(CardReview cardReview, boolean isLastView) {
            CardVisibleContent content = cardReview.visibleContent();

            List<KeywordDto> keywordCues = content.keywordCues() == null
                    ? null
                    : content.keywordCues().stream().map(KeywordDto::of).toList();

            String summary = content.summary() == null
                    ? null
                    : content.summary().getValue();

            return new CardReviewDto(
                    cardReview.getId(),
                    cardReview.getCard().getId(),
                    cardReview.getCardOrder(),
                    cardReview.getReviewStep(),
                    isLastView,
                    MainNoteDto.of(cardReview),
                    keywordCues,
                    summary,
                    cardReview.getComparingStartedAt()
            );
        }
    }

    public record MainNoteDto(
            String textContent,
            String imageUrl,
            MainContentType contentType
    ) {
        public static MainNoteDto of(CardReview cardReview) {
            var mainNote = cardReview.getCard().getMainNote();
            return new MainNoteDto(
                    mainNote.getTextContent(),
                    mainNote.getImageUrl(),
                    mainNote.getContentType()
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
    }
}