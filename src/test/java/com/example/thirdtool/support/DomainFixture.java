package com.example.thirdtool.support;

import com.example.thirdtool.Card.domain.model.*;
import com.example.thirdtool.Deck.domain.model.Deck;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class DomainFixture {

    // ─── Deck ─────────────────────────────────────────────────────────────────

    public static Deck sampleDeck() {
        Deck deck = Mockito.mock(Deck.class);
        Mockito.when(deck.getId()).thenReturn(1L);
        Mockito.when(deck.getName()).thenReturn("테스트 덱");
        return deck;
    }

    // ─── Card ─────────────────────────────────────────────────────────────────

    /** 기본 카드 (keyword: LIFO, push, pop / 태그 없음) */
    public static Card sampleCard() {
        return Card.create(
                sampleDeck(),
                MainNote.of("스택은 LIFO 구조다.", null),
                Summary.of("스택은 LIFO 구조다."),
                List.of("LIFO", "push", "pop")
                          );
    }

    /** ID가 있는 카드 (CardRelationFinder 테스트용) */
    public static Card sampleCardWithId(Long id) {
        Card card = sampleCard();
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    /** lastViewedAt이 설정된 카드 (SoftScheduleTemplate 테스트용) */
    public static Card sampleCardWithLastViewedAt(LocalDateTime lastViewedAt) {
        Card card = sampleCard();
        ReflectionTestUtils.setField(card, "lastViewedAt", lastViewedAt);
        return card;
    }

    /** enteredFieldAt이 설정된 카드 (OnFieldBudget 기간 초과 테스트용) */
    public static Card sampleCardWithEnteredFieldAt(LocalDateTime enteredFieldAt) {
        Card card = sampleCard();
        ReflectionTestUtils.setField(card, "enteredFieldAt", enteredFieldAt);
        return card;
    }

    /** ARCHIVE 상태 카드 */
    public static Card archivedCard() {
        Card card = sampleCard();
        card.archive();
        return card;
    }

    // ─── Tag ──────────────────────────────────────────────────────────────────

    public static Tag sampleTag(String value) {
        return Tag.of(value);
    }

    /** ID가 있는 태그 (CardRelationFinder - Set<Long> tagId 비교용) */
    public static Tag sampleTagWithId(Long id, String value) {
        Tag tag = Tag.of(value);
        ReflectionTestUtils.setField(tag, "id", id);
        return tag;
    }

    // ─── OnFieldBudget ────────────────────────────────────────────────────────

    public static OnFieldBudget sampleBudget() {
        return OnFieldBudget.of(3, Duration.ofDays(10));
    }

    public static OnFieldBudget budgetWithMaxView(int maxView) {
        return OnFieldBudget.of(maxView, Duration.ofDays(30));
    }

    public static OnFieldBudget budgetWithMaxDuration(Duration maxDuration) {
        return OnFieldBudget.of(10, maxDuration);
    }

    // ─── SoftScheduleTemplate ─────────────────────────────────────────────────

    /** 기본 템플릿 [1일, 3일, 7일] */
    public static SoftScheduleTemplate defaultTemplate() {
        return SoftScheduleTemplate.of(List.of(
                new SoftScheduleTemplate.IntervalStep(Duration.ofDays(1), SoftScheduleState.INTERVAL_1D),
                new SoftScheduleTemplate.IntervalStep(Duration.ofDays(3), SoftScheduleState.INTERVAL_3D),
                new SoftScheduleTemplate.IntervalStep(Duration.ofDays(7), SoftScheduleState.INTERVAL_7D)
                                              ));
    }
}