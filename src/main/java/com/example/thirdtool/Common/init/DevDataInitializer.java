package com.example.thirdtool.Common.init;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardImage;
import com.example.thirdtool.Card.domain.model.ImageType;
import com.example.thirdtool.Card.domain.repository.CardRepository;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile("local")           // ★ dev 프로파일에서만 실행
@Order(1)
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;

    // ===== 시드 규모 조절 상수 =====
    private static final int ROOT_DECKS_PER_USER   = 5;   // 유저당 상위 덱 개수
    private static final int CHILDREN_PER_ROOT     = 2;   // 상위 덱당 하위 덱 개수
    private static final int CARDS_PER_ROOT        = 6;   // 상위 덱당 카드 개수
    private static final int CARDS_PER_CHILD       = 4;   // 하위 덱당 카드 개수

    @Override
    @Transactional
    public void run(String... args) {
        // 이미 한 번이라도 유저가 있으면 스킵(안전장치)
        if (userRepository.count() > 0) {
            log.info("[DevSeed] existing data detected. skip.");
            return;
        }

        // 1) 유저 3명 생성
        List<UserEntity> users = List.of(
                UserEntity.ofLocal("testuser1", passwordEncoder.encode("pass1234!"), "테스트1", "u1@example.com"),
                UserEntity.ofLocal("testuser2", passwordEncoder.encode("pass1234!"), "테스트2", "u2@example.com"),
                UserEntity.ofLocal("testuser3", passwordEncoder.encode("pass1234!"), "테스트3", "u3@example.com")
                                        );
        users = userRepository.saveAll(users);
        log.info("[DevSeed] users created: {}", users.size());

        // 2) 주제 Pool (의미 있는 이름들). 유저별로 다른 조합을 순환 사용
        List<DeckSpec> topicPool = List.of(
                new DeckSpec("Java 기초", "LEITNER"),
                new DeckSpec("Spring Boot 핵심", "SM2"),
                new DeckSpec("알고리즘/자료구조", "LEITNER"),
                new DeckSpec("요리 - 한식 기초", "SM2"),
                new DeckSpec("스포츠 - 축구 상식", "LEITNER"),
                new DeckSpec("영어 - 기초 단어", "SM2"),
                new DeckSpec("역사 - 세계사 핵심연도", "LEITNER"),
                new DeckSpec("Java 컬렉션", "SM2"),
                new DeckSpec("요리 - 베이킹 기초", "LEITNER"),
                new DeckSpec("스포츠 - 농구 상식", "SM2")
                                          );

        int topicCursor = 0;
        int imgSeed = 100;

        for (UserEntity user : users) {
            List<Deck> roots = new ArrayList<>();
            for (int i = 0; i < ROOT_DECKS_PER_USER; i++) {
                DeckSpec spec = topicPool.get(topicCursor % topicPool.size());
                topicCursor++;

                // ✅ 중복 체크: 같은 유저에 같은 이름이 이미 있으면 skip
                if (deckRepository.existsByUserAndName(user, spec.name())) {
                    log.warn("[DevSeed] duplicate deck name={} for user={} skipped", spec.name(), user.getUsername());
                    continue;
                }

                Deck root = Deck.of(spec.name(), null, spec.algorithm(), user);
                roots.add(root);
            }
            deckRepository.saveAll(roots);

            // 하위 덱 생성 + 카드 생성 (상위/하위 모두)
            List<Deck> children = new ArrayList<>();
            for (Deck root : roots) {
                // 하위 덱 CHILDREN_PER_ROOT 개 (부모 알고리즘 그대로 사용)
                for (int c = 1; c <= CHILDREN_PER_ROOT; c++) {
                    Deck child = Deck.of(root.getName() + " - 서브" + c, root, root.getScoringAlgorithmType(), user);
                    children.add(child);
                }
            }
            deckRepository.saveAll(children);

            // 카드 만들기
            List<Card> cards = new ArrayList<>();

            // 상위 덱 카드
            for (Deck root : roots) {
                List<QAPair> pairs = pairsFor(root.getName());
                for (int i = 0; i < Math.min(CARDS_PER_ROOT, pairs.size()); i++) {
                    Card card = Card.of(pairs.get(i).q(), pairs.get(i).a(), root);
                    attachImages(card, imgSeed++);
                    cards.add(card);
                }
            }

            // 하위 덱 카드
            for (Deck child : children) {
                List<QAPair> pairs = pairsForChild(child.getName());
                for (int i = 0; i < Math.min(CARDS_PER_CHILD, pairs.size()); i++) {
                    Card card = Card.of(pairs.get(i).q(), pairs.get(i).a(), child);
                    attachImages(card, imgSeed++);
                    cards.add(card);
                }
            }

            cardRepository.saveAll(cards);
            log.info("[DevSeed] user {} done: roots={}, children={}, cards={}",
                    user.getUsername(), roots.size(), children.size(), cards.size());
        }

        log.info("[DevSeed] DONE ✅ (users=3)");
    }

    // ===== 이미지 부착 (QUESTION/ANSWER 1장씩) =====
    private void attachImages(Card card, int seed) {
        String qUrl = "https://picsum.photos/seed/q" + seed + "/600/400";
        String aUrl = "https://picsum.photos/seed/a" + seed + "/600/400";
        card.addImage(CardImage.of(card, qUrl, ImageType.QUESTION, 1));
        card.addImage(CardImage.of(card, aUrl, ImageType.ANSWER, 1));
    }

    // ===== Q/A 데이터: 상위 덱 전용 =====
    private List<QAPair> pairsFor(String deckName) {
        switch (deckName) {
            case "Java 기초": return List.of(
                    qa("JVM/JRE/JDK 차이?", "JVM은 실행환경, JRE는 JVM+라이브러리, JDK는 JRE+개발도구."),
                    qa("기본형/참조형?", "기본형은 값 저장, 참조형은 객체 참조 저장."),
                    qa("== vs equals?", "==는 참조 비교, equals는 내용 비교."),
                    qa("오버로딩/오버라이딩?", "오버로딩=시그니처 다름, 오버라이딩=상속 재정의."),
                    qa("final의 의미?", "상수/오버라이드 금지/상속 금지."),
                    qa("String이 불변인 이유?", "보안/캐싱/스레드안전/해시코드 유지.")
                                          );
            case "Spring Boot 핵심": return List.of(
                    qa("@Component vs @Bean", "자동 탐지 vs 수동 등록."),
                    qa("@Configuration", "프록시로 싱글톤 보장."),
                    qa("DI 방법", "생성자(권장)/세터/필드."),
                    qa("AOP 핵심", "관심사 분리: 어드바이스/포인트컷."),
                    qa("@Profile", "환경별 설정 분리."),
                    qa("Actuator", "헬스/메트릭/엔드포인트.")
                                                 );
            case "알고리즘/자료구조": return List.of(
                    qa("O(N log N) 예", "퀵/머지/힙 정렬 평균."),
                    qa("스택/큐", "LIFO vs FIFO."),
                    qa("해시 충돌 해결", "체이닝/오픈어드레싱."),
                    qa("트리/그래프", "트리는 비순환, 그래프 일반."),
                    qa("BFS/DFS", "레벨 우선/깊이 우선."),
                    qa("힙 특징", "완전이진트리 기반.")
                                            );
            case "요리 - 한식 기초": return List.of(
                    qa("육수 비율", "물:멸치:다시마 비율 조절."),
                    qa("간 맞추기", "소금/간장→설탕/식초 순."),
                    qa("지단 팁", "약불/체치기/식혀 접기."),
                    qa("잡채 면", "덜 삶고 팬에서 마무리."),
                    qa("된장/고추장 보관", "냉장/표면 랩/밀폐."),
                    qa("불고기 양념", "간장/설탕/배즙/마늘/참기름.")
                                             );
            case "스포츠 - 축구 상식": return List.of(
                    qa("오프사이드", "패스 순간 수비 뒤."),
                    qa("4-3-3 장점", "측면 전개/압박."),
                    qa("경고/퇴장", "거친 파울/방해."),
                    qa("VAR", "명백한 오심 최소화."),
                    qa("스루패스", "라인 사이 공간 찌르기."),
                    qa("하프스페이스", "측면과 중앙 사이.")
                                              );
            case "영어 - 기초 단어": return List.of(
                    qa("apple 뜻", "사과"), qa("book 뜻", "책"),
                    qa("city 뜻", "도시"), qa("family 뜻", "가족"),
                    qa("bread 뜻", "빵"), qa("water 뜻", "물")
                                             );
            case "역사 - 세계사 핵심연도": return List.of(
                    qa("서로마 멸망", "476년"),
                    qa("마그나카르타", "1215년"),
                    qa("백년전쟁", "1337~1453"),
                    qa("프랑스혁명", "1789"),
                    qa("미 독립선언", "1776"),
                    qa("UN 창설", "1945")
                                                );
            case "Java 컬렉션": return List.of(
                    qa("List/Set 차이", "순서/중복 vs 중복 불가."),
                    qa("HashMap/TreeMap", "O(1) vs O(logN) 정렬."),
                    qa("ArrayList/LinkedList", "랜덤접근 vs 삽입/삭제 강점."),
                    qa("HashSet 원리", "hashCode + equals."),
                    qa("ConcurrentHashMap", "버킷 동시성 제어."),
                    qa("Queue/Deque", "FIFO vs 양쪽 입출력.")
                                           );
            case "요리 - 베이킹 기초": return List.of(
                    qa("크리밍", "버터+설탕 공기 주입."),
                    qa("글루텐", "밀가루+물+반죽."),
                    qa("BP/BS 차이", "파우더 완제품/소다는 산 필요."),
                    qa("머랭 단계", "소프트/스티프 피크."),
                    qa("오븐 예열", "필수, 온도 안정화."),
                    qa("오버믹싱", "질겨짐 원인.")
                                              );
            case "스포츠 - 농구 상식": return List.of(
                    qa("픽앤롤", "스크린 후 롤/팝."),
                    qa("3점 라인", "국제 6.75m."),
                    qa("파울 기준", "FIBA 5, NBA 6."),
                    qa("트랜지션", "속공."),
                    qa("박스아웃", "뒤 공간 선점."),
                    qa("존 디펜스", "공간 수비.")
                                              );
            default:
                return genericPairs(deckName, CARDS_PER_ROOT);
        }
    }

    // ===== Q/A 데이터: 하위 덱 전용 =====
    private List<QAPair> pairsForChild(String deckName) {
        if (deckName.contains("서브")) {
            return List.of(
                    qa(deckName + " - Q1", deckName + " - A1"),
                    qa(deckName + " - Q2", deckName + " - A2"),
                    qa(deckName + " - Q3", deckName + " - A3"),
                    qa(deckName + " - Q4", deckName + " - A4"),
                    qa(deckName + " - Q5", deckName + " - A5") // 여분 1개(필요 시)
                          );
        }
        return genericPairs(deckName, CARDS_PER_CHILD);
    }

    private List<QAPair> genericPairs(String deckName, int n) {
        List<QAPair> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) list.add(qa(deckName + " Q" + i, deckName + " A" + i));
        return list;
    }

    private QAPair qa(String q, String a) { return new QAPair(q, a); }

    private record QAPair(String q, String a) {}
    private record DeckSpec(String name, String algorithm) {}
}