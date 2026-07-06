# 0.0.3v / Cost — 로컬 스코프 유지 (실측)

> **범위**: 본 버전(0.0.3v)은 M2와 동일한 **로컬 개발 스코프**. 신규 AWS 리소스·LLM 호출 없음. **신규 비용 발생 = 0원 / 0 USD**.

---

## 본 버전 신규 비용 (실측)

| 카테고리 | 신규 비용 (일) | 실측 근거 | 비고 |
| --- | --- | --- | --- |
| AWS 신규 리소스 | **$0** | 리소스 신설 SQL·API 0건 (`infra.md` 활동 로그 참조) | 배포 라인 미변경 |
| LLM (Gemini · GPT · Claude) | **$0** | 호출 코드 0건, `thirdtool.suggestion.provider=static` 기본 유지 | Static Adapter (backend-developer.json 확장) 만 활성. LLM Adapter 여전히 미도입 |
| 외부 도구·SaaS | **$0** | `build.gradle.kts` 의존성 신설 0건 | Reviewer subagent도 개발 세션 내부. 외부 비용 없음 |
| Vercel / Cloudflare CDN | **$0** | FE 배포 미수행 유지 | M1 잔존 유지 |
| **합계 (신규)** | **$0** | | 목표 달성 |

---

## M1 잔존 비용 (이관 · 참조용)

M1에서 신설된 AWS 리소스는 여전히 가동. 본 버전 스코프 밖. 상세는 `../0.0.1v/cost.md` 참조.

| 항목 | 참조 실측 | M3 예상 유지 | 비고 |
| --- | --- | --- | --- |
| ECS Fargate (prod 2 · staging 1) | ~$2.80 / 일 | ~$2.80 / 일 | 재배포 없음, M3 코드는 미반영 |
| ALB | ~$0.80 / 일 | ~$0.80 / 일 | |
| Route 53 hosted zone | ~$0.02 / 일 | ~$0.02 / 일 | |
| RDS single-AZ | ~$0.45 / 일 | ~$0.45 / 일 | |
| Secrets Manager | ~$0.07 / 일 | ~$0.07 / 일 | |
| ECR | ~$0.05 / 일 | ~$0.05 / 일 | |
| CloudWatch Logs | ~$0.10 / 일 | ~$0.10 / 일 | 배포 없이 축적만 |
| NAT Gateway | ~$1.44 / 일 | ~$1.44 / 일 | |
| Prometheus + Grafana | ~$0.40 / 일 | ~$0.40 / 일 | 로컬 관측 재개 시 유용 |
| **잔존 총 (참조)** | **~$6.13 / 일** | **~$6.13 / 일** | 배포 정지 상태의 유지 비용 |

**M3 7일 잔존 누적 (참조)**: ~$42.91 (M2와 동일)

---

## 비용 관리 (로컬 스코프 관점)

본 버전 활동 중 다음 확인:

- [ ] AWS Budgets 알림 이상 없음 확인 (월 한도 대비 진척)
- [ ] NAT Gateway 트래픽 급증 없음 (M3도 미배포이므로 트래픽 0 예상)
- [ ] LLM 호출 실수 방지 (Adapter 미구현이므로 원천 차단 유지)
- [ ] 로컬 개발 중 실수로 AWS SDK 호출 발생 안 하도록 profile 관리 (`local` profile 강제)
- [ ] AI Reviewer subagent 5관점 세션 3회 실행 — 사용자 클라우드 계정 크레딧에서만 소진 (프로젝트 원가 무관)

---

## 신규 비용 유발 조치 (본 버전 중)

- 없음. 본 버전 스코프상 인프라·외부 서비스 도입 금지 유지.
- **catalog 확장(backend-developer.json 하네스 엔지니어링 축 5 챕터 추가)** 도 로컬 classpath JSON 조작만 · 비용 무관.
- **prompt 자산 신설 (7 파일 · concept-spec.txt 포함)** 도 classpath 리소스 · 비용 무관.

---

## M4 (0.0.4v) 이후 비용 재산정 트리거

다음 조건 중 최소 1개 성립 시 재산정 착수:

1. **LLM Adapter 활성화 (AS Epic 4)** → Vertex AI Gemini 첫 비용 baseline 필요. M3에서 6-Port 인터페이스가 확립되고 concept-spec.txt · 6개 프롬프트 템플릿까지 자산으로 배치됨 → **M6 진입 시점에 물리적 활성화만 남음**.
2. **dev 환경 재배포** → CloudWatch 로그 축적 · 트래픽 재개 · MySQL V20~V22 실행 시간 실측.
3. **RDS Multi-AZ 전환** → 비용 배수 (v1 릴리스 2026-08-19 이후 안정화 판단 시).
4. **k6 부하 테스트 (Product 9)** → 트래픽 폭증 시 데이터 전송 비용.
5. **Static Adapter role catalog 다각화 (planner/designer/problem-solver JSON 신설)** — 여전히 classpath 리소스 · 비용 무관 (조기 알림 목적으로 나열).

---

## 참고

- M1 상세 비용 실측: `../0.0.1v/cost.md`
- M2 실측: `../0.0.2v/cost.md`
- 배포 재개 시 재측정 대상: ECS · ALB · RDS · Grafana · LLM 요금 baseline
- 비용 통제 원칙: 정당화 없는 인프라 신설 금지 (M2·M3 준수)

---

## AI Static Adapter M3 완성에도 왜 비용 0인가

M3 완주된 6-Port Static Adapter도 **로컬 리소스 + classpath JSON만**이라 실질 외부 지출이 없다:

- **4 신규 Port + DTO**: Java record · 컴파일 타임 자산 · 외부 호출 없음
- **4 Static Adapter**: `RoleDetector` (하드코드 Java Map) + `SuggestionCatalogLoader` (classpath 로딩) + role별 JSON 조회. LLM API 호출 없음.
- **backend-developer.json 확장**: 로컬 JSON 편집 (10 챕터 + 3 selectionOutline). 재배포 없이 프로젝트 JAR 내부.
- **prompts/ 7 파일 (concept-spec + 6 템플릿)**: classpath 리소스. v1은 Static Adapter가 소비하지 않고 v2 LLM Adapter (M6~) 대비 자산으로만 존재.
- **SuggestionController 4 엔드포인트**: 로컬 HTTP 라우팅 · Static Adapter 응답 매핑. AWS·LLM 무관.

**LLM Adapter 활성 시점 (M6~ 예상 비용 baseline 필요)**:
- Spring AI 1.0 GA + Vertex AI Gemini Flash 2.5 도입 시 첫 API 호출 요금 발생.
- 예상 요금 (Gemini 1.5 Flash 참조): $0.075 / 1M input tokens · $0.30 / 1M output tokens.
- 예상 요청 크기:
  - `chapters-outline` — ~700 input tokens (concept-spec 500 + few-shot 200) + ~300 output tokens (챕터 5개 title/rationale)
  - `chapter-subtree` — ~600 input tokens + ~500 output tokens (bodyAsciiTree 통짜)
  - `selection-outline` / `selection-subtree` — 유사 규모
- **세션당 호출**: outline 1 + subtree N (챕터 개수) → 예상 6~10 호출/세션. 세션당 예상 비용 <$0.001.
- 트리거 시점(M6)에 `cost.md` 재산정.

**관찰 지표 로깅 v1 (이슈 #20 이관)** — 아직 미배선. LLM Adapter 도입과 함께 동시 배선 예정:
- 세션당 AI 호출 수 (Port별 분해)
- 세션당 총 토큰 (input/output/cached 분해)
- 세션당 재생성 횟수 (챕터별)
- Static fallback 발동 비율
- Rate limit 접촉 여부

목적: v2 시점의 예산 상한 값·정책 세부 산정 근거 데이터 확보.

---

*작성일: 2026-07-03 | 본 버전 스코프: 로컬 개발 · **비용 신규 발생 $0** (M2와 동일 · Reviewer 재개해도 프로젝트 원가 무관) | 참조: `../0.0.1v/cost.md` · `../0.0.2v/cost.md`*
