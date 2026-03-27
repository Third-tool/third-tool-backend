package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.infrastructure.persistence.CardStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardStatusHistoryAppender {

    private final CardStatusHistoryRepository historyRepository;

    // 변경 이력 테이블 규칙 부여
    public void append(Card card, CardStatus fromStatus, CardStatus toStatus) {
        if (fromStatus == toStatus) {
            // 멱등성 처리로 상태가 바뀌지 않은 경우 — 이력 생성 불필요
            return;
        }
        CardStatusHistory history = CardStatusHistory.of(card, fromStatus, toStatus);
        historyRepository.save(history);
    }
}