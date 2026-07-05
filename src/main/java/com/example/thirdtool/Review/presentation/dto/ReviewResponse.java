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

/**
 * ReviewResponse — REV E2 · Story 2-1/2-4 재편.
 *
 * <p>PR#2 궤적: deckId · deckName 필드 폐기 · batchId + finishedAt로 대체.
 * TodayCandidates는 DailyBatch 엔드포인트로 흡수 폐기 (Story 2-5).
 */
public class ReviewResponse {

    // ─── 1. 리뷰 세션 시작 응답 (201) ────────────────────────────────────────
    public record StartSession(
            Long sessionId,
            Long batchId,
            int totalCardCount,
            int availableCardCount,
            int currentIndex,
            LocalDateTime startedAt,
            boolean isFinished,
            CardReviewDto currentCard   // 첫 카드. 항상 RECALLING 상태.
    ) {
        public static StartSession of(ReviewSession session) {
            return new StartSession(
                    session.getId(),
                    session.getBatch().getId(),
                    session.getTotalCardCount(),
                    session.getAvailableCardCount(),
                    session.getCurrentIndex(),
                    session.getStartedAt(),
                    session.isFinished(),
                    CardReviewDto.of(session.currentCardReview())
            );
        }
    }

    // ─── 2. 세션 단건 조회 응답 (200) ────────────────────────────────────────
    public record SessionDetail(
            Long sessionId,
            Long batchId,
            int totalCardCount,
            int availableCardCount,
            int currentIndex,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            boolean isFinished,
            CardReviewDto currentCard   // isFinished == true이면 null
    ) {
        public static SessionDetail of(ReviewSession session) {
            CardReviewDto currentCard = session.isFinished()
                    ? null
                    : CardReviewDto.of(session.currentCardReview());

            return new SessionDetail(
                    session.getId(),
                    session.getBatch().getId(),
                    session.getTotalCardCount(),
                    session.getAvailableCardCount(),
                    session.getCurrentIndex(),
                    session.getStartedAt(),
                    session.getFinishedAt(),
                    session.isFinished(),
                    currentCard
            );
        }
    }

    // ─── 3. 다음 카드 이동 응답 (200) ────────────────────────────────────────
    public record NextCard(
            Long sessionId,
            int currentIndex,
            boolean isFinished,
            CardReviewDto currentCard   // isFinished == true이면 null
    ) {
        public static NextCard of(ReviewSession session) {
            CardReviewDto currentCard = session.isFinished()
                    ? null
                    : CardReviewDto.of(session.currentCardReview());

            return new NextCard(
                    session.getId(),
                    session.getCurrentIndex(),
                    session.isFinished(),
                    currentCard
            );
        }
    }

    // ─── 4. 세션 목록 아이템 응답 (200) ──────────────────────────────────────
    public record SessionSummary(
            Long sessionId,
            Long batchId,
            int totalCardCount,
            int availableCardCount,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
        public static SessionSummary of(ReviewSessionSummaryRow row) {
            return new SessionSummary(
                    row.getSessionId(),
                    row.getBatchId(),
                    row.getTotalCardCount(),
                    row.getAvailableCardCount(),
                    row.getStartedAt(),
                    row.getFinishedAt()
            );
        }
    }

    // ─── 5. Finish 응답 (200) ────────────────────────────────────────────────
    public record FinishSession(
            Long sessionId,
            boolean isFinished,
            LocalDateTime finishedAt
    ) {
        public static FinishSession of(ReviewSession session) {
            return new FinishSession(
                    session.getId(),
                    session.isFinished(),
                    session.getFinishedAt()
            );
        }
    }

    // ─── 공통 중첩 DTO ────────────────────────────────────────────────────────

    public record CardReviewDto(
            Long cardReviewId,
            Long cardId,
            int cardOrder,
            ReviewStep reviewStep,
            MainNoteDto mainNote,
            List<KeywordDto> keywordCues,   // RECALLING이면 null
            String summary,                  // RECALLING이면 null
            LocalDateTime comparingStartedAt
    ) {
        public static CardReviewDto of(CardReview cardReview) {
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
