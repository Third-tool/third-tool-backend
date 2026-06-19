package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.infrastructure.persistence.TagRepository;
import com.example.thirdtool.Card.presentation.dto.TagResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * product-card.md Epic 5 Story 5-1 — Tag 관리 화면 진입점 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagQueryService {

    private final TagRepository tagRepository;

    /**
     * 본인 카드에 부착된 Tag 목록 + 각 Tag별 연결 카드 수 반환.
     * Tag value 사전순 정렬은 Repository 쿼리가 보장.
     */
    public List<TagResponse.Item> listMyTags(Long userId) {
        return TagResponse.Item.listOf(tagRepository.findTagSummariesByUserId(userId));
    }
}
