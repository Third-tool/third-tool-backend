package com.example.thirdtool.Deck.domain.repository;

import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {
        boolean existsByUserAndName(UserEntity user, String name);

        // parentDeck이 null인 모든 덱을 조회하는 메서드
        List<Deck> findByUserIdAndParentDeckIsNull(Long userId);

        // ✅ 최근 학습한 덱 5개를 가져오는 쿼리 메서드
        List<Deck> findTop5ByUserIdOrderByLastAccessedDesc(Long userId);

        // ✅ 특정 부모 덱 ID를 가진 하위 덱들을 조회하는 쿼리 메서드
        List<Deck> findByUserIdAndParentDeckId(Long userId, Long parentDeckId);

        // ✅ lastAccessed 필드를 원자적으로 업데이트하는 JPQL 쿼리
        @Modifying
        @Query("UPDATE Deck d SET d.lastAccessed = :lastAccessed WHERE d.id = :deckId")
        void updateLastAccessed(@Param("deckId") Long deckId, @Param("lastAccessed") LocalDateTime lastAccessed);

        @Query("""
        select avg(lp.score) from Card c
        join c.learningProfile lp
        where c.deck.id = :deckId
    """)
        Double findAvgScore(@Param("deckId") Long deckId);

        List<Deck> findByUser(UserEntity user);

        Optional<Deck> findFirstByUserIdOrderByLastAccessedDesc(Long userId);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("update Deck d set d.lastAccessed = :now where d.id = :deckId and d.user.id = :userId")
        int touchLastAccessed(@Param("userId") Long userId,
                              @Param("deckId") Long deckId,
                              @Param("now") LocalDateTime now);
}
