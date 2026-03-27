package com.example.thirdtool.Deck.infrastructure.repository;

import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

        // ─── 단건 조회 ────────────────────────────────────────

        /**
         * 활성 덱 단건 조회.
         * 논리 삭제된 덱은 반환하지 않는다.
         * DeckQueryService.getActiveDeck()에서 사용한다.
         */
        Optional<Deck> findByIdAndDeletedFalse(Long id);

        /**
         * 특정 사용자의 가장 최근 접근 덱 단건 조회.
         */
        Optional<Deck> findFirstByUserIdAndDeletedFalseOrderByLastAccessedDesc(Long userId);

        // ─── 목록 조회 ────────────────────────────────────────

        /**
         * 사용자의 최상위 덱(부모 없는 덱) 목록 조회.
         * 논리 삭제된 덱 제외.
         */
        List<Deck> findByUserIdAndParentDeckIsNullAndDeletedFalse(Long userId);

        /**
         * 특정 부모 덱 하위의 덱 목록 조회.
         * 논리 삭제된 덱 제외.
         */
        List<Deck> findByUserIdAndParentDeckIdAndDeletedFalse(Long userId, Long parentDeckId);

        /**
         * 사용자의 최근 접근 덱 상위 N개 조회.
         * 논리 삭제된 덱 제외.
         * 기본 5개. 개수 변경이 필요하면 DeckQueryRepository의 QueryDSL 메서드를 사용한다.
         */
        List<Deck> findTop5ByUserIdAndDeletedFalseOrderByLastAccessedDesc(Long userId);

        // ─── 중복 검사 ────────────────────────────────────────

        /**
         * 동일 사용자 내 덱 이름 중복 여부 확인.
         * 논리 삭제된 덱은 중복 대상에서 제외한다.
         */
        boolean existsByUserIdAndNameAndDeletedFalse(Long userId, String name);

        // ─── 업데이트 ─────────────────────────────────────────

        /**
         * lastAccessed 필드 원자적 업데이트.
         *
         * <p>userId를 함께 검증해 타인의 덱을 수정하는 것을 방지한다.
         * 반환값이 0이면 덱이 존재하지 않거나 소유자가 다른 경우다.
         *
         * <p>{@code clearAutomatically = true}: 업데이트 후 1차 캐시를 비워
         * 이후 조회 시 DB에서 최신값을 읽도록 한다.
         */
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("UPDATE Deck d SET d.lastAccessed = CURRENT_TIMESTAMP WHERE d.id = :deckId AND d.user.id = :userId")
        int touchLastAccessed(@Param("userId") Long userId,
                              @Param("deckId") Long deckId);

        Page<Deck> findRootDecksByUserId(Long userId, Pageable pageable);
}