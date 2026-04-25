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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LearningFacadeRepository slice 테스트 (@DataJpaTest + H2).
 *
 * <p>slice §1-3 S1, S2 시나리오에 대응한다.
 * S3 (uk_learning_facade_user 위반)은 운영 코드 도메인에 명시적 unique 제약이
 * 없어 본 PR에서는 제외. S4 (soft delete) 역시 @SQLRestriction 미적용으로 제외.
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
}
