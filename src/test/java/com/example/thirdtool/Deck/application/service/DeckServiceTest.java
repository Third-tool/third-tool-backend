package com.example.thirdtool.Deck.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import com.example.thirdtool.Deck.presentation.dto.DeckResponseDto;
import com.example.thirdtool.Deck.presentation.dto.DeckSearchDto;
import com.example.thirdtool.Scoring.aapplication.service.CardAlgorithmService;
import com.example.thirdtool.Scoring.domain.model.Sm2Algorithm;
import com.example.thirdtool.Tag.domain.model.Tag;
import com.example.thirdtool.Tag.domain.repository.TagRepository;
import com.example.thirdtool.User.domain.model.User;
import com.example.thirdtool.User.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import javax.swing.text.html.parser.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
@SpringBootTest
@Transactional
class DeckServiceTest {

    @Autowired
    private DeckRepository deckRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private DeckService deckService;
    @Autowired
    private EntityManager em;

    private static final String DEFAULT_ALGORITHM = "sm2Algorithm";

    @Test
    void findDecksByName() {
        User user1 = userRepository.save(User.of("qwer@naver.com","12345678"));
        Deck deck1 = deckRepository.save(Deck.of("Java", DEFAULT_ALGORITHM, user1));
        Deck deck2 = deckRepository.save(Deck.of("Math", DEFAULT_ALGORITHM, user1));

        em.flush();
        em.clear();

        List<DeckResponseDto> foundDeck = deckService.findDecksByName(user1.getId(), "Java");

        assertThat(foundDeck).hasSize(1);
        assertThat(foundDeck.get(0).name()).isEqualTo("Java");
    }

    @Test
    void findDecksByTagId() {
        User user1 = userRepository.save(User.of("qwer@naver.com","12345678"));
        Tag tag1 = tagRepository.save(Tag.of("Java", user1)) ;
        Tag tag2 = tagRepository.save(Tag.of("Math", user1)) ;
        Deck deck1 = deckRepository.save(Deck.of("Java Programming", DEFAULT_ALGORITHM, user1));
        Deck deck2 = deckRepository.save(Deck.of("Math Logic", DEFAULT_ALGORITHM, user1));
        deck1.addTag(tag1);
        deck2.addTag(tag2);

        List<DeckResponseDto> foundDeck = deckService.findDecksByTagId(user1.getId(),
            List.of(tag1.getId()));
        assertThat(foundDeck).hasSize(1);
        assertThat(foundDeck.get(0).name()).isEqualTo("Java Programming");
    }

    @Test
    void getAutocompleteDeckNames_PublicDeck() {
        User user1 = userRepository.save(User.of("qwer@naver.com","12345678"));
        User user2 = userRepository.save(User.of("asdf@naver.com","12345678"));

        Deck publicDeck = deckRepository.save(Deck.of("Java Programming",DEFAULT_ALGORITHM, user1));
        Deck deck2 = deckRepository.save(Deck.of("Math Logic",DEFAULT_ALGORITHM, user2));

        publicDeck.setShared();

        Deck otherPublicDeck = deckRepository.save(Deck.of("C programming",DEFAULT_ALGORITHM,user1));

        List<DeckSearchDto> suggestions1 = deckService.getAutocompleteDeckNames("Java","public", user1.getId());

        List<DeckSearchDto> suggestions2 = deckService.getAutocompleteDeckNames("C","public", user1.getId());

        List<DeckSearchDto> suggestions3 = deckService.getAutocompleteDeckNames("Math","private", user2.getId());

        assertThat(suggestions1.get(0).name()).isEqualTo(publicDeck.getName());
        assertThat(suggestions2).isEmpty();
        assertThat(suggestions3.get(0).name()).isEqualTo(deck2.getName());
    }

    @Test
    void archiveDeck() {
        User user1 = userRepository.save(User.of("qwer@naver.com","12345678"));
        Deck deck = deckRepository.save(Deck.of("Java Programming",DEFAULT_ALGORITHM,user1));
        deck.setShared();
        assertThat(deck.isShared()).isTrue();

    }

    @Test
    void unArchiveDeck() {
        User user1 = userRepository.save(User.of("qwer@naver.com","12345678"));
        Deck deck = deckRepository.save(Deck.of("Java Programming",DEFAULT_ALGORITHM,user1));
        deck.setUnshared();
        assertThat(deck.isShared()).isFalse();
    }

    @Test
    void copyDeckToUser() {
        User user1 = userRepository.save(User.of("qwer@naver.com","12345678"));
        User user2 = userRepository.save(User.of("asdfg@naver.com","12345678"));

        Deck deck1 = deckRepository.save(Deck.of("Java Programming",DEFAULT_ALGORITHM,user1));
        deck1.setShared();
        Deck archivedDeck = deckService.copyDeckToUser(deck1.getId(),user2.getId());
        assertThat(archivedDeck.getUser()).isEqualTo(user2);
        assertThat(deck1.getUser()).isEqualTo(user1);
    }

    @Test
    void getAllArchivedDecks() {
        User user = userRepository.save(User.of("qwer@naver.com","12345678"));
        Deck deck1 = deckRepository.save(Deck.of("Java Programming",DEFAULT_ALGORITHM,user));
        Deck deck2 = deckRepository.save(Deck.of("Math",DEFAULT_ALGORITHM,user));
        Deck deck3 = deckRepository.save(Deck.of("Science",DEFAULT_ALGORITHM,user));

        deck1.setShared();
        deck2.setShared();
        List<Deck> sharedDecks = deckService.getAllArchivedDecks();

        assertThat(sharedDecks).hasSize(2);
        assertThat(sharedDecks.get(0)).isEqualTo(deck1);
        assertThat(sharedDecks.get(1)).isEqualTo(deck2);
    }
}