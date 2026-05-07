package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.RevisionReasonOption;
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

@DataJpaTest
@ActiveProfiles("test")
@Import({RevisionReasonOptionRepositoryAdapter.class, QuerydslTestConfig.class})
@DisplayName("RevisionReasonOptionRepository slice")
class RevisionReasonOptionRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    RevisionReasonOptionRepositoryAdapter repository;

    @BeforeEach
    void setUp() {
        em.persist(RevisionReasonOption.of("기존 주제가 너무 좁았다", 1, true));
        em.persist(RevisionReasonOption.of("기존 주제가 너무 넓었다", 2, true));
        em.persist(RevisionReasonOption.of("더 정확한 표현을 찾았다", 3, true));
        em.persist(RevisionReasonOption.of("(폐기) 다른 사유", 4, false));
        em.flush();
    }

    @Test
    @DisplayName("active=true 선택지만 displayOrder 오름차순으로 반환한다")
    void findActiveOrderByDisplayOrderAsc() {
        List<RevisionReasonOption> result = repository.findActiveOrderByDisplayOrderAsc();

        assertThat(result)
                .hasSize(3)
                .extracting(RevisionReasonOption::getLabel)
                .containsExactly("기존 주제가 너무 좁았다", "기존 주제가 너무 넓었다", "더 정확한 표현을 찾았다");
    }

    @Test
    @DisplayName("findActiveById — 활성 선택지는 조회 가능")
    void findActiveById_active() {
        RevisionReasonOption seed = em.getEntityManager()
                .createQuery(
                        "select o from RevisionReasonOption o where o.label = :label",
                        RevisionReasonOption.class)
                .setParameter("label", "더 정확한 표현을 찾았다")
                .getSingleResult();

        Optional<RevisionReasonOption> result = repository.findActiveById(seed.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getLabel()).isEqualTo("더 정확한 표현을 찾았다");
    }

    @Test
    @DisplayName("findActiveById — 비활성 선택지는 empty")
    void findActiveById_inactive_empty() {
        RevisionReasonOption seed = em.getEntityManager()
                .createQuery(
                        "select o from RevisionReasonOption o where o.label = :label",
                        RevisionReasonOption.class)
                .setParameter("label", "(폐기) 다른 사유")
                .getSingleResult();

        Optional<RevisionReasonOption> result = repository.findActiveById(seed.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findActiveById — 미존재 id는 empty")
    void findActiveById_not_found_empty() {
        Optional<RevisionReasonOption> result = repository.findActiveById(99_999L);
        assertThat(result).isEmpty();
    }
}
