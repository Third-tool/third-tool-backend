package com.example.thirdtool.Deck.domain.repository;

import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.presentation.dto.DeckSearchDto;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {
        // parentDeck이 null인 모든 덱을 조회하는 메서드
        List<Deck> findByParentDeckIsNull();

        // ✅ 최근 학습한 덱 5개를 가져오는 쿼리 메서드
        List<Deck> findTop5ByOrderByLastAccessedDesc();

        // ✅ 특정 부모 덱 ID를 가진 하위 덱들을 조회하는 쿼리 메서드
        List<Deck> findByParentDeckId(Long parentDeckId);

        List<Deck> findByIsSharedTrue();

        // ✅ userId와 덱 이름으로 덱을 조회하는 메서드
        @Query("SELECT d FROM Deck d WHERE d.user.id = :userId and d.name = :deckName")
        List<Deck> findAllByUserIdAndDeckName(@Param("userId")Long userId, @Param("deckName")String deckName);

        // ✅ userId와 태그 ID로 덱을 조회하는 메서드
        @Query("SELECT d FROM Deck d JOIN d.tags t WHERE d.user.id = :userId and t.id IN :tagIds")
        List<Deck> findAllByUserIdAndTagId(@Param("userId") Long userId, @Param("tagIds") List<Long> tagIds);

        //공유 라이브러리의 검색 자동 완성 기능
        @Query("SELECT new com.example.thirdtool.Deck.presentation.dto.DeckSearchDto(d.id, d.name) " +
            "FROM Deck d WHERE d.isShared = true AND d.name LIKE :keyword% ")
        List<DeckSearchDto> findPublicDecksByNameStartingWith(@Param("keyword") String keyword, Pageable pageable);

        //내 덱의 검색 자동 완성 기능
        @Query("SELECT new com.example.thirdtool.Deck.presentation.dto.DeckSearchDto(d.id,d.name) FROM Deck d WHERE d.user.id = :userId AND d.name LIKE :keyword%")
        List<DeckSearchDto> findUserDecksByNameStartingWith(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

        // ✅ lastAccessed 필드를 원자적으로 업데이트하는 JPQL 쿼리
        @Modifying
        @Query("UPDATE Deck d SET d.lastAccessed = :lastAccessed WHERE d.id = :deckId")
        void updateLastAccessed(@Param("deckId") Long deckId, @Param("lastAccessed") LocalDateTime lastAccessed);

}
