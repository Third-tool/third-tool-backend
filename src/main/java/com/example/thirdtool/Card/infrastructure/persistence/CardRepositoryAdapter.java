package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Card;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CardRepositoryAdapter implements CardRepository {

    private final CardJpaRepository cardJpaRepository;

    @Override
    public Card save(Card card) {
        return cardJpaRepository.save(card);
    }

    /**
     * ID로 카드를 조회한다.
     *
     * <p>KeywordCue가 필요한 경우 {@link CardJpaRepository#findByIdWithKeywords(Long)}을
     * 직접 호출하는 서비스를 별도로 두거나,
     * 이 메서드에서 페치 조인 버전으로 교체한다.
     */
    @Override
    public Optional<Card> findById(Long id) {
        return cardJpaRepository.findByIdWithKeywords(id);
    }

    @Override
    public void delete(Card card) {
        cardJpaRepository.delete(card);
    }
}