# [Fix] 로컬 H2 데이터가 화면 이동 시 사라지는 현상 차단 (2026-06-30)

## 대상 (원본)

- 원본 PR / Story: N/A (운영 코드 fix 아님 — 로컬 dev 환경 트러블슈팅)
- 본 fix 발견 시점: 2026-06-30 (M1 PR-A~PR-E 머지 직후 로컬 통합 테스트 중)
- 영향 받는 메서드 / 클래스: 없음 (application-local.yml 설정 변경)
- 관련 설정 파일: `src/main/resources/application-local.yml`, `src/main/resources/application-dev.yml:17`

## 트리거

로컬에서 `./gradlew bootRun` (또는 IDE Run) 실행 후 사용자가 **덱을 생성하고 다른 화면으로 이동하면 방금 만든 덱이 사라짐**. H2 인메모리 DB라 휘발성이긴 하지만 단일 JVM 세션 내에서 데이터가 사라지면 안 됨. 동시에 부팅 로그에서 약 30초마다 `Restarting due to 378 class path changes` 라인이 반복 출력되는 패턴 관찰.

## 근본 원인 (분석)

두 설정의 의도하지 않은 조합이 만든 무한 데이터 소실 루프:

1. **`application-dev.yml:17` — `ddl-auto: create-drop`** — Spring 컨텍스트 시작 시 모든 테이블 drop + 재생성, 종료 시 drop. H2 인메모리에 적합한 dev 기본값이지만 컨텍스트 재시작에 매우 민감.
2. **DevTools restart 무한 루프** — `build.gradle:44` `developmentOnly 'org.springframework.boot:spring-boot-devtools'` + `application-local.yml`에 restart 제어 0건 = 기본값(restart 활성, poll-interval 1s). IntelliJ "Build project automatically" + Gradle delegation 조합이 30초마다 `build/classes/java/main`을 전체 재생성 → DevTools가 "378 additions" 감지 → 강제 restart.
3. **두 조합의 효과**: 30초마다 컨텍스트가 재시작되며 `create-drop`로 모든 테이블이 drop·재생성됨. 사용자가 만든 덱은 다음 restart 직후 사라짐.

로그 결정적 단서:
- `restartedMain` 스레드 + `LocalDevToolsAutoConfiguration$RestartingClassPathChangeChangedEventListener` 로거 = DevTools
- `378 additions, 0 deletions, 0 modifications` = 백그라운드 빌드가 전체 클래스 파일을 새로 쓴 흔적
- shutdown 시 `HikariDataSource - HikariPool-2 - Shutdown completed` → 다음 사이클 `Starting ThirdToolApplication` 즉시 재시작

## 변경 내역

### 코드 (설정)

- `src/main/resources/application-local.yml` — `spring` 트리 하위에 `devtools.restart.enabled: false` 신규 (5줄 추가, 주석 4줄 포함). DevTools restart 완전 비활성으로 30초 루프 차단. dev/prod profile에는 영향 없음 (`developmentOnly` scope라 운영 jar에 DevTools 자체 미포함).

### AC 보강

- N/A (운영 AC 아님)

## 검증

검증은 로컬 환경에서 수동 실행:

1. `./gradlew --stop` → `./gradlew bootRun` 신규 실행
2. `Started ThirdToolApplication in N seconds` 라인 1회만 출력 확인
3. **2분 무동작 대기** → `Restarting due to ... class path changes` 라인이 다시 출력되지 않음 확인 (1차 합격선)
4. H2 콘솔 접속 (`http://localhost:8080/h2-console` · JDBC URL `jdbc:h2:mem:thirdtool` · user/pwd `sa`/`sa`) → `SHOW TABLES;` → 도메인 테이블 존재 확인
5. 앱에서 덱 1건 생성 → H2 콘솔 `SELECT * FROM deck;` → 1건 보임
6. 다른 화면 이동 후 다시 `SELECT * FROM deck;` → **여전히 1건 보이면 fix 성공** (2차 합격선)

## 2차 진단 — tx 관련 (1차 합격선 통과 후에도 데이터 누락 시 적용)

DevTools restart 차단 후에도 데이터가 사라지면 트랜잭션 측 의심:

### 의심 항목

| # | 의심 | 확인 방법 |
| --- | --- | --- |
| T1 | Service 메서드에 `@Transactional` 누락 → Hibernate flush·commit 안 됨 | Service 클래스에 `@Transactional` (또는 read-only 메서드면 `@Transactional(readOnly = true)`) 있는지 확인. CommandService는 클래스 또는 메서드 단위 `@Transactional` 필수 |
| T2 | `@Transactional(readOnly = true)`가 commit 차단 | QueryService가 아닌데 readOnly=true이면 INSERT/UPDATE는 commit 안 됨. 클래스 어노테이션 상속 시 메서드에서 명시 override 필요 |
| T3 | tx 커밋 로그 부재 | `logback-spring.xml`에 `org.springframework.orm.jpa.JpaTransactionManager` DEBUG 임시 활성 → "Initiating transaction commit" / "Committing JPA transaction" 라인 보이는지 |
| T4 | 다른 JVM 인스턴스가 떠 있음 — 각자 자기 인메모리 H2 (공유 안 됨) | `jps -l` 또는 `tasklist | findstr java`로 `ThirdToolApplication` 프로세스가 정확히 1개인지 확인. 2개면 어느 JVM이 H2 콘솔에 붙어 있는지 모호 — 모두 kill 후 단일 재시작 |
| T5 | H2 콘솔의 JDBC URL이 `jdbc:h2:mem:thirdtool`가 아닌 기본값 `jdbc:h2:~/test` | 콘솔 첫 화면 JDBC URL 필드 정확히 매칭 확인. 다른 URL이면 별도 DB 인스턴스를 조회 중 |
| T6 | REST 호출이 다른 endpoint로 가는 중 (URL 오타 / FE 환경변수 불일치) | 브라우저 DevTools Network 탭 → POST /decks 응답 201 + Location 헤더 정상인지 |
| T7 | Hibernate 1st level cache는 채워졌지만 DB flush 시점 누락 | `application-local.yml`에 `spring.jpa.properties.hibernate.show_sql: true` (이미 활성) → INSERT 로그 보이는지. 안 보이면 flush 안 됨 |

### 의심 확정 시 fix 분기

- T1·T2 확정 → `fix/one-line-spec/...`로 트랜잭션 어노테이션 추가 fix 분기 (단순 한 줄 수정)
- T3·T7 확정 → 도메인 행위·flush 타이밍 재검토 (`fix/pes/...` 잠재)
- T4·T5·T6은 환경/조작 오류 — fix 코드 변경 불요, 운영 가이드 추가만

## 영향

- 원본 명세 갱신 필요? — N/A (운영 코드 변경 없음. 로컬 dev 환경 설정만)
- 원본 ADR — N/A (DevTools 의존은 build.gradle에 이미 존재. 본 변경은 restart 동작만 제어)
- 다른 Story / BC — N/A
- dev profile (CI / 외부 dev 서버) — 무영향 (`developmentOnly` scope라 운영 jar에 DevTools 미포함)
- prod profile — 무영향 (동일)

## 향후 재발 시 권장 후속 조치

1차 config 1줄 적용 후에도 무한 restart가 재발하면:

1. **IntelliJ "Build project automatically" 체크 해제** — Settings → Build, Execution, Deployment → Compiler
2. **gradle daemon 정리** — `./gradlew --stop` → bootRun 단독 재기동
3. **gradle continuous 잔존 확인** — `tasklist | findstr java` 또는 `jps -l`
4. **IDE Run "On 'Update' action"** — Run/Debug Configuration → Spring Boot 설정 → **Do nothing**

위 4단계는 본 fix와 별개로 "백그라운드 빌드 출처 자체를 제거" 하는 본질 해결책. DevTools를 다시 켜고 싶을 때 (예: trigger-file 패턴) 함께 적용 필요.

## 3차 발견 — QueryDSL Q클래스 `NoClassDefFoundError` (2026-06-30 19:49)

DevTools restart 차단 직후 fresh bootRun 시도 시 다음 에러 발생:

```
Caused by: java.lang.NoClassDefFoundError: com/example/thirdtool/Card/domain/model/QCard
    at com.example.thirdtool.Card.infrastructure.persistence.CardJpaRepositoryImpl.<clinit>(CardJpaRepositoryImpl.java:26)
Caused by: java.lang.ClassNotFoundException: com.example.thirdtool.Card.domain.model.QCard
    at org.springframework.boot.devtools.restart.classloader.RestartClassLoader.loadClass(RestartClassLoader.java:121)
```

### 원인 (재현 추정)

- `CardJpaRepositoryImpl.java:26`의 `private static final QCard card = QCard.card;` static 초기화 시점에 `QCard.class` 로드 시도 → 못 찾음
- 사후 디스크 확인 시 `build/classes/java/main/.../QCard.class` 존재 → **에러 발생 당시 classpath race**가 원인 (QCard 생성·컴파일 미완료 상태에서 부팅 시작)
- jps 확인 결과 `GradleDaemon` + `IntelliJ` + `VSCode GradleServer ×2` + `Spring Boot LSP` 동시 실행 — 여러 빌드 도구가 build/ 디렉토리를 서로 다른 시점에 건드림
- CLAUDE.md 알려진 quirk: "도메인 클래스 추가/이동 직후 IDE에서 Q클래스가 보이지 않으면 한 번 컴파일" — 동일 패턴

### 즉시 해결

```bash
./gradlew --stop          # 기존 데몬 + orphan JVM 정리
./gradlew clean           # build/ + src/main/generated/ (Q클래스) 둘 다 삭제
./gradlew compileJava     # annotation processor 재실행 → Q클래스 재생성 + 컴파일
# BUILD SUCCESSFUL 확인 후
./gradlew bootRun         # 부팅
```

### 재발 방지

- 동시 IDE 최소화 — IntelliJ + VSCode 동시 실행 금지 (Spring Boot LSP가 백그라운드 컴파일 시도하면 race)
- IntelliJ Run 사용 시 Run/Debug Configuration의 "Before launch"에 **단일 빌드 task만** (gradle delegation OR IDE build — 둘 중 하나, 동시 X)
- 도메인 클래스 새로 추가하거나 이동했을 때는 **`./gradlew compileJava` 1회 명시 실행** 후 bootRun
- Q클래스 누락 의심 시 빠른 확인: `find build/classes -name "Q*.class" | wc -l` (정상이면 도메인 entity 수와 일치)

### 영향

- 운영 코드 변경 X — 환경/실행 절차 가이드만
- 원본 ADR / DOMAIN.md 갱신 불요 (CLAUDE.md "QueryDSL Q클래스" 항목에 이미 명시)
- 단, 본 항목을 ts (운영 트러블슈팅) docs에 따로 등록할지 검토 가치 있음 — `docs/operations/troubleshooting/ts-local-dev-quirks.md` (잠재 후속)

## 참조

- 분석 대화: 2026-06-30 로컬 H2 데이터 소실 트러블슈팅 세션
- application-local.yml DevTools 비활성 설정 적용 commit (별도)
- 관련 설정: `src/main/resources/application-dev.yml:17` (`ddl-auto: create-drop`) — 이 설정 자체는 유지 (H2 인메모리에 적합)
