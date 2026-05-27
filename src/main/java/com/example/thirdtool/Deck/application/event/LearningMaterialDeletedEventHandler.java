package com.example.thirdtool.Deck.application.event;

import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.LearningFacade.domain.event.LearningMaterialDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 학습 자료 삭제 이벤트를 수신해 연결된 Deck들의 자료 참조를 끊는다 (Story-005-2, ADR007).
 *
 * <p><strong>동기 처리 — {@code @TransactionalEventListener} 아님</strong>. 자료 삭제 트랜잭션 안에서
 * 즉시 실행되며, 본 핸들러의 변경(Deck.learningMaterialId = null)은 dirty checking으로
 * 동일 트랜잭션의 flush 시점에 함께 영속화된다.
 *
 * <p>FK 안전: Deck.learning_material_id가 자료를 참조하는 동안 자료를 먼저 삭제하면 FK violation 위험.
 * 따라서 호출자(LearningMaterialCommandService.deleteMaterial)는 {@code materialRepository.delete(material)}
 * <strong>이전</strong>에 본 이벤트를 발행한다.
 *
 * <p>멱등: 이미 {@code learningMaterialId == null}인 Deck은 {@link Deck#markMaterialDeleted()}가 무영향 처리한다.
 */
@Component
@RequiredArgsConstructor
public class LearningMaterialDeletedEventHandler {

    private final DeckRepository deckRepository;

    @EventListener
    public void handle(LearningMaterialDeletedEvent event) {
        List<Deck> affected = deckRepository.findByLearningMaterialIdAndDeletedFalse(event.materialId());
        affected.forEach(Deck::markMaterialDeleted);
    }
}
