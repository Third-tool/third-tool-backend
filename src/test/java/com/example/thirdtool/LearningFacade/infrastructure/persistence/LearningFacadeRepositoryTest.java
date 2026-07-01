package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.support.QuerydslTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LearningFacadeRepository slice 테스트 (@DataJpaTest + H2).
 *
 * <p>slice §1-3 S1, S2 시나리오에 대응한다.
 * S3 (uk_learning_facade_user 위반)은 운영 코드 도메인에 명시적 unique 제약이
 * 없어 본 PR에서는 제외.
 * S4 (soft delete) — Fix Axis↔Deck 완전 통합 2026-07-01부로 LearningAxis에
 * {@code @SQLRestriction("deleted_at IS NULL")} 도입, 아래 SoftDelete 케이스 추가.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({LearningFacadeRepositoryAdapter.class, QuerydslTestConfig.class})
@DisplayName("LearningFacadeRepository slice")
class LearningFacadeRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    LearningFacadeRepositoryAdapter repository;

    @Test
    @DisplayName("S1: 저장된 Facade를 userId로 조회하면 Optional에 담겨 반환된다")
    void S1_findByUserId_존재() {
        // given
        UserEntity user = UserEntity.ofLocal(
                "tester-1", "encoded-pw", "닉네임-1", "tester1@example.com");
        em.persist(user);

        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        em.persist(facade);
        em.flush();
        em.clear();

        // when
        Optional<LearningFacade> result = repository.findByUserId(user.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getConcept()).isEqualTo("백엔드 개발자");
    }

    @Test
    @DisplayName("S2: 저장 이력 없는 userId로 조회하면 Optional.empty를 반환한다")
    void S2_findByUserId_없음() {
        // when
        Optional<LearningFacade> result = repository.findByUserId(999_999L);

        // then
        assertThat(result).isEmpty();
    }

    // ─── findAxisNamesByIds — cross-BC read (Fix-Story 1) ─────────────

    @Test
    @DisplayName("findAxisNamesByIds — 여러 axisId 입력 시 (id, name) Map 반환")
    void findAxisNamesByIds_정상() {
        // given
        UserEntity user = UserEntity.ofLocal(
                "axis-name-tester", "encoded-pw", "닉네임", "axis@example.com");
        em.persist(user);

        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        facade.addAxis("시스템 설계");
        facade.addAxis("Spring 내부");
        em.persist(facade);
        em.flush();

        List<Long> axisIds = facade.getAxes().stream()
                .map(axis -> axis.getId())
                .toList();

        // when
        Map<Long, String> result = repository.findAxisNamesByIds(axisIds);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(axisIds.get(0))).isEqualTo("시스템 설계");
        assertThat(result.get(axisIds.get(1))).isEqualTo("Spring 내부");
    }

    @Test
    @DisplayName("findAxisNamesByIds — 미존재 axisId는 결과 Map에 포함되지 않음")
    void findAxisNamesByIds_미존재() {
        // when
        Map<Long, String> result = repository.findAxisNamesByIds(List.of(99_999L, 88_888L));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAxisNamesByIds — null 입력은 빈 Map (DB 호출 회피)")
    void findAxisNamesByIds_null() {
        Map<Long, String> result = repository.findAxisNamesByIds(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAxisNamesByIds — 빈 입력은 빈 Map (DB 호출 회피)")
    void findAxisNamesByIds_빈입력() {
        Map<Long, String> result = repository.findAxisNamesByIds(List.of());
        assertThat(result).isEmpty();
    }

    // ─── S4: Soft Delete — @SQLRestriction 필터 검증 (Fix Axis↔Deck 완전 통합) ─────

    @Test
    @DisplayName("S4-1: 축을 softDelete하면 @SQLRestriction으로 findByUserId 조회 시 getAxes()에서 자동 제외")
    void S4_softDelete_후_getAxes_필터() {
        // given: 축 2개 중 1개 소프트 삭제
        UserEntity user = UserEntity.ofLocal(
                "s4-tester", "encoded-pw", "닉네임", "s4@example.com");
        em.persist(user);

        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        facade.addAxis("살아있는 축");
        facade.addAxis("삭제될 축");
        em.persist(facade);
        em.flush();

        Long toDeleteAxisId = facade.getAxes().stream()
                .filter(a -> a.getName().equals("삭제될 축"))
                .findFirst().orElseThrow()
                .getId();
        facade.removeAxis(toDeleteAxisId);
        em.flush();
        em.clear();

        // when: 재조회 (@SQLRestriction 필터가 DB 쿼리 레벨에서 적용되는지 검증)
        Optional<LearningFacade> reloaded = repository.findByUserId(user.getId());

        // then: 활성 축만 로드
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getAxes()).hasSize(1);
        assertThat(reloaded.get().getAxes().get(0).getName()).isEqualTo("살아있는 축");
    }

    @Test
    @DisplayName("S4-2: findAxisNamesByIds — 소프트 삭제된 축의 axisId는 결과에서 자동 제외")
    void S4_findAxisNamesByIds_softDeleted_필터() {
        // given: 축 2개 중 1개 소프트 삭제
        UserEntity user = UserEntity.ofLocal(
                "s4-tester-2", "encoded-pw", "닉네임", "s4-2@example.com");
        em.persist(user);

        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        facade.addAxis("살아있는 축");
        facade.addAxis("삭제될 축");
        em.persist(facade);
        em.flush();

        Long aliveAxisId = facade.getAxes().stream()
                .filter(a -> a.getName().equals("살아있는 축"))
                .findFirst().orElseThrow().getId();
        Long toDeleteAxisId = facade.getAxes().stream()
                .filter(a -> a.getName().equals("삭제될 축"))
                .findFirst().orElseThrow().getId();
        facade.removeAxis(toDeleteAxisId);
        em.flush();
        em.clear();

        // when: 두 axisId 모두 요청
        Map<Long, String> result = repository.findAxisNamesByIds(List.of(aliveAxisId, toDeleteAxisId));

        // then: 살아있는 축만 반환 (Deck 응답에 삭제된 축 name이 새어나가지 않음)
        assertThat(result).hasSize(1);
        assertThat(result.get(aliveAxisId)).isEqualTo("살아있는 축");
        assertThat(result).doesNotContainKey(toDeleteAxisId);
    }

    @Test
    @DisplayName("S4-3: 소프트 삭제된 축과 동일 이름의 신규 축 생성 시 UNIQUE 제약 통과 (V14 3-column composite)")
    void S4_동일이름_재사용_UNIQUE통과() {
        // given: "알고리즘" 축 생성 → 소프트 삭제
        UserEntity user = UserEntity.ofLocal(
                "s4-tester-3", "encoded-pw", "닉네임", "s4-3@example.com");
        em.persist(user);

        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        facade.addAxis("알고리즘");
        em.persist(facade);
        em.flush();

        Long deletedAxisId = facade.getAxes().get(0).getId();
        facade.removeAxis(deletedAxisId);
        em.flush();
        em.clear();

        // when: 재조회 후 동일 이름 "알고리즘" 축 신규 추가 — V14의 3-column UNIQUE로 통과해야 함
        LearningFacade reloaded = repository.findByUserId(user.getId()).orElseThrow();
        reloaded.addAxis("알고리즘");
        em.persist(reloaded);
        em.flush();
        em.clear();

        // then: 활성 축 1개 (신규), 소프트 삭제된 축은 필터
        LearningFacade finalState = repository.findByUserId(user.getId()).orElseThrow();
        assertThat(finalState.getAxes()).hasSize(1);
        assertThat(finalState.getAxes().get(0).getName()).isEqualTo("알고리즘");
        assertThat(finalState.getAxes().get(0).getId()).isNotEqualTo(deletedAxisId);
    }
}
