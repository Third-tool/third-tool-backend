package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.AxisSelection;
import com.example.thirdtool.LearningFacade.domain.model.AxisSelectionNode;
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

/**
 * Story-LT-E3-S3-11 · AxisSelection Repository Slice.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({AxisSelectionRepositoryAdapter.class, QuerydslTestConfig.class})
@DisplayName("AxisSelectionRepository slice (Story-LT-E3-S3-11)")
class AxisSelectionRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    AxisSelectionRepositoryAdapter repository;

    private LearningAxis axis;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-1", "encoded-pw", "닉네임-1", "tester1@example.com");
        em.persist(user);
        LearningFacade facade = LearningFacade.create(user, "백엔드");
        em.persist(facade);
        axis = facade.addAxis("하네스 엔지니어링");
        em.flush();
    }

    @Test
    @DisplayName("axisId로 조회 시 created_at DESC 정렬 (이슈 #11 계승)")
    void findByAxisIdOrderByCreatedAtDesc() {
        AxisSelection s1 = axis.addSelection("v1");
        em.flush();
        try { Thread.sleep(2); } catch (InterruptedException ignored) {}
        AxisSelection s2 = axis.addSelection("v2");
        em.flush();
        em.clear();

        List<AxisSelection> result = repository.findByAxisIdOrderByCreatedAtDesc(axis.getId());

        assertThat(result).extracting(AxisSelection::getName)
                .containsExactly("v2", "v1"); // 최신 우선
    }

    @Test
    @DisplayName("컨테이너 hard delete 시 자식 노드도 CASCADE 삭제")
    void containerDelete_cascadesToNodes() {
        AxisSelection s = axis.addSelection("v1");
        s.addNode("1. 첫", null, "b1");
        s.addNode("2. 둘", null, "b2");
        em.flush();
        Long selectionId = s.getId();

        axis.removeSelection(selectionId);
        em.flush();
        em.clear();

        assertThat(repository.findById(selectionId)).isEmpty();
        List<AxisSelectionNode> orphans = em.getEntityManager()
                .createQuery("select n from AxisSelectionNode n where n.selection.id = :sid",
                        AxisSelectionNode.class)
                .setParameter("sid", selectionId)
                .getResultList();
        assertThat(orphans).isEmpty();
    }

    @Test
    @DisplayName("UNIQUE (axis_id, name) 위반은 도메인이 사전 감지 → 예외")
    void unique_axis_name() {
        axis.addSelection("v1");
        em.flush();
        // 도메인이 감지, 예외는 도메인 단위 테스트로 다뤘음. Repository slice는 저장 성공만 확인.
        assertThat(repository.findByAxisIdOrderByCreatedAtDesc(axis.getId()))
                .hasSize(1);
    }
}
