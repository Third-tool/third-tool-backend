package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Tag;
import com.example.thirdtool.Card.infrastructure.dto.TagSummaryRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TagJpaRepository extends JpaRepository<Tag, Long> {

    /**
     * 태그 이름으로 단건 조회.
     * Tag.value에 DB 유니크 제약이 걸려 있으므로 0 또는 1건이 반환된다.
     */
    Optional<Tag> findByValue(String value);

    /**
     * Story 5-1 — 사용자가 부착한 Tag 목록 + 각 Tag별 본인 활성 카드 수.
     * <p>본인 카드 중 soft delete된 카드는 카운트에서 제외. ON_FIELD·ARCHIVE 모두 포함.
     * 정렬은 tag value 사전순(클라이언트가 다시 정렬해도 무관).
     */
    @Query("""
            SELECT new com.example.thirdtool.Card.infrastructure.dto.TagSummaryRow(
                       t.id, t.value, COUNT(DISTINCT ct.card.id))
            FROM CardTag ct
            JOIN ct.tag t
            WHERE ct.card.deck.user.id = :userId
              AND ct.card.deleted = false
            GROUP BY t.id, t.value
            ORDER BY t.value ASC
            """)
    List<TagSummaryRow> findTagSummariesByUserId(@Param("userId") Long userId);
}
