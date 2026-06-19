package com.example.thirdtool.Card.presentation.dto;

import com.example.thirdtool.Card.infrastructure.dto.TagSummaryRow;

import java.util.List;

public class TagResponse {

    /**
     * GET /api/v1/tags — 본인 Tag 목록 응답.
     * connectedCardCount는 해당 Tag가 부착된 본인 활성 카드 수(ON_FIELD + ARCHIVE).
     */
    public record Item(
            Long tagId,
            String value,
            long connectedCardCount
    ) {
        public static Item of(TagSummaryRow row) {
            return new Item(row.tagId(), row.value(), row.connectedCardCount());
        }

        public static List<Item> listOf(List<TagSummaryRow> rows) {
            return rows.stream().map(Item::of).toList();
        }
    }
}
