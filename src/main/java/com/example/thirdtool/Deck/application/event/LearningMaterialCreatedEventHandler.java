package com.example.thirdtool.Deck.application.event;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.LearningFacade.domain.event.LearningMaterialCreatedEvent;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 학습 자료 등록 이벤트를 수신해 동명의 Deck을 자동 생성한다 (Story-005-1, ADR007).
 *
 * <p><strong>동기 처리 — {@code @TransactionalEventListener} 아님</strong>. 자료 등록 트랜잭션 안에서
 * 즉시 실행되며, 본 핸들러에서 throw된 예외는 호출자(LearningMaterialCommandService) 트랜잭션을 롤백시킨다.
 *
 * <p>동작:
 * <ul>
 *   <li>Deck 이름 결정: {@code requestedDeckName} 우선, 없으면 {@code materialName}</li>
 *   <li>동명 검사 (soft delete 제외): 존재 + {@code forceCreateDeck=false} → {@code DECK_NAME_DUPLICATE} (409)</li>
 *   <li>존재 + {@code forceCreateDeck=true} → suffix `(2)`, `(3)`, … 자동 부여 (최대 100회)</li>
 *   <li>{@code Deck.createFromLearningMaterial(...)} 호출 + 저장</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class LearningMaterialCreatedEventHandler {

    private static final int SUFFIX_MAX_TRY = 100;

    private final DeckRepository deckRepository;
    private final UserRepository userRepository;

    @EventListener
    public void handle(LearningMaterialCreatedEvent event) {
        String deckName = resolveBaseName(event);

        boolean exists = deckRepository.existsByUserIdAndNameAndDeletedFalse(event.userId(), deckName);

        if (exists && !event.forceCreateDeck()) {
            throw new BusinessException(ErrorCode.DECK_NAME_DUPLICATE);
        }

        if (exists) {
            deckName = findNextAvailableName(event.userId(), deckName);
        }

        UserEntity user = userRepository.findById(event.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Deck deck = Deck.createFromLearningMaterial(user, event.axisId(), event.materialId(), deckName);
        Deck saved = deckRepository.save(deck);

        // 호출자(LearningMaterialCommandService)가 응답에 사용 — 동기 이벤트의 결과 통신 (ADR007).
        event.setResult(saved.getId(), saved.getName());
    }

    private String resolveBaseName(LearningMaterialCreatedEvent event) {
        if (event.requestedDeckName() == null || event.requestedDeckName().isBlank()) {
            return event.materialName();
        }
        return event.requestedDeckName().trim();
    }

    private String findNextAvailableName(Long userId, String baseName) {
        for (int suffix = 2; suffix <= SUFFIX_MAX_TRY; suffix++) {
            String candidate = baseName + " (" + suffix + ")";
            if (!deckRepository.existsByUserIdAndNameAndDeletedFalse(userId, candidate)) {
                return candidate;
            }
        }
        // 100회 동명이 누적된 비정상 케이스 — DECK_NAME_DUPLICATE로 거부.
        throw new BusinessException(ErrorCode.DECK_NAME_DUPLICATE);
    }
}
