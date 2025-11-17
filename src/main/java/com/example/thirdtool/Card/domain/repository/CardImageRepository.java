package com.example.thirdtool.Card.domain.repository;

import com.example.thirdtool.Card.domain.model.CardImage;
import com.example.thirdtool.Card.domain.model.ImageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardImageRepository extends JpaRepository<CardImage, Long> {
    List<CardImage> findByCardIdOrderBySequenceAsc(Long cardId);
    List<CardImage> findByCardIdAndImageTypeOrderBySequenceAsc(Long cardId, ImageType type);
    Optional<CardImage> findFirstByCardDeckId(Long deckId);

    /** ✅ 덱 1개 대표 이미지: QUESTION → sequence ASC → card.id DESC → image.id ASC */
    @Query("""
        select ci
        from CardImage ci
        join ci.card c
        where c.deck.id = :deckId
          and (c.deleted = false or c.deleted is null)
        order by
          case when ci.imageType = com.example.thirdtool.Card.domain.model.ImageType.QUESTION then 0 else 1 end,
          ci.sequence asc nulls last,
          c.id desc,
          ci.id asc
        """)
    Page<CardImage> pickDeckThumbnail(@Param("deckId") Long deckId, Pageable pageable);

    default Optional<CardImage> pickDeckThumbnailOne(Long deckId) {
        Page<CardImage> p = pickDeckThumbnail(deckId, PageRequest.of(0, 1));
        return p.hasContent() ? Optional.of(p.getContent().get(0)) : Optional.empty();
    }
}