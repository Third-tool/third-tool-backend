package com.example.thirdtool.Card.infrastructure.dto;

/**
 * Story 5-1 — Tag 관리 화면용 프로젝션.
 *
 * <p>본인 카드(deleted=false)에 부착된 Tag 단위 집계.
 * `connectedCardCount`는 해당 Tag가 부착된 본인 활성 카드 수.
 */
public record TagSummaryRow(
        Long tagId,
        String value,
        long connectedCardCount
) {
}
