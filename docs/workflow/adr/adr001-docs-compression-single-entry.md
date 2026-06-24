# adr001: 문서 체계를 docs/ 단일 진입으로 압축한다

**영역**: workflow | **상태**: Accepted | **날짜**: 2026-05-14

> **면접 포인트**
> "프로젝트 문서화를 어떻게 관리했나요? ADR은 왜 사용했나요?"
> → 30,000줄·61개 파일의 분산 문서가 실제 작업 흐름에서 활용되지 않음을 지표(30개 커밋에서 문서 인용 0건)로 확인 후, "코드가 진실 소스인 것은 코드에, 판단과 의도만 docs에"라는 원칙으로 1/10 압축했다.

---

## 왜 이 결정이 필요했나 (Context)

문서 체계가 4개 영역에 분산되어 약 30,000줄·61개 파일에 이르렀다:
- `private-docs/` — API·테이블·테스트·도메인 reference
- `update-docs/` — Claude Code 작업 출력 (architecture·adr·dict 등)
- `docs/architecture/`, `docs/database/` — 아키텍처·DB 설계
- `.claude/rules/` — 작업 흐름 규칙 (12개 파일)

**두 가지 관찰이 결정의 계기였다:**

1. **인용 부재**: 최근 30개 커밋 메시지에서 `근거:` 문서 인용이 단 한 번도 없었다. 사전에 채워두는 reference 문서가 실제 작업에서 활용되지 않는다는 신호.
2. **Plan mode 전환**: Story 단위 Plan mode 협업으로 바꾼 후, "사전 reference 채워두기"보다 "Plan mode가 매번 코드에서 추출"하는 모델이 더 효율적임이 확인됐다.

---

## 무엇을 결정했나 (Decision)

### 원칙: 코드가 진실 소스인 것은 코드에

| 영역 | 진실 소스 |
|------|-----------|
| API 명세 | Controller + Request/Response DTO + Swagger UI |
| DB 스키마 | Flyway `V*.sql` + JPA 매핑 |
| 테스트 매트릭스 | 테스트 코드 + 메서드명 |
| 패키지·디렉토리 사실 | 실제 디렉토리 트리 |

`docs/`에 두는 것은 **코드만으론 알 수 없는 의도와 결정**뿐:

```
docs/
├── DOMAIN.md     # 도메인 의도·용어·BC별 불변식 (코드에는 "what"만, "why"가 없음)
├── PACKAGE.md    # 패키지·BC·레이어 의존 규칙 (구조는 코드, 규칙은 의도)
└── adr/          # 아키텍처 결정 기록 (대안 비교·거부 사유는 코드에 없음)
```

### Reviewer 세션 신설

단일 Claude 자가 점검으로는 도메인 캡슐화·BC 의존 위반·NPE 가능성 누락이 잦았다. Story 작업 완료 후 push 전 **5관점 병렬 subagent**(Domain / Architecture / API·Exception / Test / Sceptical)로 강제 리뷰를 도입했다.

### 폐기된 패턴

- `private-docs/` 전체 (16파일/약 8,000줄) → 도메인 의도는 `DOMAIN.md`로 압축, API·테이블·테스트는 코드가 진실 소스이므로 폐기
- `update-docs/` 전체 (17파일/약 3,600줄) → ADR은 `docs/adr/` tracked로, 나머지는 Plan mode가 매번 코드에서 추출
- `.claude/rules/` 13개 파일 → 5개 파일로 압축

---

## 대안과 거부 이유 (Alternatives)

| 대안 | 장점 | 거부 이유 |
|------|------|-----------|
| 4영역 현상 유지 | 변경 없음 | 30,000줄 유지 부담. 인용 0건으로 reference 매칭 프로토콜이 실제 작동 안 함 |
| docs 전면 폐기 + 코드 주석으로 이전 | 가장 단순 | 코드만으론 BC 횡단 의도를 알 수 없음. ADR의 영속 가치도 사라짐 |
| `private-docs` 유지 + `update-docs`만 폐기 | 도메인·API 명세 별도 보존 | API·테이블은 코드가 진실 — 이중 관리 부담 유지. 인용 부재 문제 해결 안 됨 |

---

## 결과와 트레이드오프 (Consequences)

**긍정적**
- 문서 부담 약 30,000줄 → 약 3,000줄로 1/10 압축
- 진실 소스 명확화 — 의도는 `docs/`, 사실은 코드
- ADR이 GitHub에 tracked로 공개 → PR 리뷰 흐름에 자연스럽게 합류
- Reviewer 세션 강제로 단일 Claude 자가 점검 누락 보완

**트레이드오프**
- BC별 도메인 상세 매트릭스가 사라짐 → 깊은 도메인 디테일은 코드+테스트로 보강
- API 외부 컨슈머 시점 자료는 Swagger UI에 의존
- Reviewer 세션 1회당 5배 토큰 비용 (Story당 1회 발생)

---

## 재검토 시점

- `DOMAIN.md` 단일 파일이 1,500줄을 넘는 시점 → BC별 파일 분할 재도입 검토
- API 외부 컨슈머가 생긴 시점 → 별도 API 명세 문서 도입
- Reviewer 세션 토큰 비용이 작업 흐름을 막을 정도가 된 시점
