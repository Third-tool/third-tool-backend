package com.example.thirdtool.LearningFacade.integration;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardStatus;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.presentation.dto.CardResponse;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.LearningFacade.application.service.LearningFacadeQueryService;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * fix-deck-axis-visibility (0.0.2v) Story 4 — 축 스코프 Card 조회 통합 테스트.
 *
 * <p>GET /api/v1/learning-facade/axes/{axisId}/cards 흐름의 BC 협력(LearningFacade →
 * Card 단일 read-model) + 소유권 검증 + DB 영속화까지 검증. Controller는 facade.findAxisCards로
 * 위임만 하므로 Service-level 통합으로 핵심 흐름 커버.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("findAxisCards 통합 (Fix-Story 4)")
class AxisCardsQueryIntegrationTest {

    @Autowired LearningFacadeQueryService facadeQueryService;

    @PersistenceContext EntityManager em;

    private UserEntity user;
    private LearningFacade facade;
    private LearningAxis axis;

    @BeforeEach
    void setUp() {
        user = UserEntity.ofLocal("owner", "encoded-pw", "닉네임", "owner@example.com");
        em.persist(user);

        facade = LearningFacade.create(user, "백엔드");
        axis = facade.addAxis("Spring 내부");
        em.persist(facade);
        em.flush();
    }

    // axisId 결합 덱 — Story 2의 Deck.createUnderAxis에 의존하지 않도록 raw 컬럼만 세팅.
    private Deck deckUnderAxis(UserEntity user, Long axisId, String name) {
        Deck deck = Deck.of(name, null, user);
        ReflectionTestUtils.setField(deck, "axisId", axisId);
        return deck;
    }

    private Card persistCard(Deck deck, boolean archived) {
        Card card = Card.create(deck, MainNote.of("본문", null), Summary.of("한 문장."), List.of("k"));
        if (archived) card.archive();
        em.persist(card);
        em.flush();
        return card;
    }

    @Test
    @DisplayName("정상 — 축 결합 덱들의 ON_FIELD 카드만 단일 호출로 반환 (ARCHIVE·고아 덱 제외)")
    void 정상_onField() {
        Deck axisDeck1 = deckUnderAxis(user, axis.getId(),"deck1");
        Deck axisDeck2 = deckUnderAxis(user, axis.getId(),"deck2");
        Deck orphan = Deck.of("orphan", null, user);
        em.persist(axisDeck1);
        em.persist(axisDeck2);
        em.persist(orphan);
        em.flush();

        Card onField1 = persistCard(axisDeck1, false);
        Card onField2 = persistCard(axisDeck2, false);
        persistCard(axisDeck1, true);   // ARCHIVE — 제외
        persistCard(orphan, false);     // 고아 덱 — 제외
        em.clear();

        List<CardResponse.Summary> result =
                facadeQueryService.findAxisCards(user.getId(), axis.getId(), CardStatus.ON_FIELD);

        assertThat(result).extracting(CardResponse.Summary::cardId)
                .containsExactlyInAnyOrder(onField1.getId(), onField2.getId());
        assertThat(result).extracting(CardResponse.Summary::status)
                .containsOnly(CardStatus.ON_FIELD);
    }

    @Test
    @DisplayName("status=ARCHIVE 지정 시 ARCHIVE 카드만 반환")
    void archive_status() {
        Deck axisDeck = deckUnderAxis(user, axis.getId(),"deck");
        em.persist(axisDeck);
        em.flush();
        persistCard(axisDeck, false);                 // ON_FIELD — 제외
        Card archived = persistCard(axisDeck, true);  // ARCHIVE — 포함
        em.clear();

        List<CardResponse.Summary> result =
                facadeQueryService.findAxisCards(user.getId(), axis.getId(), CardStatus.ARCHIVE);

        assertThat(result).extracting(CardResponse.Summary::cardId)
                .containsExactly(archived.getId());
    }

    @Test
    @DisplayName("본인 facade에 없는 axisId — LEARNING_AXIS_NOT_FOUND")
    void 미소유_axisId_거부() {
        assertThatThrownBy(() ->
                facadeQueryService.findAxisCards(user.getId(), 99_999L, CardStatus.ON_FIELD))
                .isInstanceOf(LearningFacadeDomainException.class)
                .matches(e -> ((LearningFacadeDomainException) e).getErrorCode()
                        == ErrorCode.LEARNING_AXIS_NOT_FOUND);
    }

    @Test
    @DisplayName("facade 미보유 사용자 — LEARNING_FACADE_NOT_FOUND")
    void facade_미보유() {
        UserEntity newUser = UserEntity.ofLocal("nofacade", "pw", "닉", "nf@example.com");
        em.persist(newUser);
        em.flush();

        assertThatThrownBy(() ->
                facadeQueryService.findAxisCards(newUser.getId(), 999L, CardStatus.ON_FIELD))
                .isInstanceOf(LearningFacadeDomainException.class)
                .matches(e -> ((LearningFacadeDomainException) e).getErrorCode()
                        == ErrorCode.LEARNING_FACADE_NOT_FOUND);
    }
}
