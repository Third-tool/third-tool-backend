package com.example.thirdtool.Card.domain.repository;


import com.example.thirdtool.Card.Document.CardDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardSearchRepository extends ElasticsearchRepository<CardDocument, String> {

    List<CardDocument> findByQuestionContainingOrAnswerContaining(String question, String answer);

    void deleteByCardId(Long cardId);

    Optional<CardDocument> findByCardId(Long cardId);
}