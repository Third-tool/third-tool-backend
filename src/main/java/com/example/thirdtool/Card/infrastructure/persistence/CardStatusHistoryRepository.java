package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.CardStatusHistory;


public interface CardStatusHistoryRepository {

    /**
     * CardStatusHistory를 저장한다.
     *
     * @param history 저장할 이력
     * @return 저장된 이력
     */
    CardStatusHistory save(CardStatusHistory history);
}
