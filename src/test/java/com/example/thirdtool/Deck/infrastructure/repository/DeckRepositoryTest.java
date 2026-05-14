package com.example.thirdtool.Deck.infrastructure.repository;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DeckRepository slice 테스트 (@DataJpaTest + H2).
 *
 * Story-005-2에서 추가된 두 조회 메서드 검증:
 * - findByAxisIdInAndDeletedFalse — N+1 회피 다중 축 일괄 조회
 * - findByLearningMaterialIdAndDeletedFalse — 자료 삭제 시 영향 Deck 조회
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("DeckRepository slice — Story-005-2 조회 메서드")
class DeckRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    DeckRepository deckRepository;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserEntity.ofLocal("tester-1", "encoded-pw", "닉네임", "tester1@example.com");
        em.persist(user);
        em.flush();
    }

    private Deck persistDeckWithFacadeLink(String name, Long axisId, Long materialId) {
        Deck deck = Deck.createFromLearningMaterial(user, axisId, materialId, name);
        em.persist(deck);
        em.flush();
        return deck;
    }

    @Test
    @DisplayName("findByAxisIdInAndDeletedFalse — 다중 축의 Deck 일괄 조회")
    void findByAxisIdIn_정상() {
        persistDeckWithFacadeLink("A 축 Deck1", 10L, 100L);
        persistDeckWithFacadeLink("A 축 Deck2", 10L, 101L);
        persistDeckWithFacadeLink("B 축 Deck", 20L, 102L);
        persistDeckWithFacadeLink("C 축 Deck (조회 제외)", 30L, 103L);

        List<Deck> result = deckRepository.findByAxisIdInAndDeletedFalse(List.of(10L, 20L));

        assertThat(result).extracting(Deck::getName)
                .containsExactlyInAnyOrder("A 축 Deck1", "A 축 Deck2", "B 축 Deck");
    }

    @Test
    @DisplayName("findByAxisIdInAndDeletedFalse — soft delete된 Deck 제외")
    void findByAxisIdIn_softDelete_제외() {
        Deck a1 = persistDeckWithFacadeLink("A 축 활성", 10L, 100L);
        Deck a2 = persistDeckWithFacadeLink("A 축 삭제예정", 10L, 101L);
        a2.softDelete();
        em.flush();

        List<Deck> result = deckRepository.findByAxisIdInAndDeletedFalse(List.of(10L));

        assertThat(result).extracting(Deck::getName).containsExactly("A 축 활성");
    }

    @Test
    @DisplayName("findByAxisIdInAndDeletedFalse — 빈 입력은 빈 리스트")
    void findByAxisIdIn_빈입력() {
        List<Deck> result = deckRepository.findByAxisIdInAndDeletedFalse(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByLearningMaterialIdAndDeletedFalse — 자료 1건 → 연결 Deck 다건 조회")
    void findByLearningMaterialId_정상() {
        persistDeckWithFacadeLink("자료 200 Deck", 10L, 200L);
        persistDeckWithFacadeLink("자료 200 Deck (2)", 10L, 200L);
        persistDeckWithFacadeLink("자료 201 Deck", 10L, 201L);

        List<Deck> result = deckRepository.findByLearningMaterialIdAndDeletedFalse(200L);

        assertThat(result).extracting(Deck::getName)
                .containsExactlyInAnyOrder("자료 200 Deck", "자료 200 Deck (2)");
    }

    @Test
    @DisplayName("findByLearningMaterialIdAndDeletedFalse — 미연결 자료ID는 빈 리스트")
    void findByLearningMaterialId_미존재() {
        persistDeckWithFacadeLink("자료 200 Deck", 10L, 200L);

        List<Deck> result = deckRepository.findByLearningMaterialIdAndDeletedFalse(999L);

        assertThat(result).isEmpty();
    }
}
