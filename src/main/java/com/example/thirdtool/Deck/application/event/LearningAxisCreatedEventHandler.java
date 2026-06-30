package com.example.thirdtool.Deck.application.event;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.LearningFacade.domain.event.LearningAxisCreatedEvent;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Axis 생성 이벤트를 수신해 Axis 단위 Deck을 자동 생성한다 (ADR007).
 *
 * <p><strong>동기 처리 — {@code @TransactionalEventListener} 아님</strong>.
 * Axis 등록 트랜잭션 안에서 즉시 실행된다.
 *
 * <p><strong>멱등 보장</strong>: 동일 axisId로 Deck이 이미 존재하면 재생성하지 않는다.
 * Axis 1개 = Deck 1개 (1:1).
 */
@Component
@RequiredArgsConstructor
public class LearningAxisCreatedEventHandler {

    private final DeckRepository deckRepository;
    private final UserRepository userRepository;

    @EventListener
    public void handle(LearningAxisCreatedEvent event) {
        if (deckRepository.existsByAxisIdAndDeletedFalse(event.axisId())) {
            return;
        }

        UserEntity user = userRepository.findById(event.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Deck deck = Deck.createFromAxis(user, event.axisId(), event.axisName());
        deckRepository.save(deck);
    }
}
