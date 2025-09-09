package com.example.thirdtool.Deck.domain.repository;

import com.example.thirdtool.Deck.domain.model.Deck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {
        // parentDeck이 null인 모든 덱을 조회하는 메서드
        List<Deck> findByParentDeckIsNull();

        // ✅ 최근 학습한 덱 5개를 가져오는 쿼리 메서드
        List<Deck> findTop5ByOrderByLastAccessedDesc();

        // ✅ 특정 부모 덱 ID를 가진 하위 덱들을 조회하는 쿼리 메서드
        List<Deck> findByParentDeckId(Long parentDeckId);

        @Query("SELECT d FROM Deck d JOIN d.tags t WHERE t.name = :tagName")
        List<Deck> findAllByTagName(@Param("tagName") String tagName);

        // ✅ lastAccessed 필드를 원자적으로 업데이트하는 JPQL 쿼리
        @Modifying
        @Query("UPDATE Deck d SET d.lastAccessed = :lastAccessed WHERE d.id = :deckId")
        void updateLastAccessed(@Param("deckId") Long deckId, @Param("lastAccessed") LocalDateTime lastAccessed);
}
