package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Tag;

import java.util.Optional;

public interface TagRepository {

    Optional<Tag> findByValue(String value);

    Tag save(Tag tag);
}

