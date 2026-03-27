package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.CardStatusHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CardStatusHistoryRepositoryAdapter
        implements CardStatusHistoryRepository {

    private final CardStatusHistoryJpaRepository jpaRepository;

    @Override
    public CardStatusHistory save(CardStatusHistory history) {
        return jpaRepository.save(history);
    }
}
