package com.example.thirdtool.Deck.infrastructure.repository;


import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class DeckQueryRepository {
    private final JPAQueryFactory queryFactory;

}
