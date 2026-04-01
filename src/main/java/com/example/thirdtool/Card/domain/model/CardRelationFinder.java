package com.example.thirdtool.Card.domain.model;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CardRelationFinder {

    public List<RelatedCardCandidate> findCandidates(
            Card currentCard,
            List<Card> taggedCards
                                                    ) {
        Set<Long> currentTagIds = extractTagIds(currentCard);

        return taggedCards.stream()
                          // 현재 카드 자신 제외
                          .filter(card -> !card.getId().equals(currentCard.getId()))
                          // 태그가 없는 카드 제외
                          .filter(card -> !card.getCardTags().isEmpty())
                          // 공유 태그 계산
                          .map(card -> {
                              List<Tag> sharedTags = card.getCardTags().stream()
                                                         .map(CardTag::getTag)
                                                         .filter(tag -> currentTagIds.contains(tag.getId()))
                                                         .toList();
                              return Map.entry(card, sharedTags);
                          })
                          // 공유 태그 0개 카드 제외
                          .filter(entry -> !entry.getValue().isEmpty())
                          // RelatedCardCandidate 변환
                          .map(entry -> RelatedCardCandidate.of(entry.getKey(), entry.getValue()))
                          // sharedTagCount 내림차순 정렬
                          .sorted(Comparator.comparingInt(RelatedCardCandidate::getSharedTagCount).reversed())
                          .toList();
    }

    private Set<Long> extractTagIds(Card card) {
        return card.getCardTags().stream()
                   .map(CardTag::getTag)
                   .map(Tag::getId)
                   .collect(Collectors.toSet());
    }
}