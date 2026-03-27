package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.CardStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardStatusHistoryJpaRepository extends JpaRepository<CardStatusHistory, Long> {
}
