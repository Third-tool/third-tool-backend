package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;

import java.util.Optional;


public interface LearningFacadeRepository {

    Optional<LearningFacade> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    LearningFacade save(LearningFacade facade);
}