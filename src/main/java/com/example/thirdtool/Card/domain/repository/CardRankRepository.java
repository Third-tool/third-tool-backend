package com.example.thirdtool.Card.domain.repository;

import com.example.thirdtool.Card.domain.model.CardRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRankRepository extends JpaRepository<CardRank, Long> {
    // 특정 사용자의 ID와 랭크 이름(예: "GOLD")으로 랭크 기준을 찾습니다.
    Optional<CardRank> findByUserIdAndName(Long userId, String name);
}