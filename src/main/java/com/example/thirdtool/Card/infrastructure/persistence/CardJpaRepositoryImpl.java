package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.MainContentType;
import com.example.thirdtool.Card.domain.model.QCard;
import com.example.thirdtool.Card.domain.model.QKeywordCue;
import com.example.thirdtool.Card.infrastructure.dto.CardSearchCondition;
import com.example.thirdtool.Card.infrastructure.dto.CardSummaryRow;
import com.example.thirdtool.Card.infrastructure.dto.QCardSummaryRow;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class CardJpaRepositoryImpl implements CardRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QCard       card       = QCard.card;
    private static final QKeywordCue keywordCue = QKeywordCue.keywordCue;

    /**
     * 카드 검색 (동적 조건 + 페이징).
     * <p>논리 삭제된 카드는 항상 제외한다 ({@code card.deleted.isFalse()}).
     * summaryKeyword / contentType 조건은 null이면 자동으로 WHERE절에서 제외된다.
     */
    @Override
    public Page<CardSummaryRow> searchCards(CardSearchCondition condition, Pageable pageable) {

        List<CardSummaryRow> content = queryFactory
                .select(new QCardSummaryRow(
                        card.id,
                        card.summary.value,
                        card.mainNote.contentType,
                        keywordCountSubquery()
                ))
                .from(card)
                .where(
                        card.deleted.isFalse(),
                        summaryKeywordContains(condition.summaryKeyword()),
                        contentTypeEq(condition.contentType())
                      )
                .orderBy(card.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(card.count())
                .from(card)
                .where(
                        card.deleted.isFalse(),
                        summaryKeywordContains(condition.summaryKeyword()),
                        contentTypeEq(condition.contentType())
                      )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    // ─── 서브쿼리 ─────────────────────────────────────────

    /**
     * 카드에 연결된 keywordCue 수를 서브쿼리로 집계한다.
     * keywordCues 컬렉션 초기화 없이 카운트만 조회하기 위해 서브쿼리를 사용한다.
     */
    private Expression<Long> keywordCountSubquery() {
        return JPAExpressions
                .select(keywordCue.count())
                .from(keywordCue)
                .where(keywordCue.card.eq(card));
    }

    // ─── 동적 조건 ────────────────────────────────────────

    /**
     * Summary에 키워드가 포함되는지 검사한다.
     * null 또는 빈 문자열이면 조건을 무시한다 (null 반환 → QueryDSL WHERE절 자동 제외).
     *
     * ⚠️ LIKE '%keyword%' 형태로 앞 와일드카드 포함 — 인덱스 미사용.
     * 데이터 증가 시 Full-Text Index 또는 검색 엔진 도입 검토.
     */
    private BooleanExpression summaryKeywordContains(String keyword) {
        return StringUtils.hasText(keyword)
                ? card.summary.value.containsIgnoreCase(keyword)
                : null;
    }

    /**
     * MainNote의 contentType이 일치하는지 검사한다.
     * null이면 조건을 무시한다.
     */
    private BooleanExpression contentTypeEq(MainContentType contentType) {
        return contentType != null
                ? card.mainNote.contentType.eq(contentType)
                : null;
    }
}