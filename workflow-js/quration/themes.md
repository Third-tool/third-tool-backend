# Curation Themes — 8 카탈로그

> **성격**: 큐레이션 후보의 태깅 축. 이 8개로 국한하고 새 theme 추가는 최소화.
> **사용**: 각 후보에 primary theme 1개 + 필요시 secondary 1개 (합 최대 2개).
> **원칙**: 백엔드 개발자 자소서·포트폴리오에서 자주 등장하는 큐레이션 축. 자소서 기업이 통상 관심 갖는 표준 카테고리 기준.

---

## 1. 아키텍처 · 시스템 설계 `architecture`

**무엇을 담나**:
- 헥사고날 · Port/Adapter 스왑 설계
- 4-레이어 (Presentation/Application/Domain/Infrastructure) 경계
- Bounded Context 나누기 · BC 간 협력 방식 결정
- 의존 방향 제약 · 도메인 격리
- 재사용 가능한 구조 결정

**이 프로젝트에서 나올 만한 예시**:
- AI 6-Port + Static/LLM Adapter 스왑 (`@ConditionalOnProperty`)
- 4-레이어 강제 · BC 6개 나누기 · Aggregate 경계
- Common → BC 의존 금지 규칙

**면접 훅 소재**: "왜 헥사고날인가", "BC 경계는 어떻게 정했나", "Adapter 교체 시 리스크 관리"

---

## 2. 데이터베이스 · 마이그레이션 `database`

**무엇을 담나**:
- 스키마 설계 결정 (PK 전략, Enum 저장, 인덱스)
- 무중단 마이그레이션 (3-phase, forward-only, 백필)
- Soft Delete 범위 · composite UNIQUE
- Flyway 관행 · 롤백 전략
- FK 정책 (CASCADE/RESTRICT/SET NULL)

**이 프로젝트에서 나올 만한 예시**:
- V19 3-phase 마이그레이션 (learning_axis.layer_id FK 재배선)
- Soft Delete 범위 결정 (ADR003 → ADR021 승격)
- Enum VARCHAR + CHECK (ADR002)
- `(facade_id, name, deleted_at)` 3-col UNIQUE composite

**면접 훅 소재**: "무중단 배포 안전성", "Enum 저장 방식 선택 근거", "Soft Delete 어디에?"

---

## 3. API · 계약 `api-contract`

**무엇을 담나**:
- Controller ↔ Service 경계 (Command/Query record)
- 에러 응답 통일 (GlobalExceptionHandler, ErrorCode enum)
- ApiResponse 래퍼 표준
- HTTP 메서드 매핑 원칙 (POST/GET/PATCH/PUT/DELETE)
- Request/Response DTO 검증 (Bean Validation)
- find-or-create 패턴 등 API 관행

**이 프로젝트에서 나올 만한 예시**:
- Command/Query record 도입 (ADR005)
- GlobalExceptionHandler + ErrorCode 매핑
- ApiResponse<T> 래퍼 표준화

**면접 훅 소재**: "Controller에서 try-catch 왜 안 쓰나", "record 채택 이유", "다중 필드 부분 수정 API 설계"

---

## 4. 도메인 모델링 · DDD `domain`

**무엇을 담나**:
- Aggregate Root · Entity · VO 경계
- 정적 팩토리 + `new` 금지
- 컬렉션 캡슐화 (`unmodifiableList`)
- 결과 VO 패턴 (`ChangeRecord`류)
- displayOrder 1-based · reorder id 집합 검증
- 상태 전이 멱등성
- 이중 방어 (도메인 검증 + DB 제약)

**이 프로젝트에서 나올 만한 예시**:
- `LearningFacade.concepts[]` 다중 컨셉 확장 (`ConceptsChangeRecord`)
- `AxisTopic` 명사구 결정 (ADR004)
- Aggregate 캡슐화 · 결과 VO 패턴 · displayOrder 1-based
- `Card.archive()` 멱등, `OnFieldBudget.resolveReason` 우선순위

**면접 훅 소재**: "왜 이 경계를 Aggregate 안으로 넣었나", "결과 VO 왜 필요한가", "상태 전이 멱등성 왜 중요한가"

---

## 5. 인증 · 보안 `auth-security`

**무엇을 담나**:
- JWT 저장 위치 결정 (Cookie · Memory)
- Refresh Token 화이트리스트 · 재사용 탐지
- 소셜 로그인 통합 · Provider 분리
- OIDC AssumeRole · IAM Role scope
- Secrets 관리
- CORS · CSRF · HttpOnly · SameSite

**이 프로젝트에서 나올 만한 예시**:
- Access = HttpOnly Cookie, Refresh = React 메모리 (ADR009)
- 자체 + 소셜(Kakao/Naver) 통합 UserEntity
- OIDC AssumeRole (ADR013) — GHA long-lived key 폐기
- Secrets Manager 5종 명명 · env-prefix scope (ADR019)

**면접 훅 소재**: "왜 Access는 Cookie, Refresh는 Memory인가", "long-lived key 폐기 리스크 관리"

---

## 6. 인프라 · 배포 `infra-deployment`

**무엇을 담나**:
- VPC 네트워크 설계 · subnet 계층
- ECS Task Role · IAM 분리
- ALB · Target Group · TLS
- CI/CD 파이프라인 (GHA · dual tag · sha7 추적성)
- Base image 결정 · Docker multi-stage
- graceful shutdown · deregistration_delay 매칭
- 무중단 배포 · 롤백

**이 프로젝트에서 나올 만한 예시**:
- VPC 10.0.0.0/16 + 2 AZ × 3-layer + 단일 NAT (ADR015)
- ECS Task IAM Role 3종 분리 (ADR014)
- ALB Listener 80→443 + Target Group type=ip + TLS 1.3 (ADR016)
- graceful shutdown 30s + deregistration_delay 매칭 (ADR017)
- Base image `eclipse-temurin:21-jre-alpine` (ADR012)
- CI/CD dual tag :latest + :sha7 (Story-055)

**면접 훅 소재**: "왜 subnet을 3-layer로 나눴나", "Task Role을 왜 3종 분리", "graceful shutdown이 왜 30s"

---

## 7. 관측성 · 트러블슈팅 `observability`

**무엇을 담나**:
- 로깅 인프라 (Appender · Encoder · MDC)
- 에러 코드 · 알람 규약
- 트러블슈팅 근본원인 추적 서사
- 성능 · N+1 감지·해결
- 인시던트 대응

**이 프로젝트에서 나올 만한 예시**:
- 로깅 profile별 Appender + LogstashEncoder + MDC 화이트리스트 (ADR008)
- MDC에 errorCode 추가 (ADR011)
- "카드 만들 때 축 인식 실패" 근본원인 = orphanRemoval hard delete
- N+1 회피 (`findByAxisIds` 배치 조회)

**면접 훅 소재**: "관측 가능성 어떻게 확보했나", "이 이슈의 진짜 원인은 뭐였나"

---

## 8. 협업 · 프로세스 `collaboration`

**무엇을 담나**:
- ADR 관행 · 결정 기록 문화
- 브랜치 · PR · 커밋 컨벤션
- Reviewer 세션 · 다관점 검토
- workflow 3구간 (milestones/living-docs/topologys) 규칙
- Domain ownership · stop & ask
- 도메인 문서 · 용어 통일

**이 프로젝트에서 나올 만한 예시**:
- ADR 관행 (23건 유지) + 인덱스 관리
- workflow 3구간 규칙 (박제 vs 인덱스 vs 수평제약)
- Story 단위 PR · Reviewer 5관점 세션 (`.claude/rules/review.md`)
- Domain ownership · stop & ask (`.claude/rules/workflow.md` §10)

**면접 훅 소재**: "결정을 어떻게 문서화하나", "코드 리뷰를 어떻게 구조화하나", "팀 협업 표준을 어떻게 정착시켰나"

---

## Theme 간 겹침 처리

한 후보가 여러 theme에 걸치는 경우 흔한 패턴:

| 후보 성격 | 주 theme | 부 theme |
| --- | --- | --- |
| V19 3-phase | `database` | `infra-deployment` (무중단) |
| ADR021 Axis Soft Delete | `domain` | `database` |
| AI 6-Port | `architecture` | `domain` |
| OIDC AssumeRole | `auth-security` | `infra-deployment` |
| MDC errorCode | `observability` | `api-contract` |
| ADR 관행 | `collaboration` | (없음) |

**규칙**: 최대 2개. 그 이상은 후보를 나눔.

---

*최신 갱신: 2026-07-03 · 8개 theme 카탈로그 초기 셋업*
