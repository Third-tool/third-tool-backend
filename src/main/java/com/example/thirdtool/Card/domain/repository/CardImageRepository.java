package com.example.thirdtool.Card.domain.repository;

import com.example.thirdtool.Card.domain.model.CardImage;
import com.example.thirdtool.Card.domain.model.ImageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardImageRepository extends JpaRepository<CardImage, Long> {
    List<CardImage> findByCardIdOrderBySequenceAsc(Long cardId);
    List<CardImage> findByCardIdAndImageTypeOrderBySequenceAsc(Long cardId, ImageType type);
}