package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Card.infrastructure.persistence.CardStatusHistoryRepository;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

//도메인 서비스
@Component
@RequiredArgsConstructor
public class CardStatusHistoryAppender {

    private final CardStatusHistoryRepository historyRepository;

    // 변경 이력 테이블 규칙 부여
    public void append(Card card, CardStatus fromStatus, CardStatus toStatus, ArchiveReason reason) {
        if (fromStatus == toStatus) {
            // 상태 변화 없음 — 이력 생성 불필요
            return;
        }

        // 방향 검증은 CardStatusHistory.of() 내부에서 수행한다.
        // Appender는 올바른 인자를 전달하는 책임만 가진다. -validator
        if (toStatus == CardStatus.ARCHIVE && reason == null) {
            throw CardDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "CardStatusHistoryAppender: ON_FIELD → ARCHIVE 이력에는 reason이 필수입니다.");
        }
        if (toStatus == CardStatus.ON_FIELD && reason != null) {
            throw CardDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "CardStatusHistoryAppender: ARCHIVE → ON_FIELD 복귀 이력에는 reason을 전달하지 않습니다. reason=" + reason);
        }

        CardStatusHistory history = CardStatusHistory.of(card, fromStatus, toStatus, reason);
        historyRepository.save(history);
    }
}