package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * product-card.md Epic 5 Story 5-1 — Tag 관리 화면의 "Tag 삭제" 액션.
 *
 * <p>본인 활성 카드에서 해당 Tag 부착(`card_tag` row)만 일괄 해제한다.
 * Tag row 자체는 시스템 전역 UNIQUE 자원이므로 보존(다른 유저가 동일 Tag를 사용 중일 수 있음 —
 * Open Question 5 v1 결정).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TagCommandService {

    private final CardRepository cardRepository;

    /**
     * 본인 카드에서 해당 Tag의 모든 부착을 해제하고 영향받은 매핑 row 수를 반환한다.
     */
    public int detachTagFromMyCards(Long tagId, Long userId) {
        return cardRepository.detachTagFromUserCards(userId, tagId);
    }
}
