package com.example.thirdtool.Common.init;

import com.example.thirdtool.LegacyCard.Card.domain.model.Card;
import com.example.thirdtool.LegacyCard.Card.domain.repository.CardRepository;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Profile("dev") // ✅ EC2(dev 환경)에서만 실행
@Order(2)
@RequiredArgsConstructor
public class ProdDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 이미 데이터 있으면 skip
        if (userRepository.count() > 0) {
            log.info("[ProdSeed] existing data detected. skip.");
            return;
        }

        log.info("[ProdSeed] 🚀 Initializing production seed data...");

        // 1️⃣ 운영 관리자 계정 생성
        UserEntity admin = UserEntity.ofLocal(
                "admin",
                passwordEncoder.encode("admin1234!"),
                "운영관리자",
                "admin@thethirdtool.com"
                                             );
        userRepository.save(admin);
        log.info("[ProdSeed] Admin user created.");

        // 2️⃣ 대표 덱 생성
        Deck mainDeck = deckRepository.save(
                Deck.of("운영용 기본 덱", null, "LEITNER", admin)
                                           );
        log.info("[ProdSeed] Main deck created.");

        // 3️⃣ 기본 카드 몇 개 등록
        List<Card> cards = List.of(
                Card.of("The Third Tool이란?", "기억을 확장하는 학습 보조 도구입니다.", mainDeck),
                Card.of("핵심 가치 3가지는?", "꿈, 희망, 낭만.", mainDeck),
                Card.of("이 프로젝트의 목표는?", "감정을 움직이는 학습 경험 제공.", mainDeck)
                                  );
        cardRepository.saveAll(cards);
        log.info("[ProdSeed] Default cards created: {}", cards.size());

        log.info("[ProdSeed] ✅ 운영용 초기 데이터 등록 완료");
    }
}
