package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Tag;
import com.example.thirdtool.Card.infrastructure.dto.TagSummaryRow;

import java.util.List;
import java.util.Optional;

public interface TagRepository {

    Optional<Tag> findByValue(String value);

    Tag save(Tag tag);

    /**
     * Story 5-1 — 본인 카드에 부착된 Tag 목록 + 연결 카드 수.
     */
    List<TagSummaryRow> findTagSummariesByUserId(Long userId);
}

