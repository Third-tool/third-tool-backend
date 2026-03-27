package com.example.thirdtool.Deck.infrastructure.repository;


import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.Deck.domain.model.QDeck;
import com.example.thirdtool.Deck.infrastructure.dto.DeckSearchCondition;
import com.example.thirdtool.Deck.infrastructure.dto.DeckSummaryRow;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
@RequiredArgsConstructor
public class DeckQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QDeck deck = QDeck.deck;

    // ─── 1. 사용자 덱 목록 동적 검색 ─────────────────────────────────────────
    /**
     * 사용자의 덱 목록을 조건에 따라 동적으로 조회한다.
     *
     * <p>지원 조건:
     * <ul>
     *   <li>이름 부분 검색 (LIKE %keyword%)
     *   <li>최상위 덱만 조회 (parentDeck == null)
     *   <li>특정 부모 덱 하위만 조회
     *   <li>DeckMode 필터
     * </ul>
     *
     * <p>정렬: lastAccessed 내림차순 (최근 접근 순)
     * 논리 삭제된 덱은 항상 제외한다.
     */
    public List<DeckSummaryRow> searchDecks(DeckSearchCondition condition) {
        return queryFactory
                .select(new QDeckSummaryRow(
                        deck.id,
                        deck.name,
                        deck.mode,
                        deck.depth,
                        deck.lastAccessed,
                        deck.cards.size(),     // 카드 수
                        deck.subDecks.size()   // 하위 덱 수
                ))
                .from(deck)
                .where(
                        deck.user.id.eq(condition.getUserId()),
                        deck.deleted.isFalse(),
                        nameContains(condition.getKeyword()),
                        parentDeckEq(condition.getParentDeckId()),
                        rootDeckOnly(condition.isRootOnly()),
                        modeEq(condition.getMode())
                      )
                .orderBy(deck.lastAccessed.desc())
                .fetch();
    }

    // ─── 2. 최근 접근 덱 N개 조회 ─────────────────────────────────────────────
    /**
     * 사용자의 최근 접근 덱을 N개 조회한다.
     * {@link DeckRepository#findTop5ByUserIdAndDeletedFalseOrderByLastAccessedDesc}의
     * 개수 유연성이 필요할 때 사용한다.
     */
    public List<Deck> findRecentDecks(Long userId, int limit) {
        return queryFactory
                .selectFrom(deck)
                .where(
                        deck.user.id.eq(userId),
                        deck.deleted.isFalse()
                      )
                .orderBy(deck.lastAccessed.desc())
                .limit(limit)
                .fetch();
    }

    // ─── 3. 하위 덱 트리 조회 (1단계) ─────────────────────────────────────────
    /**
     * 특정 덱의 직계 하위 덱 목록을 조회한다.
     * 논리 삭제된 덱 제외.
     *
     * <p>전체 트리 재귀 조회가 필요하면 서비스 레이어에서
     * 이 메서드를 재귀적으로 호출하거나, 별도 재귀 쿼리를 작성한다.
     */
    public List<Deck> findDirectSubDecks(Long parentDeckId) {
        return queryFactory
                .selectFrom(deck)
                .where(
                        deck.parentDeck.id.eq(parentDeckId),
                        deck.deleted.isFalse()
                      )
                .orderBy(deck.name.asc())
                .fetch();
    }

    // ─── 4. 활성 덱 소유자 검증 포함 단건 조회 ────────────────────────────────
    /**
     * sessionId + userId로 활성 덱을 조회한다.
     * 소유자가 다르거나 삭제된 덱이면 빈 Optional을 반환한다.
     * 서비스 레이어에서 소유권 검증을 한 번의 쿼리로 처리할 때 사용한다.
     */
    public Optional<Deck> findActiveByIdAndUserId(Long deckId, Long userId) {
        Deck result = queryFactory
                .selectFrom(deck)
                .where(
                        deck.id.eq(deckId),
                        deck.user.id.eq(userId),
                        deck.deleted.isFalse()
                      )
                .fetchOne();
        return Optional.ofNullable(result);
    }

    // ─── 5. 이름 중복 검사 (대소문자 무시) ────────────────────────────────────
    /**
     * 동일 사용자 내 동일 이름 덱 존재 여부를 확인한다.
     * 대소문자를 무시한다 (equalsIgnoreCase).
     * 논리 삭제된 덱은 중복 대상에서 제외한다.
     *
     * <p>{@link DeckRepository#existsByUserIdAndNameAndDeletedFalse}는
     * 정확한 대소문자 일치만 검사한다. 대소문자 무시가 필요하면 이 메서드를 사용한다.
     */
    public boolean existsByUserIdAndNameIgnoreCase(Long userId, String name) {
        return queryFactory
                .selectOne()
                .from(deck)
                .where(
                        deck.user.id.eq(userId),
                        deck.name.equalsIgnoreCaseBinding(name),
                        deck.deleted.isFalse()
                      )
                .fetchFirst() != null;
    }

    // -------------------------------------------------------
    // 동적 조건 메서드
    // -------------------------------------------------------

    /** 이름 부분 검색. null이면 조건 없음. */
    private BooleanExpression nameContains(String keyword) {
        return (keyword != null && !keyword.isBlank())
                ? deck.name.containsIgnoreCase(keyword.trim())
                : null;
    }

    /** 특정 parentDeckId 필터. null이면 조건 없음. */
    private BooleanExpression parentDeckEq(Long parentDeckId) {
        return parentDeckId != null ? deck.parentDeck.id.eq(parentDeckId) : null;
    }

    /**
     * 최상위 덱만 조회 조건.
     * rootOnly == true이면 parentDeck IS NULL 조건 추가.
     * parentDeckEq와 동시에 사용하면 rootOnly가 우선 적용되어야 한다.
     */
    private BooleanExpression rootDeckOnly(boolean rootOnly) {
        return rootOnly ? deck.parentDeck.isNull() : null;
    }

    /** DeckMode 필터. null이면 조건 없음. */
    private BooleanExpression modeEq(DeckMode mode) {
        return mode != null ? deck.mode.eq(mode) : null;
    }
}