package com.example.thirdtool.Stats.application.service;


import static org.assertj.core.api.Assertions.assertThat;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.repository.CardRepository;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import com.example.thirdtool.Stats.domain.model.UserCardEntity;
import com.example.thirdtool.Stats.domain.repository.UserCardProgressRepository;
import com.example.thirdtool.Stats.presentation.dto.TagStudyCountDto;
import com.example.thirdtool.Tag.domain.model.Tag;
import com.example.thirdtool.Tag.domain.repository.TagRepository;

import com.example.thirdtool.User.domain.model.User;
import com.example.thirdtool.User.domain.repository.UserRepository;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StatsServiceTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StatsService statsService;
    @Autowired
    private UserCardProgressRepository ucr;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private DeckRepository deckRepository;

    private static final String DEFAULT_ALGORITHM = "sm2Algorithm";
    @Autowired
    private CardRepository cardRepository;

    @Test
    void getThisMonthTagRatio() {
        User user = userRepository.save(User.of("qwer@naver.com","12345678"));
        Tag tag1 = tagRepository.save(Tag.of("Java",user));
        Tag tag2 = tagRepository.save(Tag.of("Math",user));
        Deck deck1 = deckRepository.save(Deck.of("Java programming",DEFAULT_ALGORITHM,user));
        Deck deck2 = deckRepository.save(Deck.of("University Math",DEFAULT_ALGORITHM,user));
        deck1.addTag(tag1);
        deck2.addTag(tag2);

        Card card1 = cardRepository.save(Card.of("what is this","apple",deck1));
        Card card2 = cardRepository.save(Card.of("what is this?","banana",deck2));

        saveProgress(user,card1,true);
        saveProgress(user,card2,false);

        List<TagStudyCountDto> result = statsService.getThisMonthTagRatio(user.getId());

        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).name()).isEqualTo("java");
        assertThat(result.get(0).studyCount()).isEqualTo(1);

    }

    @Test
    @DisplayName("이번 달 학습 기록이 없으면 빈 리스트를 반환해야 한다")
    void getThisMonthTagRatio_NoRecord() {
        // given
        User user = userRepository.save(User.of("qwer@naver.com", "12345678"));

        // when
        List<TagStudyCountDto> result = statsService.getThisMonthTagRatio(user.getId());

        // then
        assertThat(result).isEmpty();
    }

    // 학습 기록 저장을 위한 헬퍼(Helper) 메소드
    private void saveProgress(User user, Card card, boolean isCorrect) {
        UserCardEntity progress = UserCardEntity.of(user, card);
        progress.recordAnswer(isCorrect);
        ucr.save(progress);
    }

}