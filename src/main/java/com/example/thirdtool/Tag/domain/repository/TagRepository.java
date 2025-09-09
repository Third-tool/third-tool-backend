package com.example.thirdtool.Tag.domain.repository;

import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Tag.domain.model.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findAllById(List<Long> tagIds);

}

