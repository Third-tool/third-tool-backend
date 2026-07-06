# Brainstorming · AI 횡단 카탈로그

> AI — LLM 어댑터 추상화 · 프롬프트 버전 · 출력 품질 평가 · 환각 가드 · 비용 · 사용자 피드백 루프 · 캐싱 · 세션 저장소 · 컨텍스트 윈도우 — 횡단 관심사를 모은다.

---

## [후보 1] LLM Provider 어댑터 추상화 강화

> Spring AI ChatClient / Gemini / Claude를 같은 Port 뒤로 숨겨서 swap 가능하게.

### 배경
- 현재 활성화된 구현체는 `StaticAxisSuggestionAdapter` / `StaticAxisTopicSuggestionAdapter` 두 개뿐 (`feat/045` 머지).
- Port (`AxisSuggestionPort` 등)는 `feat/044`로 분리되어 있어 LLM 어댑터 추가 시 어디에 끼울지 명확.
- 그러나 **여러 LLM provider를 동시에 운영**하거나 **A/B 테스트**할 패턴은 아직 검토 X.

### 후보
- **A안**: `Map<ProviderType, AxisSuggestionPort>` 자동 주입 + 설정으로 선택 (`thirdtool.suggestion.provider=spring-ai|gemini|claude`). 장점: User BC의 SocialOAuthFlow 패턴 답습. 비용: 어댑터별 응답 형식 정규화 필요.
- **B안**: Spring AI ChatClient만 활용 — Spring AI가 provider 추상화 책임. 장점: 외부 라이브러리에 위임. 비용: Spring AI 제약 (구조화 출력 한계).
- **C안**: 현재 Static만 유지하고 LLM은 신규 BC로 분리. 장점: 명확한 경계. 비용: BC 폭증.

### 1차 권장
A안 + B안 결합 — Spring AI ChatClient 어댑터를 Map의 한 항목으로. 다른 provider는 필요 시 추가.

### PES 승격 경로
- `product-aisuggestion.md` Epic 추가: "LLM 어댑터 도입" (Story 단위)
- ADR 후보: "AI Provider 어댑터 패턴" (User BC와 동일 답습 결정 기록)

### 미해결 질문
- 토큰 한도가 다른 provider 간 응답 형식 (구조화 출력 지원도)이 일치하나?

---

## [후보 2] 프롬프트 버전 관리

> 운영 중 프롬프트를 바꾸면 결과가 미세하게 변한다 — 변경 추적 가능해야.

### 배경
- 현재 LLM 활성 어댑터가 없어 프롬프트 자체가 없음.
- v1.5 LLM 어댑터 도입 시점에 프롬프트가 코드에 박히면 변경 이력 추적이 git log에 종속.
- 동일 conceptSet에 대해 어제 결과와 오늘 결과가 다른 이유 추적이 어려워짐.

### 후보
- **A안**: `prompts/` 디렉토리 + 버전 suffix (`axis-v1.txt`, `axis-v2.txt`) + 어댑터가 명시 선택. 장점: 단순·git 친화. 비용: 운영 중 hot-swap 불가.
- **B안**: DB 테이블 `prompt_registry` + 버전 컬럼 + A/B 분배. 장점: 운영 중 변경 가능. 비용: 인프라 추가.
- **C안**: 코드 안에 inline. 비용: 추적 불가능.

### 1차 권장
A안. v1.5 시점에 단순하게 시작, 사용자 기반 커지면 B안.

### PES 승격 경로
- `product-aisuggestion.md` Epic 또는 신규 `product-ai-prompts.md`
- ADR 후보: 프롬프트 버전 관리 정책

---

## [후보 3] 출력 품질 평가 파이프라인

> "이 제안 좋아졌는가"를 사람 감 대신 정량으로 측정.

### 배경
- LLM 응답은 비결정적 — 프롬프트 1줄 바꿔도 품질 회귀 가능.
- 회귀를 감지할 gold dataset · 평가 메트릭이 없음.
- v1.5 → v2(AI Interactive Roadmap)로 갈수록 평가 부재 비용 누적.

### 후보
- **A안**: gold dataset (concept × 기대 axis 후보 100건) + LLM-as-judge로 자동 점수. 장점: 자동화. 비용: gold dataset 구축 + judge LLM 비용.
- **B안**: 사용자 클릭률(channel 선택률) 같은 implicit signal. 장점: 실사용 데이터. 비용: 사용자 N명 충분해야 통계적 유의.
- **C안**: 수기 spot check. 비용: 사람 부담·일관성 없음.

### 1차 권장
A안 + B안 결합. v1.5는 A안만, v2는 B안 추가.

### PES 승격 경로
- 신규 Product (v2): `product-ai-evaluation.md`
- ADR 후보: "AI 출력 품질 평가 메트릭"

---

## [후보 4] 환각 / 유해 응답 가드

> LLM이 사실과 다른 내용을 자신있게 말하면 막는다.

### 배경
- 학습 도메인 특성 — 잘못된 자료 추천 / 존재하지 않는 책 / 부정확한 개념 설명이 학습 사고 위험.
- v1.5 LLM 도입 시점에 가드 부재면 신뢰도 크게 깎임.

### 후보
- **A안**: 룰 기반 1차 필터 (금칙어·형식 검증) + LLM-as-judge 2차. 장점: 단계적. 비용: 2단계 비용.
- **B안**: 출처 강제 (LLM이 추천 시 URL/책명 필수 + 검증). 장점: 사용자 검증 가능. 비용: 검증 인프라 필요.
- **C안**: 가드 없음 + 사용자 책임 안내 ("AI 제안 참고용"). 비용: 신뢰도 깎임.

### 1차 권장
A안 + 후보 6 (피드백 루프)로 보완. B안의 출처 검증은 비용 크고 ROI 불확실.

### PES 승격 경로
- `product-aisuggestion.md` Epic 또는 후보 3 (`product-ai-evaluation.md`)에 흡수
- ADR 후보: "AI 환각 가드 정책"

---

## [후보 5] AI 비용 모니터링 + 사용자별 한도

> LLM 토큰 비용 폭증 방지.

### 배경
- ops.md 후보 4는 AWS 일반 비용, AI 토큰 비용은 별도 차원.
- Rate Limit (분당 10회)은 명세에 있으나 사용자별 일·월 토큰 한도는 미명시.
- 악의적·실수 polling으로 비용 폭주 가능.

### 후보
- **A안**: 사용자별 일 N 토큰 한도 + 초과 시 v1.5 Static fallback. 장점: 비용 통제. 비용: 한도 결정 + 안내 UI.
- **B안**: 시스템 전역 월 한도 — 초과 시 모든 사용자 차단. 장점: 단순. 비용: 정상 사용자도 피해.
- **C안**: 한도 없음 (Rate Limit만). 비용: 비용 폭주 위험.

### 1차 권장
A안. 사용자 단위 한도가 공정. Rate Limit (분당 10회) + 일 한도 (예: 100회)의 이중 방어.

### PES 승격 경로
- `product-aisuggestion.md` Epic "비용 통제"
- ADR 후보: 한도 정책

---

## [후보 6] 사용자 피드백 루프

> "이 제안이 도움 됐나요?" 시그널 수집 → 프롬프트 개선.

### 배경
- AI 제안 결과를 사용자가 수용 / 거부 / 수정하는 행동이 운영 시그널.
- 그 신호를 수집하는 인프라 없음.
- 후보 3 (평가 파이프라인)의 B안과 결합.

### 후보
- **A안**: 제안 결과에 👍/👎 + 자유 텍스트. 응답을 `ai_feedback` 테이블에 누적. 장점: 단순·실사용 데이터. 비용: 사용자에게 노이즈.
- **B안**: implicit signal (제안 후 채택률) 자동 측정. 장점: 사용자 부담 0. 비용: 측정 인프라 필요.
- **C안**: 별도 인터뷰. 비용: 1인 운영 부담.

### 1차 권장
A안 + B안 결합. B안은 인프라 있으면 추가 비용 작음.

### PES 승격 경로
- 후보 3 (`product-ai-evaluation.md`)에 흡수
- ADR 후보: "AI 피드백 수집 정책 — 명시·암시 둘 다"

---

## [후보 7] AI 응답 캐싱 전략

> 같은 conceptSet에 대한 재요청을 굳이 LLM 비용으로 처리할 필요 없음.

### 배경
- 사용자가 AI 제안 화면을 새로고침하면 매번 LLM 호출.
- `product-aisuggestion.md`에 "refresh context 변경으로만 재추천 허용" 의도 명세.
- 그러나 캐시 키·TTL·무효화 룰 미명세.

### 후보
- **A안**: `conceptSet + 기존 이름 목록` 해시를 key, 24h TTL. 장점: 단순. 비용: stale 응답 가능.
- **B안**: 캐시 없음 + 새 요청은 새 응답. 장점: 신선함. 비용: 비용 증가.
- **C안**: 사용자별 lastSuggestion 1건만 보존 (재요청 시 그대로 반환 + "다시 받기" 버튼만 호출). 장점: 비용 명확. 비용: 다양성 깎임.

### 1차 권장
C안. 사용자에게 "다시 받기" 명시적 의사로만 새 호출 → 비용 통제 + 사용자 통제권.

### PES 승격 경로
- `product-aisuggestion.md` Epic "캐싱"
- ADR 후보: 캐시 정책

---

## [후보 8] AI Interactive Roadmap 세션 저장소

> Redis (휘발) vs DB (영속) — 어디에 둘 것인가.

### 배경
- `product-ai-interactive-roadmap.md` (v2) 세션 기반 흐름 — `STARTED → AXIS_DRAFTED → ... → SAVED`.
- 세션 중단·재개를 PES가 명세 → 영속 저장소 필요.
- 그러나 단순 RDB에 두면 부하 증가, Redis에 두면 영속성 한계.

### 후보
- **A안**: RDB `roadmap_session` 테이블. 장점: ACID·영속·기존 인프라. 비용: 부하 증가.
- **B안**: Redis (RDB persistence 활성). 장점: 빠름·세션 만료 자동. 비용: Redis 인프라 추가 + 백업 절차.
- **C안**: 클라이언트 측 localStorage + 서버는 commit 시점에만 저장. 장점: 서버 부하 0. 비용: 디바이스 간 이동 불가.

### 1차 권장
A안. v2 진입 시점에는 단순함 우선. 사용자 N명 도달 시 B안 검토.

### PES 승격 경로
- `product-ai-interactive-roadmap.md` 자체에 흡수 — Story 단위 추가
- ADR 후보: 세션 저장소 결정

---

## [후보 9] 컨텍스트 윈도우 관리

> 사용자 학습 자료 + 카드 누적이 LLM 토큰 한도 넘으면 어떻게 압축할 것인가.

### 배경
- AI 제안에 사용자 컨텍스트(현재 axes, topics, 자료 목록)를 함께 전달하는 게 v1.5 갭 인지 모델.
- 사용자가 axes 50개·topics 200개로 커지면 컨텍스트 토큰 폭증.
- 압축 정책 미명세.

### 후보
- **A안**: 사용자 컨텍스트를 사전 요약 (`/api/v1/learning-facade/context` 요약 응답) → 그 요약을 LLM에 전달. 장점: 토큰 통제. 비용: 요약 정확도.
- **B안**: 관련성 높은 axes만 동적 선택 (예: 최근 단련된 N개). 장점: 토큰 작음. 비용: 누락 위험.
- **C안**: 잘라내기 (앞 N개만). 비용: 의도 누락.

### 1차 권장
B안 + A안 결합. 가까운 영역만 보내고, 더 필요하면 요약본 추가.

### PES 승격 경로
- 후보 1 (LLM 어댑터)과 같은 Product에 흡수
- ADR 후보: "AI 컨텍스트 윈도우 관리 정책"
