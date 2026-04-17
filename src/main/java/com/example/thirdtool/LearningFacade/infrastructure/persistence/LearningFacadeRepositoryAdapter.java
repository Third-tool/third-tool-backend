package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LearningFacadeRepositoryAdapter implements LearningFacadeRepository {

    private final LearningFacadeJpaRepository jpa;

    @Override
    public Optional<LearningFacade> findByUserId(Long userId) {
        // @EntityGraph({"axes", "axes.actions"}) 적용 → 이중 컬렉션 SELECT 분리 로딩
        return jpa.findByUserId(userId);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return jpa.existsByUserId(userId);
    }

    @Override
    public LearningFacade save(LearningFacade facade) {
        return jpa.save(facade);
    }
}
