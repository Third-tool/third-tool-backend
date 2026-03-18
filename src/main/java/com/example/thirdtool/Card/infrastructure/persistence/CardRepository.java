package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Card;

import java.util.Optional;

/**
 * Card Aggregate의 영속성 포트.
 *
 * 실제 구현은 infrastructure 레이어의 {@code CardRepositoryImpl}에서 담당한다.
 */
public interface CardRepository {

    Card save(Card card);

    Optional<Card> findById(Long id);

    void delete(Card card);
}

