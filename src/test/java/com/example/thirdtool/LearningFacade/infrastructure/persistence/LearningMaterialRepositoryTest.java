package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.LearningMaterial;
import com.example.thirdtool.LearningFacade.domain.model.MaterialType;
import com.example.thirdtool.LearningFacade.domain.model.ProficiencyLevel;
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
 * LearningMaterialRepository slice 테스트 (@DataJpaTest + H2).
 *
 * <p>slice §1-6 S15~S18 시나리오에 대응한다.
 * S18 (soft delete)은 운영 코드에 @SQLRestriction 미적용으로 제외.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("LearningMaterialRepository slice")
class LearningMaterialRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    LearningMaterialJpaRepository jpa;

    private LearningFacade facade;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-1", "encoded-pw", "닉네임-1", "tester1@example.com");
        em.persist(user);

        facade = LearningFacade.create(user, "백엔드 개발자");
        em.persist(facade);
        em.flush();
    }

    @Test
    @DisplayName("S15: facadeId로 조회하면 해당 Facade에 속한 자료를 모두 반환한다")
    void S15_findByFacadeId() {
        // given
        LearningMaterial m1 = LearningMaterial.create(facade, "DDD 책", MaterialType.TOP_DOWN, "https://e.com/1");
        LearningMaterial m2 = LearningMaterial.create(facade, "토비의 스프링", MaterialType.TOP_DOWN, "https://e.com/2");
        em.persist(m1);
        em.persist(m2);
        em.flush();
        em.clear();

        // when
        List<LearningMaterial> result = jpa.findByFacadeId(facade.getId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(LearningMaterial::getName)
                .containsExactlyInAnyOrder("DDD 책", "토비의 스프링");
    }

    @Test
    @DisplayName("S16: 자료 생성 시 proficiencyLevel은 UNRATED로 자동 초기화된다")
    void S16_persist_proficiencyLevel_UNRATED_기본값() {
        // given
        LearningMaterial material = LearningMaterial.create(
                facade, "DDD 책", MaterialType.TOP_DOWN, "https://e.com/ddd");
        em.persist(material);
        em.flush();
        em.clear();

        // when
        LearningMaterial found = em.find(LearningMaterial.class, material.getId());

        // then
        assertThat(found.getProficiencyLevel()).isEqualTo(ProficiencyLevel.UNRATED);
    }

    @Test
    @DisplayName("S17: url=null인 자료를 저장하면 조회 시 null이다 — nullable 컬럼 매핑 확인")
    void S17_persist_url_null_허용() {
        // given
        LearningMaterial material = LearningMaterial.create(
                facade, "팀원에게 공유받은 노션", MaterialType.BOTTOM_UP, null);
        em.persist(material);
        em.flush();
        em.clear();

        // when
        LearningMaterial found = em.find(LearningMaterial.class, material.getId());

        // then
        assertThat(found.getUrl()).isNull();
    }
}
