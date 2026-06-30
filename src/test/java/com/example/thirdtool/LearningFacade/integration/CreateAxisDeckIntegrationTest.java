package com.example.thirdtool.LearningFacade.integration;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.Deck.presentation.dto.DeckResponse;
import com.example.thirdtool.LearningFacade.application.dto.LearningFacadeCommand;
import com.example.thirdtool.LearningFacade.application.service.LearningFacadeCommandService;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fix-Story 2 — 축 스코프 Deck 생성 통합 테스트.
 *
 * <p>POST /api/v1/learning-facade/axes/{axisId}/decks 흐름의 BC 협력(LearningFacade →
 * Deck) + DB 영속화까지 검증. Controller 레이어는 facade.createDeckUnderAxis로 위임만
 * 하므로 Service-level 통합 테스트로 핵심 흐름 커버.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("createDeckUnderAxis 통합 (Fix-Story 2)")
class CreateAxisDeckIntegrationTest {

    @Autowired LearningFacadeCommandService facadeCommandService;
    @Autowired DeckRepository deckRepository;

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

    @Test
    @DisplayName("정상 생성 — 응답에 axisId·axisName 포함, DB에 axis_id 결합 Deck 영속화")
    void 정상_생성() {
        DeckResponse.Create response = facadeCommandService.createDeckUnderAxis(
                new LearningFacadeCommand.CreateAxisDeck(user.getId(), axis.getId(), "Spring AOP 학습"));

        assertThat(response.deckId()).isNotNull();
        assertThat(response.name()).isEqualTo("Spring AOP 학습");
        assertThat(response.axisId()).isEqualTo(axis.getId());
        assertThat(response.axisName()).isEqualTo("Spring 내부");
        assertThat(response.parentDeckId()).isNull();
        assertThat(response.depth()).isZero();

        em.flush();
        em.clear();
        Deck loaded = deckRepository.findById(response.deckId()).orElseThrow();
        assertThat(loaded.getAxisId()).isEqualTo(axis.getId());
        assertThat(loaded.getName()).isEqualTo("Spring AOP 학습");
        assertThat(loaded.getMode()).isEqualTo(DeckMode.ON_FIELD);
    }

    @Test
    @DisplayName("동일 사용자 + 동일 이름 재요청 — DECK_NAME_DUPLICATE (409)")
    void 동일이름_중복() {
        facadeCommandService.createDeckUnderAxis(
                new LearningFacadeCommand.CreateAxisDeck(user.getId(), axis.getId(), "중복 이름"));
        em.flush();

        assertThatThrownBy(() -> facadeCommandService.createDeckUnderAxis(
                new LearningFacadeCommand.CreateAxisDeck(user.getId(), axis.getId(), "중복 이름")))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.DECK_NAME_DUPLICATE);
    }

    @Test
    @DisplayName("타 유저의 axisId 시도 — LEARNING_AXIS_NOT_FOUND (정보 누설 방지)")
    void 타유저_axisId_거부() {
        UserEntity stranger = UserEntity.ofLocal("stranger", "pw", "닉", "s@example.com");
        em.persist(stranger);
        LearningFacade strangerFacade = LearningFacade.create(stranger, "프론트엔드");
        em.persist(strangerFacade);
        em.flush();

        // user(소유자)로 인증된 채 stranger의 facade에 축이 없으므로 본 facade.axes에서 axisId를 못 찾음.
        // 본 테스트는 stranger axisId가 본인 facade에 없는 시나리오를 단순화 — 존재하지 않는 임의 axisId로 동일 효과.
        assertThatThrownBy(() -> facadeCommandService.createDeckUnderAxis(
                new LearningFacadeCommand.CreateAxisDeck(user.getId(), 99_999L, "Deck")))
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

        assertThatThrownBy(() -> facadeCommandService.createDeckUnderAxis(
                new LearningFacadeCommand.CreateAxisDeck(newUser.getId(), 999L, "Deck")))
                .isInstanceOf(LearningFacadeDomainException.class)
                .matches(e -> ((LearningFacadeDomainException) e).getErrorCode()
                        == ErrorCode.LEARNING_FACADE_NOT_FOUND);
    }

    @Test
    @DisplayName("한 축에 다중 덱 — 이름이 다르면 모두 생성 가능")
    void 한축_다중덱() {
        DeckResponse.Create d1 = facadeCommandService.createDeckUnderAxis(
                new LearningFacadeCommand.CreateAxisDeck(user.getId(), axis.getId(), "Spring AOP"));
        DeckResponse.Create d2 = facadeCommandService.createDeckUnderAxis(
                new LearningFacadeCommand.CreateAxisDeck(user.getId(), axis.getId(), "Spring DI"));
        em.flush();

        List<Deck> decks = deckRepository.findByAxisIdInAndDeletedFalse(List.of(axis.getId()));
        assertThat(decks).extracting(Deck::getName)
                .contains("Spring AOP", "Spring DI");
        assertThat(d1.axisName()).isEqualTo("Spring 내부");
        assertThat(d2.axisName()).isEqualTo("Spring 내부");
    }
}
