package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TagRepositoryAdapter implements TagRepository {

    private final TagJpaRepository tagJpaRepository;

    @Override
    public Optional<Tag> findByValue(String value) {
        return tagJpaRepository.findByValue(value);
    }

    @Override
    public Tag save(Tag tag) {
        return tagJpaRepository.save(tag);
    }
}

