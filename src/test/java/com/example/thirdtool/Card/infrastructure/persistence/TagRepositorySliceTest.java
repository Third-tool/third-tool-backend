package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.domain.model.Tag;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.support.QuerydslTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TagRepository slice 테스트 (@DataJpaTest + H2).
 *
 * <p>Story 1-1 — Tag·CardTag 테이블 V11 마이그레이션 대응 슬라이스 검증.
 * <ul>
 *   <li>tag.value 시스템 전역 UNIQUE 제약 (`uk_tag_value`) 동작</li>
 *   <li>findByValue 단건 조회가 find-or-create 패턴에 정합</li>
 *   <li>CardTag UNIQUE(card_id, tag_id) 제약으로 동일 Card-Tag 중복 연결 차단</li>
 * </ul>
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("TagRepository slice — Story 1-1 Tag·CardTag 제약")
class TagRepositorySliceTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    TagJpaRepository tagJpaRepository;

    @Autowired
    CardJpaRepository cardJpaRepository;

    private UserEntity user;
    private Deck deck;

    @BeforeEach
    void setUp() {
        user = UserEntity.ofLocal("tester-1", "encoded-pw", "닉네임", "tester1@example.com");
        em.persist(user);
        deck = Deck.createFromAxis(user, 1L, "테스트 덱");
        em.persist(deck);
        em.flush();
    }

    @Test
    @DisplayName("findByValue — 저장된 Tag는 value로 단건 조회된다")
    void findByValue_returnsExistingTag() {
        // given
        Tag tag = Tag.of("백엔드");
        tagJpaRepository.save(tag);
        em.flush();
        em.clear();

        // when
        Optional<Tag> found = tagJpaRepository.findByValue("백엔드");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getValue()).isEqualTo("백엔드");
    }

    @Test
    @DisplayName("findByValue — 미존재 value 조회 시 Optional.empty 반환")
    void findByValue_returnsEmpty_whenAbsent() {
        // when
        Optional<Tag> found = tagJpaRepository.findByValue("존재하지않음");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("find-or-create 패턴 — 동일 value 입력 시 기존 Tag row가 재사용된다")
    void findOrCreate_reusesExistingTag() {
        // given — 첫 저장
        Tag saved = tagJpaRepository.save(Tag.of("백엔드"));
        em.flush();
        em.clear();

        // when — 동일 value로 findByValue 호출 (find-or-create의 find 단계)
        Optional<Tag> found = tagJpaRepository.findByValue("백엔드");

        // then — 새로 만들지 않고 기존 id로 매핑된다
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());

        // 도메인 규약 — `tag.value` 시스템 전역 UNIQUE는 MySQL Flyway V11에서 `uk_tag_value`로 강제한다.
        // H2 슬라이스에서는 매핑·쿼리 정합만 검증하고 UNIQUE 위반은 통합 테스트(@SpringBootTest + Flyway 적용) 범위.
    }

    @Test
    @DisplayName("CardTag UNIQUE(card_id, tag_id) — 동일 Card-Tag 중복 연결은 도메인 차단 (CARD_TAG_ALREADY_EXISTS)")
    void addTag_duplicate_blockedByDomain() {
        // given
        Tag tag = tagJpaRepository.save(Tag.of("백엔드"));
        Card card = persistCard(tag);
        em.flush();
        em.clear();

        Card reloaded = cardJpaRepository.findById(card.getId()).orElseThrow();
        Tag reloadedTag = tagJpaRepository.findByValue("백엔드").orElseThrow();

        // when & then — 도메인 단에서 중복을 사전 차단한다 (DB 제약까지 가지 않음).
        assertThatThrownBy(() -> reloaded.addTag(reloadedTag))
                .hasMessageContaining("이미");
    }

    // ─── helper ────────────────────────────────────────────────────────────────

    private Card persistCard(Tag tag) {
        Card card = Card.create(
                deck,
                MainNote.of("스택은 LIFO 구조다.", null),
                Summary.of("스택은 LIFO 구조다."),
                List.of("LIFO", "push", "pop"),
                List.of(tag)
        );
        em.persist(card);
        em.flush();
        return card;
    }
}
