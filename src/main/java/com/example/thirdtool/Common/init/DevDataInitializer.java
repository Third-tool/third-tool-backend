package com.example.thirdtool.Common.init;

import com.example.thirdtool.LegacyCard.Card.application.service.CardService;
import com.example.thirdtool.LegacyCard.Card.domain.model.Card;
import com.example.thirdtool.LegacyCard.Card.presentation.dto.WriteCardDto;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

@Slf4j
@Component
@Profile("local")
@Order(1)
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final CardService cardService;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.s3-base-url}")
    private String s3BaseUrl;

    /** 🔹 덱당 카드 20장 (테스트용) */
    private static final int CARDS_PER_DECK = 20;

    @Override
    public void run(String... args) throws Exception {
        log.info("==============================================");
        log.info("🚀 [DevSeed] 테스트용 시드 데이터 생성 시작 (2덱 × 20장)");
        log.info("==============================================");

        if (userRepository.count() > 0) {
            log.info("[DevSeed] 기존 데이터 감지됨 → 시드 생성 스킵");
            return;
        }

        // 1) 유저 생성 (testuser1, testuser2)
        List<UserEntity> users = createUsers();
        userRepository.saveAll(users);

        UserEntity testUser1 = users.stream()
                                    .filter(u -> "testuser1".equals(u.getUsername()))
                                    .findFirst()
                                    .orElse(users.get(0));

        // 2) testuser1에게만 테스트용 덱 2개 생성
        List<Deck> decks = List.of(
                Deck.of("백엔드 CS 지식", null, "LEITNER", testUser1),
                Deck.of("스프링 지식", null, "SM2", testUser1)
                                  );
        deckRepository.saveAll(decks);

        // 3) 각 덱당 20장씩 카드 생성
        for (Deck deck : decks) {
            createBulkCards(testUser1, deck, CARDS_PER_DECK, deck.getName());
        }

        log.info("==============================================");
        log.info("✅ [DevSeed] 테스트용 시드 생성 완료");
        log.info("==============================================");
    }

    private List<UserEntity> createUsers() {
        return List.of(
                UserEntity.ofLocal("testuser1", passwordEncoder.encode("pass1234!"), "테스트1", "u1@example.com"),
                UserEntity.ofLocal("testuser2", passwordEncoder.encode("pass1234!"), "테스트2", "u2@example.com")
                      );
    }

    /** 덱당 카드 N장 생성 (이미지는 S3에서 바로 읽기) */
    private void createBulkCards(UserEntity user, Deck deck, int count, String topic) {
        log.info("[DevSeed] ▶ 덱 '{}' → {}장 카드 생성 중...", deck.getName(), count);

        for (int i = 1; i <= count; i++) {
            try {
                String question = makeRealisticQuestion(topic, i);
                String answer = makeRealisticAnswer(topic, i);

                MultipartFile qFile = fromS3Seed("question", i); // q1~q10
                MultipartFile aFile = fromS3Seed("answer", i);   // a1~a10

                WriteCardDto dto = new WriteCardDto(
                        question,
                        answer,
                        List.of(qFile),
                        List.of(aFile)
                );

                // ⭐ 카드 생성 (Card 반환형이라고 가정)
                Card card = cardService.createCard(deck.getId(), dto);

                log.info("[DevSeed] ✅ 카드 생성 완료 - deck='{}', idx={}, cardId={}",
                        deck.getName(), i, card.getId());

            } catch (Exception e) {
                // 개별 실패는 로그만 남기고 계속 진행
                log.warn("[DevSeed] ⚠️ 카드 생성 실패 (deck='{}', idx={}): {}", deck.getName(), i, e.getMessage());
            }
        }

        log.info("[DevSeed] ✅ 덱 '{}' 카드 생성 완료", deck.getName());
    }

    /** 🔹 S3에서 q1~q10, a1~a10을 읽어 MockMultipartFile로 변환 (jpg/png 둘 다 시도) */
    private MultipartFile fromS3Seed(String folder, int index) throws IOException {
        int normalizedIndex = ((index - 1) % 10) + 1; // 1~10 반복
        char prefix = folder.equals("question") ? 'q' : 'a';

        String[] exts = {"jpg", "png", "jpeg"};

        for (String ext : exts) {
            String fileUrl = String.format("%s/%s/%c%d.%s",
                    s3BaseUrl, folder, prefix, normalizedIndex, ext);

            try (InputStream in = new URL(fileUrl).openStream()) {
                byte[] bytes = in.readAllBytes();
                log.debug("[DevSeed] S3 이미지 로드 성공: {}", fileUrl);
                return new MockMultipartFile(
                        folder,
                        prefix + normalizedIndex + "." + ext,
                        "image/" + ext,
                        bytes
                );
            } catch (Exception e) {
                // 다음 확장자 시도
                log.debug("[DevSeed] S3 이미지 로드 실패, 다음 확장자 시도: {} ({})", fileUrl, e.getMessage());
            }
        }

        // 전부 실패한 경우: 빈 더미 파일 반환 (이미지 없는 카드)
        log.warn("[DevSeed] ⚠️ S3 이미지 찾기 실패 → dummy 이미지 사용, folder={}, index={}", folder, index);
        return new MockMultipartFile(folder, "dummy.png", "image/png", new byte[0]);
    }

    // ======================================================
    // 🔹 주제별 realistic 질문/답변 (각 20개, 전부 다른 내용)
    // ======================================================

    private String makeRealisticQuestion(String topic, int index) {
        if ("백엔드 CS 지식".equals(topic)) {
            return BACKEND_CS_Q[index - 1];
        }
        if ("스프링 지식".equals(topic)) {
            return SPRING_Q[index - 1];
        }
        return topic + " 관련 질문 " + index;
    }

    private String makeRealisticAnswer(String topic, int index) {
        if ("백엔드 CS 지식".equals(topic)) {
            return BACKEND_CS_A[index - 1];
        }
        if ("스프링 지식".equals(topic)) {
            return SPRING_A[index - 1];
        }
        return topic + " 관련 답변 " + index;
    }

    // ───────── 백엔드 CS 20문항 ─────────
    private static final String[] BACKEND_CS_Q = {
            "HTTP 1.1과 HTTP 2.0의 가장 큰 차이는 무엇인가요?",
            "TCP 3-way handshake는 왜 필요한가요?",
            "프로세스와 스레드의 차이를 설명하세요.",
            "캐시 메모리가 프로그램 실행 속도를 높이는 원리는?",
            "DB 인덱스가 빠른 이유는 무엇인가요?",
            "교착상태(Deadlock)가 발생하기 위한 조건 4가지는?",
            "트랜잭션의 ACID는 각각 무엇을 의미하나요?",
            "메시지 큐를 사용하면 확장성이 좋아지는 이유는?",
            "REST API의 자원(Resource)을 설계하는 기준은?",
            "CPU 스케줄링의 Round-Robin 방식은 어떻게 동작하나요?",
            "세마포어와 뮤텍스의 차이는 무엇인가요?",
            "DNS 조회 과정은 어떻게 이루어지나요?",
            "가상 메모리(Virtual Memory)가 필요한 이유는?",
            "Load Balancer가 필요한 이유는 무엇인가요?",
            "HTTP 상태코드 301과 302의 차이는?",
            "JWT가 세션 방식과 다른 점은 무엇인가요?",
            "파일 시스템의 inode는 어떤 역할을 하나요?",
            "Redis가 빠른 핵심 이유는 무엇인가요?",
            "ORM을 사용했을 때 장점은 무엇인가요?",
            "CAP 이론의 세 가지 요소는 무엇인가요?"
    };

    private static final String[] BACKEND_CS_A = {
            "HTTP/2는 멀티플렉싱을 지원해 하나의 연결로 여러 요청을 병렬 처리합니다.",
            "연결 신뢰성을 보장하고, 양쪽이 통신 준비가 되었는지 확인하기 위해서입니다.",
            "프로세스는 독립된 메모리 공간을 가지지만, 스레드는 프로세스 내 자원을 공유합니다.",
            "CPU보다 빠른 SRAM에 자주 쓰는 데이터를 저장해 메모리 병목을 줄입니다.",
            "B-Tree 기반으로 정렬되어 있어 탐색 시간이 O(log N)으로 줄어듭니다.",
            "상호 배제, 점유와 대기, 비선점, 순환 대기의 네 가지 조건이 동시에 만족될 때 발생합니다.",
            "원자성, 일관성, 고립성, 지속성을 보장해 데이터 무결성을 유지합니다.",
            "생산자와 소비자를 느슨하게 연결해 피크 트래픽을 흡수할 수 있습니다.",
            "리소스를 명사형으로 표현하고, 상태는 HTTP 메서드와 코드로 표현합니다.",
            "각 프로세스에 일정 시간만큼 CPU를 할당하며 순환시키는 방식입니다.",
            "뮤텍스는 하나만 들어갈 수 있는 잠금, 세마포어는 카운터 기반 동기화 도구입니다.",
            "클라이언트는 로컬/리커서브 DNS를 통해 루트 → TLD → 권한 DNS 순서로 조회합니다.",
            "물리 메모리보다 큰 주소 공간을 제공해 여러 프로그램을 동시에 실행할 수 있게 합니다.",
            "트래픽을 여러 서버로 분산해 가용성과 응답 속도를 높입니다.",
            "301은 영구 이동, 302는 임시 이동을 의미합니다.",
            "JWT는 토큰 자체에 정보를 담고 있어 서버 세션 저장소가 필요 없습니다.",
            "inode는 파일의 메타 정보와 디스크 블록 위치를 담고 있는 구조체입니다.",
            "메모리 기반 저장소이고, 단일 스레드 이벤트 루프 구조로 오버헤드가 적습니다.",
            "객체 모델과 데이터베이스 간 매핑을 자동화해 생산성과 유지보수성을 높입니다.",
            "일관성, 가용성, 파티션 내성 중 둘만 완벽히 만족할 수 있다는 분산 시스템 이론입니다."
    };

    // ───────── 스프링 지식 20문항 ─────────
    private static final String[] SPRING_Q = {
            "Spring의 IoC 컨테이너는 어떤 역할을 하나요?",
            "DI(의존성 주입)의 장점은 무엇인가요?",
            "AOP가 필요한 이유는 무엇인가요?",
            "Spring Bean의 대표적인 스코프는 무엇인가요?",
            "Filter와 Interceptor의 차이는 무엇인가요?",
            "DispatcherServlet은 어떤 패턴을 구현한 컴포넌트인가요?",
            "BeanFactory와 ApplicationContext의 차이는 무엇인가요?",
            "Proxy 기반 AOP는 어떤 방식으로 동작하나요?",
            "Spring Boot AutoConfiguration은 어떤 기준으로 Bean을 등록하나요?",
            "@RestController와 @Controller의 차이는 무엇인가요?",
            "트랜잭션 전파(Transaction Propagation) 옵션은 왜 필요한가요?",
            "JPA의 영속성 컨텍스트는 어떤 이점을 주나요?",
            "Lazy Loading이 실패하는 대표적인 상황은 무엇인가요?",
            "EntityManager는 어떤 책임을 가지나요?",
            "Spring Security에서 Authentication 객체는 어디에 저장되나요?",
            "OAuth2 로그인에서 Authorization Code는 어떤 역할을 하나요?",
            "MessageConverter는 어떤 일을 담당하나요?",
            "CORS 설정은 왜 필요한가요?",
            "RestTemplate과 WebClient의 가장 큰 차이는 무엇인가요?",
            "Spring의 ApplicationEventPublisher는 언제 유용하게 사용할 수 있나요?"
    };

    private static final String[] SPRING_A = {
            "객체 생성과 생명주기 관리를 맡고, 의존성 주입을 통해 빈들을 연결합니다.",
            "결합도를 낮추고 테스트와 확장을 쉽게 만들어줍니다.",
            "로그, 트랜잭션, 보안처럼 여러 계층에 흩어진 공통 로직을 모듈화하기 위해서입니다.",
            "Singleton, Prototype, Request, Session 등이 대표적인 스코프입니다.",
            "Filter는 Servlet 앞단, Interceptor는 Spring MVC Handler 앞단에서 동작합니다.",
            "Front Controller 패턴을 구현해 모든 요청을 한 지점에서 받아 분기합니다.",
            "ApplicationContext는 BeanFactory 기능 + 메시지소스, 이벤트 등 부가 기능을 제공합니다.",
            "JDK Dynamic Proxy나 CGLIB으로 프록시 객체를 생성하여 메서드 호출을 가로챕니다.",
            "클래스패스의 의존성을 스캔해서 조건에 맞는 자동 설정 클래스를 활성화합니다.",
            "@RestController는 @ResponseBody가 포함되어 JSON 바디를 바로 반환합니다.",
            "기존 트랜잭션에 참여할지, 새로 열지, 예외 시 어떻게 처리할지 제어하기 위해 필요합니다.",
            "엔티티 변경 감지와 1차 캐시를 제공하여 성능과 일관성을 높입니다.",
            "영속성 컨텍스트가 닫힌 후 프록시를 접근할 때 LazyInitializationException이 발생합니다.",
            "엔티티의 저장, 조회, 변경 감지, 플러시 등을 담당합니다.",
            "SecurityContextHolder에 저장되어 현재 인증된 사용자를 어디서든 조회할 수 있습니다.",
            "클라이언트가 액세스 토큰을 직접 받지 않고 서버 간에 안전하게 교환할 수 있게 합니다.",
            "HTTP 요청/응답 바디를 자바 객체와 JSON/문자열 등으로 변환합니다.",
            "브라우저의 동일 출처 정책 때문에 다른 도메인 간 요청이 차단되는 것을 허용하기 위해서입니다.",
            "RestTemplate는 동기 블로킹, WebClient는 비동기 논블로킹 방식입니다.",
            "도메인 이벤트 기반으로 계층 간 결합도를 낮추고 확장 포인트를 만들 때 유용합니다."
    };
}
