package com.example.thirdtool.Deck.domain.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Tag.domain.model.Tag;
import com.example.thirdtool.Tag.domain.repository.TagRepository;
import com.example.thirdtool.User.domain.model.User;
import com.example.thirdtool.User.domain.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Transactional
class DeckRepositoryTest {

    @Autowired
    private DeckRepository deckRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private UserRepository userRepository;

    private static final String DEFAULT_ALGORITHM = "sm2Algorithm";

    @Test
    void findByParentDeckIsNull() {
    }

    @Test
    void findTop5ByOrderByLastAccessedDesc() {
    }

    @Test
    void findByParentDeckId() {
    }

    /*@Test
    void findAllByTagName() {
        User user1 = userRepository.save(User.of("testuser", "password"));
        Tag tag1 = tagRepository.save(Tag.of("tag1",user1));
        Deck deck1 = Deck.of("deck_a",DEFAULT_ALGORITHM,user1);
        deck1.addTag(tag1);
        deckRepository.save(deck1);

        Tag tag2 = tagRepository.save(Tag.of("tag2",user1));
        Deck deck2 = Deck.of("deck_b",DEFAULT_ALGORITHM,user1);
        deck2.addTag(tag2);
        deckRepository.save(deck2);

        deckRepository.flush();

        List<Deck> foundDecks = deckRepository.findAllByUserIdAndTagNameKey(user1.getId(), "tag1");
        assertEquals(1, foundDecks.size());
    }*/

    @Test
    void updateLastAccessed() {
    }
}