# docs/ — Backend Interview Documentation Hub

면접관이 백엔드 개발자에게 묻는 5개 영역을 기준으로 기술 결정·장애·운영 경험을 분류해 둔 공간.
"왜 그렇게 결정했나요?"에 바로 꺼낼 수 있는 근거 문서들.

---

## Directory Structure

```
docs/
├── docs.md                       ← 이 파일 (구조 정의 및 작성 규칙)
├── DOMAIN.md                     ← 기존 도메인 의도·용어·BC별 불변식
├── PACKAGE.md                    ← 기존 패키지·BC·레이어 구조
├── adr/                          ← 기존 원본 ADR (ADR001~)
│
├── implementation/               ← 기능 구현 (Spring, 코드 아키텍처, AI/RAG, 이벤트 등)
│   ├── troubleshooting/          ← ts001-slug.md
│   ├── adr/                      ← adr001-slug.md
│   └── living-docs/              ← slug.md or slug.url.md
│
├── workflow/                     ← 업무 방식 (개발 프로세스, 협업 전략, 브랜치 전략)
│   ├── troubleshooting/
│   ├── adr/
│   └── living-docs/
│
├── data/                         ← 데이터 설계 (모델링, SQL/NoSQL, 트랜잭션)
│   ├── troubleshooting/
│   ├── adr/
│   └── living-docs/
│
├── deployment/                   ← 배포 인프라 (Docker, CI/CD, AWS 클라우드)
│   ├── troubleshooting/
│   ├── adr/
│   └── living-docs/
│
└── operations/                   ← 운영 (장애 대응, 모니터링, 부하 테스트)
    ├── troubleshooting/
    ├── adr/
    └── living-docs/
```

---

## Top-level Domains

| Directory | 한국어 설명 | 주요 키워드 | 대표 면접 질문 |
|-----------|-------------|-------------|----------------|
| `implementation` | 기능 구현 기술 판단 및 설계 | Spring, Kotlin/Java, 이벤트, AI, RAG, 코드 구조 | "이 기술을 왜 선택했나요?" |
| `workflow` | 팀·개인 업무 스타일과 개발 방식 | ADR, 코드리뷰, PR 전략, 브랜치 전략, 협업 | "평소 개발 프로세스는 어떻게 되나요?" |
| `data` | 데이터 모델링과 저장소 설계 | ERD, JPA, QueryDSL, Redis, Mongo, 트랜잭션 | "DB 설계 시 어떤 기준으로 결정했나요?" |
| `deployment` | 빌드·배포·인프라 자동화 | Docker, GitHub Actions, AWS EC2/ECS/RDS | "CI/CD 파이프라인은 어떻게 구성했나요?" |
| `operations` | 서비스 운영 및 안정성 | 장애 대응, Grafana, Prometheus, k6, 로깅 | "장애 대응 경험이 있나요?" |

---

## Sub-directory Rules

### `adr/` — Architecture Decision Record

기술적 판단의 이유와 맥락. "왜 이걸 선택했는가"를 나중에 설명할 수 있게 남긴다.

**파일명 규칙**: `adr{NNN}-{slug}.md`
- `NNN`: 도메인 내 3자리 순번 (001부터)
- `slug`: 영문 kebab-case (3-5단어)

**예시**: `adr001-bigint-auto-increment-pk.md`, `adr002-enum-varchar-check-constraint.md`

**내용 구조**:
```
# adr{NNN}: {제목}

**영역**: {domain} | **상태**: Accepted | **날짜**: YYYY-MM-DD

> **면접 포인트**
> "면접 질문 예시?" → 핵심 답변 1줄

## 왜 이 결정이 필요했나 (Context)
## 무엇을 결정했나 (Decision)
## 대안과 거부 이유 (Alternatives)
## 결과와 트레이드오프 (Consequences)
## 재검토 시점
```

- 작성 시점: 의사결정 시점 또는 직후
- 번복된 결정도 `**상태**: Deprecated`로 남겨 둔다
- **면접 포인트** 섹션이 핵심 — 이 문서가 답하는 면접 질문과 한 줄 답변을 맨 위에 명시

---

### `troubleshooting/` — 문제 해결 기록

개발·운영 중 실제로 겪은 문제와 해결 과정.
가설 추적이 핵심 — 정답만 쓰면 운 좋게 맞춘 것처럼 보인다. 틀린 가설도 그대로 기록한다.

**파일명 규칙**: `ts{NNN}-{slug}.md`
- `NNN`: 도메인 내 3자리 순번 (001부터)
- `slug`: 증상 또는 원인을 영문 kebab-case로

**예시**: `ts001-adr-number-conflict-parallel-branches.md`

**내용 구조**:
```
# ts{NNN}: {제목}

**영역**: {domain} | **심각도**: Critical/Medium/Low | **발견**: {경위} | **복구 시간**: {MTTR}

> **면접 포인트**
> "면접 질문?" → 핵심 답변 1줄

## 증상 (Symptom)
## 가설과 검증 (시간 순 — 틀린 가설 포함)
## 근본 원인 (Root Cause)
## 해결 (Fix)
## 재발 방지 (Prevention)
## 배운 점 (선택)
```

- 작성 시점: 문제 해결 직후
- 기록하지 않는 것: typo, null check, 로그만 보면 바로 나오는 에러
- 기록하는 것: 30분 이상 헤맨 버그, 여러 계층 가로지른 버그, 잘못된 가정을 깬 순간

---

### `living-docs/` — 완성된 문서 or 외부 링크

작성 완료된 설계 문서를 직접 올리거나, Notion·Google Docs 등 외부 URL로 연결한다.

**파일명 규칙**:
- 직접 문서: `{slug}.md`
- 외부 링크 포인터: `{slug}.url.md`

**외부 URL 포인터 포맷** (`*.url.md`):
```markdown
# {문서 제목}

> 외부 문서: {URL}

## 요약
(한 줄~세 줄 요약 — 링크가 죽어도 의미를 알 수 있게)
```

---

## Naming Convention

- 파일명은 소문자 + 하이픈 (`kebab-case`)
- 공백·특수문자 사용 금지
- 언어는 자유 (한국어·영어 혼용 가능) — 단, 파일명은 영어

---

## 현재 문서 현황

### implementation/
| 파일 | 요약 | 면접 키워드 |
|------|------|-------------|
| `adr/adr001-sync-domain-event-bc-cooperation.md` | BC 간 협력에 동기 @EventListener 도입 | 이벤트, DDD, BC 협력, @TransactionalEventListener |
| `adr/adr002-jwt-at-cookie-rt-memory.md` | AT HttpOnly Cookie / RT 메모리 저장 결정 | JWT, XSS, CSRF, 인증, 쿠키 |

### workflow/
| 파일 | 요약 | 면접 키워드 |
|------|------|-------------|
| `adr/adr001-docs-compression-single-entry.md` | 30,000줄 문서를 3,000줄로 압축한 결정 | ADR, 문서 전략, 코드가 진실 소스 |
| `troubleshooting/ts001-adr-number-conflict-parallel-branches.md` | 평행 브랜치 ADR 번호 충돌 → git rebase --onto | Git, stacked PR, cascade rebase |

### data/
| 파일 | 요약 | 면접 키워드 |
|------|------|-------------|
| `adr/adr001-bigint-auto-increment-pk.md` | PK 전략 — BIGINT AUTO_INCREMENT 선택 이유 | PK 전략, UUID vs BIGINT, IDOR |
| `adr/adr002-enum-varchar-check-constraint.md` | Enum → VARCHAR + CHECK 제약 저장 | JPA Enum, ORDINAL vs STRING, DB 이중 방어 |
| `adr/adr003-soft-delete-asset-domains-only.md` | 자산성 도메인만 soft delete 선택 적용 | Soft delete, @SQLRestriction, 인덱스 |

### operations/
| 파일 | 요약 | 면접 키워드 |
|------|------|-------------|
| `adr/adr001-logback-logstash-profile-appender.md` | 프로파일별 로그 분기 + MDC 화이트리스트 | Logback, MDC, PII, LogstashEncoder |

---

## Interview Alignment

| 면접 질문 유형 | 해당 디렉토리 |
|----------------|---------------|
| "이 기술을 왜 선택했나요? 대안은 안 봤나요?" | `*/adr/` |
| "장애 또는 어려운 버그 해결 경험이 있나요?" | `*/troubleshooting/` |
| "평소 개발 방식이나 문서화 습관은?" | `*/living-docs/` |
