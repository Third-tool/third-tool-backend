# Rule: update-docs/architecture 갱신 프로토콜

> 프로젝트 아키텍처 4관점을 기록한다.
> Story/Epic 종료 시 영향받은 파일만 일괄 갱신한다.
> 본 규칙은 [`update-docs.md`](../update-docs.md) §1 패키지 일람의 architecture 패키지 상세이다.

---

## 1. 위치·파일 구성

- **위치**: `update-docs/architecture/`
- **파일**: 4개 고정. 현재 명명 그대로 유지 — 정규화·리네이밍은 사용자 요청 시에만.

| 파일 | 다루는 관점 |
| --- | --- |
| `overview.md` | 프로젝트가 무엇을 하는지 — 도메인·사용자·풀려는 문제·핵심 가치 |
| `infra.md` | AWS 리소스(EC2/ECS/RDS/ElastiCache/S3/ALB), VPC·네트워크, CI/CD 파이프라인(GitHub Actions → ECR → ECS), 환경 분리(dev/staging/prod) |
| `TechStack.md` | 언어·프레임워크·라이브러리 버전, 채택 근거(ADR 링크), Tech Radar Adopt/Trial/Assess/Hold 분류 |
| `application.md` | 헥사고날/포트-어댑터 구조, 레이어 의존 규칙(domain ← application ← adapter), 모듈·패키지 구조 (논리적 뷰) |

---

## 2. 파일별 갱신 트리거

| 파일 | 트리거 |
| --- | --- |
| `overview.md` | 제품 방향·범위가 변한 Story 종료 시 (드물게 갱신) |
| `infra.md` | 인프라·배포 구성이 변한 Story 종료 시 (AWS 리소스 추가/제거, 파이프라인 변경) |
| `TechStack.md` | 의존성 추가·교체·버전 업그레이드 Story 종료 시 (build.gradle 변경 동반) |
| `application.md` | 새 BC 도입·레이어 재편·모듈 분리 Story 종료 시 |

---

## 3. 파일별 권장 골격

각 파일은 추상화·예상이 아닌 **실제 구성·결정의 사실**을 적는다.

### overview.md
- 한 줄 요약: 누구를 위한 어떤 도구인가
- 핵심 사용자 시나리오 1-3개 (불릿)
- 시스템 경계 (BC 목록 정도)
- 외부 연동 (현재 없으면 "없음" 명시)

### infra.md
- 환경 분리 (dev/staging/prod 각 환경의 호스팅·DB)
- AWS 리소스 트리 (실제 사용 중인 것만)
- CI/CD 파이프라인 단계 요약
- 비밀·환경 변수 관리 위치 (Parameter Store 등)

### TechStack.md
- 언어·런타임 버전 (Java 21, Gradle 8.x, ...)
- 주요 의존성 표 — 라이브러리 / 버전 / 용도 / 채택 근거(ADR 또는 한 줄)
- (선택) Tech Radar 분류: Adopt / Trial / Assess / Hold
- Deprecated 의존성 표시

### application.md
- 패키지 구조 다이어그램 (또는 텍스트 트리)
- BC 목록 + 4-Layer 구조 그림 1회
- 레이어 의존 규칙 (`domain ← application ← infrastructure / presentation`)
- BC 간 의존 규칙

---

## 4. 갱신 절차

1. Story 작업 중 영향이 발생하면 메모(대화 컨텍스트로 충분).
2. Story 종료 push **직전**, 본 Story가 어느 architecture 파일에 영향을 줬는지 자체 점검.
3. 영향받은 파일별로 후보 변경분을 1줄씩 사용자에게 보고.
4. 사용자 승인 시 갱신 → 별도 커밋(`docs(architecture): ...`).
5. 영향 없는 파일은 touch 금지.

---

## 5. 커밋 규칙

- scope: `architecture`
- 메시지 예: `docs(architecture): infra에 ECS 클러스터 분리 반영 [Story-{NNN}-{N}]`
- 한 Story에서 여러 architecture 파일이 영향받으면 **한 커밋에 묶어도 무방** (한 Story = 한 architecture 커밋이 이상적).
- ADR과 함께 발생하면 ADR은 별도 커밋([`adr.md`](./adr.md) §5).

---

## 6. 금지

- 4개 파일 외 새 architecture 파일을 사용자 요청 없이 추가하지 않는다.
- 추측·예상으로 미구현 인프라/라이브러리를 미리 적지 않는다 — architecture는 사실의 기록.
- ADR로 다뤄야 할 결정 자체를 architecture에만 적지 않는다 — 결정 근거는 ADR, architecture는 결과의 사실.
