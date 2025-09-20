package com.example.thirdtool.Tag.domain.repository;

import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Tag.domain.model.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByUserId(Long userId);
    Optional<Tag> findByIdAndUserId(Long userId, Long tagId);
    Optional<Tag> findByUserIdAndNameKey(Long userId, String nameKey);

    Optional<Tag> findByNameKeyIgnoreCaseAndUserId(String nameKey, Long userId);
}

