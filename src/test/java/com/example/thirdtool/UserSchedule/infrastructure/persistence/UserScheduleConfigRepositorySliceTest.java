package com.example.thirdtool.UserSchedule.infrastructure.persistence;

import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import com.example.thirdtool.UserSchedule.domain.model.LearningModeMappingPolicy;
import com.example.thirdtool.UserSchedule.domain.model.UserScheduleConfig;
import com.example.thirdtool.support.QuerydslTestConfig;
import org.junit.jupiter.api.BeforeEach;
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
 * UserScheduleConfigRepository slice 테스트 (@DataJpaTest + H2).
 *
 * <p>Story-4-2 V12 마이그레이션 정합 검증:
 * <ul>
 *   <li>findByUserId / existsByUserId 정상·미존재</li>
 *   <li>LearningMode enum 영속 round-trip (MODE_10D/20D/30D)</li>
 *   <li>updateMode 후 mapped_mode·raw_input_days 갱신 영속</li>
 * </ul>
 *
 * <p>user_id UNIQUE 위반은 DB 제약 동작이 H2/MySQL에서 다를 수 있어 통합 테스트 범위로 위임.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("UserScheduleConfigRepository slice — Story-4-2")
class UserScheduleConfigRepositorySliceTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    UserScheduleConfigRepository repository;

    private LearningModeMappingPolicy policy;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        policy = new LearningModeMappingPolicy();
        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        em.persist(user);
        em.flush();
    }

    @Test
    @DisplayName("findByUserId — 저장된 config는 userId로 단건 조회된다")
    void findByUserId_returnsExisting() {
        // given
        UserScheduleConfig config = UserScheduleConfig.create(user.getId(), 13, policy);
        repository.save(config);
        em.flush();
        em.clear();

        // when
        Optional<UserScheduleConfig> found = repository.findByUserId(user.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getRawInputDays()).isEqualTo(13);
        assertThat(found.get().getMappedMode()).isEqualTo(LearningMode.MODE_10D);
    }

    @Test
    @DisplayName("findByUserId — 미존재 userId는 Optional.empty")
    void findByUserId_empty() {
        assertThat(repository.findByUserId(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("existsByUserId — 저장 여부 boolean으로 반환")
    void existsByUserId_returnsBoolean() {
        assertThat(repository.existsByUserId(user.getId())).isFalse();

        repository.save(UserScheduleConfig.createDefault(user.getId(), policy));
        em.flush();

        assertThat(repository.existsByUserId(user.getId())).isTrue();
    }

    @Test
    @DisplayName("LearningMode enum — MODE_20D / MODE_30D round-trip")
    void enumRoundTrip_20D_and_30D() {
        // MODE_20D (15~24)
        UserEntity user2 = UserEntity.ofLocal("u2", "pw", "n2", "u2@e.com");
        em.persist(user2);
        repository.save(UserScheduleConfig.create(user2.getId(), 20, policy));

        // MODE_30D (25+)
        UserEntity user3 = UserEntity.ofLocal("u3", "pw", "n3", "u3@e.com");
        em.persist(user3);
        repository.save(UserScheduleConfig.create(user3.getId(), 30, policy));
        em.flush();
        em.clear();

        assertThat(repository.findByUserId(user2.getId()).orElseThrow().getMappedMode())
                .isEqualTo(LearningMode.MODE_20D);
        assertThat(repository.findByUserId(user3.getId()).orElseThrow().getMappedMode())
                .isEqualTo(LearningMode.MODE_30D);
    }

    @Test
    @DisplayName("Story 6-2 — dailyTarget 기본값 20 round-trip + updateDailyTarget 영속")
    void dailyTarget_default20_updatePersists() {
        // given — 기본 생성
        repository.save(UserScheduleConfig.create(user.getId(), 10, policy));
        em.flush();
        em.clear();

        UserScheduleConfig loaded = repository.findByUserId(user.getId()).orElseThrow();
        assertThat(loaded.getDailyTarget()).isEqualTo(20);

        // when — 50으로 수정
        loaded.updateDailyTarget(50);
        em.flush();
        em.clear();

        // then
        UserScheduleConfig reloaded = repository.findByUserId(user.getId()).orElseThrow();
        assertThat(reloaded.getDailyTarget()).isEqualTo(50);
        // 다른 필드는 영향 없음
        assertThat(reloaded.getRawInputDays()).isEqualTo(10);
        assertThat(reloaded.getMappedMode()).isEqualTo(LearningMode.MODE_10D);
    }

    @Test
    @DisplayName("updateMode — raw_input_days와 mapped_mode가 함께 갱신·영속된다")
    void updateMode_persistsBothFields() {
        // given — 10D 모드로 시작
        UserScheduleConfig config = UserScheduleConfig.create(user.getId(), 10, policy);
        repository.save(config);
        em.flush();
        em.clear();

        // when — 25로 수정 → MODE_30D
        UserScheduleConfig loaded = repository.findByUserId(user.getId()).orElseThrow();
        loaded.updateMode(25, policy);
        em.flush();
        em.clear();

        // then
        UserScheduleConfig reloaded = repository.findByUserId(user.getId()).orElseThrow();
        assertThat(reloaded.getRawInputDays()).isEqualTo(25);
        assertThat(reloaded.getMappedMode()).isEqualTo(LearningMode.MODE_30D);
    }
}
