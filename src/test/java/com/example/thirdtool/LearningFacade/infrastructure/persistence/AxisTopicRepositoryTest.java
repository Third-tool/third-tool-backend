package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.CoverageStatus;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
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

@DataJpaTest
@ActiveProfiles("test")
@Import({AxisTopicRepositoryAdapter.class, QuerydslTestConfig.class})
@DisplayName("AxisTopicRepository slice")
class AxisTopicRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    AxisTopicRepositoryAdapter repository;

    private LearningAxis axis;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-1", "encoded-pw", "닉네임-1", "tester1@example.com");
        em.persist(user);

        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        em.persist(facade);
        axis = facade.addAxis("API 설계");
        em.flush();
    }

    @Test
    @DisplayName("axisId로 조회하면 displayOrder 오름차순으로 정렬된 주제를 반환한다")
    void findByAxisIdOrderByDisplayOrderAsc() {
        axis.addTopic("REST API 설계 원칙", null);
        axis.addTopic("OpenAPI 문서화", null);
        axis.addTopic("버전 관리 전략", null);
        em.flush();
        em.clear();

        List<AxisTopic> result = repository.findByAxisIdOrderByDisplayOrderAsc(axis.getId());

        assertThat(result)
                .extracting(AxisTopic::getName)
                .containsExactly("REST API 설계 원칙", "OpenAPI 문서화", "버전 관리 전략");
    }

    @Test
    @DisplayName("coverageStatus로 필터 조회가 동작한다 — 자료 없는 주제만 조회")
    void findByAxisIdAndCoverageStatus() {
        AxisTopic t1 = axis.addTopic("주제 A", null);
        AxisTopic t2 = axis.addTopic("주제 B", null);
        em.flush();
        t2.updateCoverageStatus(CoverageStatus.PARTIAL);
        em.flush();
        em.clear();

        List<AxisTopic> uncovered = repository.findByAxisIdAndCoverageStatus(
                axis.getId(), CoverageStatus.NO_MATERIAL);

        assertThat(uncovered)
                .extracting(AxisTopic::getName)
                .containsExactly("주제 A");
    }
}
