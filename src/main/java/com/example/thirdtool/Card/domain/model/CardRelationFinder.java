package com.example.thirdtool.Card.domain.model;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CardRelationFinder {

    /**
     * 현재 카드와 공유 태그를 가진 카드 목록을 관련 카드 후보로 변환한다.(미리 대기- 임시 세션)
     *
     * @param currentCard  기준 카드 (자기 자신은 후보에서 제외됨)
     * @param taggedCards  Repository에서 조회한 태그 공유 카드 목록
     * @return sharedTagCount 내림차순으로 정렬된 후보 목록
     */
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