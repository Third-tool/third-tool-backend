package com.example.thirdtool.support;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @DataJpaTest는 기본적으로 JpaRepository만 자동 구성한다.
 * QueryDSL JPAQueryFactory를 쓰는 다른 BC의 Custom Repository(예: CardJpaRepositoryImpl)
 * 가 함께 스캔되어 의존성을 요구하므로 본 Config로 JPAQueryFactory Bean을 공급한다.
 *
 * <p>각 Repository slice 테스트에서 {@code @Import(QuerydslTestConfig.class)}로 사용한다.
 */
@TestConfiguration
public class QuerydslTestConfig {

    @PersistenceContext
    private EntityManager em;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(em);
    }
}
