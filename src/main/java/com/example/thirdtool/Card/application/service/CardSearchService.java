package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.Document.CardDocument;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardImage;
import com.example.thirdtool.Card.domain.repository.CardRepository;
import com.example.thirdtool.Card.domain.repository.CardSearchRepository;
import com.example.thirdtool.Card.presentation.dto.CardSearchResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardSearchService {

    private final CardSearchRepository cardSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final CardRepository cardRepository;


    public List<CardDocument> simpleSearch(Long userId, String keyword) {
        log.info("[CardSearchService] Simple search (userId={}, keyword={})", userId, keyword);

        // ✅ 사용자 + keyword 기준으로 검색
        Query query = NativeQuery.builder()
                                 .withQuery(q -> q.bool(b -> b
                                                 .must(m -> m.term(t -> t.field("userId").value(userId)))   // 🔒 userId 필터링
                                                 .should(s -> s.match(mq -> mq.field("question").query(keyword)))
                                                 .should(s -> s.match(mq -> mq.field("answer").query(keyword)))
                                                 .minimumShouldMatch("1") // 두 필드 중 하나만 일치해도 OK
                                                       ))
                                 .withSort(Sort.by(Sort.Order.desc("_score")))
                                 .build();

        SearchHits<CardDocument> hits = elasticsearchOperations.search(query, CardDocument.class);
        return hits.stream()
                   .map(SearchHit::getContent)
                   .toList();
    }

    public Slice<CardSearchResponseDto> searchCards(Long userId,String keyword, int page, int size) {
        int from = page * size;
        Pageable pageable = PageRequest.of(page, size);


        // 🧠 Elasticsearch fuzzy match query 구성
        Query query = NativeQuery.builder()
                                 .withQuery(q -> q.bool(b -> b
                                                 // ✅ userId 필터 (자신의 카드만)
                                                 .must(m -> m.term(t -> t.field("userId").value(userId)))
                                                 // ✅ question 필드 fuzzy 검색
                                                 .should(s -> s.match(mq -> mq
                                                                 .field("question")
                                                                 .query(keyword)
                                                                 .fuzziness("AUTO")   // 오타 허용
                                                                 .prefixLength(1)     // 첫 글자는 정확히
                                                                     ))
                                                 // ✅ answer 필드 fuzzy 검색
                                                 .should(s -> s.match(mq -> mq
                                                                 .field("answer")
                                                                 .query(keyword)
                                                                 .fuzziness("AUTO")
                                                                 .prefixLength(1)
                                                                     ))
                                                 .minimumShouldMatch("1") // ✅ 두 필드 중 최소 1개 일치해야 함
                                                       ))
                                 .withSort(Sort.by(Sort.Order.desc("_score")))
                                 .withPageable(pageable)
                                 .build();

        SearchHits<CardDocument> hits = elasticsearchOperations.search(query, CardDocument.class);
        List<Long> cardIds = hits.stream()
                                 .map(hit -> hit.getContent().getCardId())
                                 .toList();

        if (cardIds.isEmpty()) {
            return new SliceImpl<>(List.of(), pageable, false);
        }

        // 2️⃣ MySQL에서 실제 Card 조회
        List<Card> cards = cardRepository.findAllById(cardIds);

        // (선택) 검색 결과 순서 맞추기 (ES 스코어 순서 유지)
        Map<Long, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < cardIds.size(); i++) {
            orderMap.put(cardIds.get(i), i);
        }
        cards.sort(Comparator.comparingInt(c -> orderMap.getOrDefault(c.getId(), Integer.MAX_VALUE)));

        // 3️⃣ Card → DTO 변환
        List<CardSearchResponseDto> dtos = cards.stream()
                                                .map(card -> {
                                                    String thumbnailUrl = card.getImages().stream()
                                                                              .sorted(Comparator.comparing(CardImage::getSequence))
                                                                              .map(CardImage::getImageUrl)
                                                                              .findFirst()
                                                                              .orElse(null);

                                                    return CardSearchResponseDto.builder()
                                                                                .cardId(card.getId())
                                                                                .question(card.getQuestion())
                                                                                .answer(card.getAnswer())
                                                                                .thumbnailUrl(thumbnailUrl)
                                                                                .build();
                                                })
                                                 .toList();

        boolean hasNext = hits.getTotalHits() > (from + dtos.size());
        return new SliceImpl<>(dtos, pageable, hasNext);
    }

    // ✅ Document 등록 (Card 생성 시 자동 인덱싱용)
    public void indexCard(CardDocument doc) {
        cardSearchRepository.save(doc);
        log.info("[Elasticsearch] Indexed cardId={}, esId={}", doc.getCardId(), doc.getEsId());
    }

    // ✅ 삭제 시 동기화
    public void deleteIndex(Long cardId) {
        cardSearchRepository.deleteByCardId(cardId);
    }

}
