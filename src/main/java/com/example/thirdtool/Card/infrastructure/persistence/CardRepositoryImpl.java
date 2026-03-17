package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.MainContentType;
import com.example.thirdtool.Card.domain.model.QCard;
import com.example.thirdtool.Card.domain.model.QKeywordCue;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class CardRepositoryImpl implements CardRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QCard card       = QCard.card;
    private static final QKeywordCue keywordCue = QKeywordCue.keywordCue;

    /**
     * 동적 조건 기반 카드 요약 목록을 페이징 조회한다.
     *
     * <p>keywordCount를 구하기 위해 서브쿼리를 사용한다.
     * 목록 조회이므로 KeywordCue 전체를 페치하지 않고 개수만 가져온다.
     */
    @Override
    public Page<CardSummaryRow> searchCards(CardSearchCondition condition, Pageable pageable) {

        List<CardSummaryRow> content = queryFactory
                .select(new QCardSummaryRow(
                        card.id,
                        card.summary.value,
                        card.mainNote.contentType,
                        card.mainNote.textContent,
                        // 서브쿼리로 키워드 개수만 조회 - 서브 쿼리로 관리
                        JPAExpressions
                                .select(keywordCue.count().intValue())
                                .from(keywordCue)
                                .where(keywordCue.card.eq(card))
                ))
                .from(card)
                .where(
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
                        summaryKeywordContains(condition.summaryKeyword()),
                        contentTypeEq(condition.contentType())
                      )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    // -------------------------------------------------------------------------
    // 동적 조건 헬퍼 메서드
    // -------------------------------------------------------------------------

    /**
     * Summary에 키워드가 포함되는지 검사한다.
     * null 또는 빈 문자열이면 조건을 무시한다.
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
