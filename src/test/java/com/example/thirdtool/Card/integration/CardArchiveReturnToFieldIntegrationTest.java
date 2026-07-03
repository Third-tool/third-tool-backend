package com.example.thirdtool.Card.integration;

import com.example.thirdtool.Card.application.service.CardCommandService;
import com.example.thirdtool.Card.domain.model.ArchiveReason;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardStatus;
import com.example.thirdtool.Card.domain.model.CardStatusHistory;
import com.example.thirdtool.Card.infrastructure.persistence.CardJpaRepository;
import com.example.thirdtool.Card.infrastructure.persistence.CardStatusHistoryJpaRepository;
import com.example.thirdtool.Card.presentation.dto.CardRequest;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.model.DeckProgressStatus;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * product-card.md Epic 7 — Archive → ON_field 복귀 사이클 통합 검증.
 *
 * <p>Card 도메인 행위 + CardStatusHistoryAppender + Deck progressStatus 재계산까지
 * Spring 컨텍스트 전체에서 정합한지 확인한다. 단위 테스트(CardCommandServiceArchiveTest)는
 * Mock 기반이라 영속 흐름의 회귀를 잡지 못한다.
 *
 * <p>인수 시나리오 (Story 7-1)
 * <ul>
 *   <li>이전 ON_FIELD 구간 통계는 유지(`CardStatusHistory`)되고 새 사이클은 깨끗한 상태에서 시작</li>
 *   <li>복귀 시점 `enteredFieldAt` 새 기록, `viewCount=0`, `lastViewedAt=null` 재초기화</li>
 *   <li>Deck progressStatus가 COMPLETED → IN_PROGRESS로 회귀</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Archive ↔ ON_field 복귀 사이클 통합 — Story 7-1")
class CardArchiveReturnToFieldIntegrationTest {

    @Autowired CardCommandService cardCommandService;
    @Autowired CardJpaRepository cardJpaRepository;
    @Autowired CardStatusHistoryJpaRepository historyJpaRepository;

    @PersistenceContext EntityManager em;

    private UserEntity user;
    private Deck deck;
    private Card card;
    private LocalDateTime initialEnteredFieldAt;

    @BeforeEach
    void setUp() {
        user = UserEntity.ofLocal("owner", "encoded-pw", "닉네임", "owner@example.com");
        em.persist(user);
        deck = Deck.createFromAxis(user, 1L, "테스트 덱");
        em.persist(deck);
        em.flush();

        var request = new CardRequest.Create(
                new CardRequest.MainNoteDto("스택은 LIFO 구조다.", null),
                List.of("LIFO", "push", "pop"),
                "스택은 LIFO 구조다.",
                List.of()
        );
        Long cardId = cardCommandService.create(deck.getId(), request).cardId();
        em.flush();
        em.clear();

        card = cardJpaRepository.findById(cardId).orElseThrow();
        initialEnteredFieldAt = card.getEnteredFieldAt();
    }

    @Test
    @DisplayName("archive → returnToField — Card 필드 재초기화 + History 2건 + Deck IN_PROGRESS 회귀")
    void archive_then_returnToField_resetsCard_keepsHistory_recalculatesDeck() {
        // given — 노출 누적해 viewCount/lastViewedAt이 0/null 아님을 보장
        card.recordView();
        em.flush();

        // when — archive(MANUAL) → returnToField
        cardCommandService.archive(card.getId(), ArchiveReason.MANUAL);
        em.flush();
        em.clear();

        Card afterArchive = cardJpaRepository.findById(card.getId()).orElseThrow();
        assertThat(afterArchive.getStatus()).isEqualTo(CardStatus.ARCHIVE);
        assertThat(afterArchive.getDeck().getProgressStatus()).isEqualTo(DeckProgressStatus.COMPLETED);

        cardCommandService.returnToField(card.getId());
        em.flush();
        em.clear();

        Card afterReturn = cardJpaRepository.findById(card.getId()).orElseThrow();

        // then — 새 사이클 시작
        assertThat(afterReturn.getStatus()).isEqualTo(CardStatus.ON_FIELD);
        assertThat(afterReturn.getViewCount()).isZero();
        assertThat(afterReturn.getLastViewedAt()).isNull();
        assertThat(afterReturn.getEnteredFieldAt()).isNotNull();
        assertThat(afterReturn.getEnteredFieldAt()).isAfterOrEqualTo(initialEnteredFieldAt);

        // Deck recalculate → IN_PROGRESS 회귀 (활성 카드 1건 모두 ON_FIELD)
        assertThat(afterReturn.getDeck().getProgressStatus()).isEqualTo(DeckProgressStatus.IN_PROGRESS);

        // 이력 2건 — ON_FIELD→ARCHIVE(MANUAL) + ARCHIVE→ON_FIELD(reason=null)
        List<CardStatusHistory> histories = historyJpaRepository.findAll();
        assertThat(histories).hasSize(2);
        assertThat(histories).extracting(CardStatusHistory::getFromStatus)
                .containsExactlyInAnyOrder(CardStatus.ON_FIELD, CardStatus.ARCHIVE);
        assertThat(histories).extracting(CardStatusHistory::getToStatus)
                .containsExactlyInAnyOrder(CardStatus.ARCHIVE, CardStatus.ON_FIELD);
        assertThat(histories).extracting(CardStatusHistory::getReason)
                .containsExactlyInAnyOrder(ArchiveReason.MANUAL, null);
    }

    @Test
    @DisplayName("이미 ON_FIELD인 카드에 returnToField — 멱등 no-op, 이력 미생성")
    void returnToField_onActiveCard_isIdempotent() {
        // 카드는 setUp 직후 ON_FIELD 상태
        assertThat(card.getStatus()).isEqualTo(CardStatus.ON_FIELD);

        cardCommandService.returnToField(card.getId());
        em.flush();

        assertThat(historyJpaRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("두 사이클 반복 — History 4건이 모두 보존 (이전 사이클 이력 손실 없음)")
    void twoFullCycles_preserveAllHistories() {
        // 사이클 1
        cardCommandService.archive(card.getId(), ArchiveReason.MANUAL);
        em.flush();
        cardCommandService.returnToField(card.getId());
        em.flush();

        // 사이클 2
        cardCommandService.archive(card.getId(), ArchiveReason.SCHEDULE_EXHAUSTED);
        em.flush();
        cardCommandService.returnToField(card.getId());
        em.flush();
        em.clear();

        List<CardStatusHistory> histories = historyJpaRepository.findAll();
        assertThat(histories).hasSize(4);

        Set<ArchiveReason> reasonsForArchive = histories.stream()
                .filter(h -> h.getToStatus() == CardStatus.ARCHIVE)
                .map(CardStatusHistory::getReason)
                .collect(java.util.stream.Collectors.toSet());
        assertThat(reasonsForArchive).containsExactlyInAnyOrder(
                ArchiveReason.MANUAL, ArchiveReason.SCHEDULE_EXHAUSTED
        );
    }
}
