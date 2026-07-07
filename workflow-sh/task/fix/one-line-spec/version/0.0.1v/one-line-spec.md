# [Fix · One-line-spec] 양식 정의

> **fix 트랙**: 이미 진행 중인 작업 안에서 발견된 **한 줄·한 값짜리 수정**의 트래킹 문서.
> planning은 `../../../../pes/workspectrum/one-line-spec/version/0.0.1v/one-line-spec.md`(시간 기반 1시간 이내) 참조.
> fix는 **수정 범위·복잡도** 기준 5단계 중 최소 단위.

---

## 사용 시점 (트리거)

다음 조건을 **모두** 만족할 때 본 양식을 쓴다.

- [ ] 원본 작업(PR / Story / Product / SDD)이 **이미 진행 중** 또는 **머지 후 발견된 미세 수정**
- [ ] 수정 범위가 **한 줄 / 한 값 / 한 상수** (코드 라인 1개 변경, 메시지·라벨·문구 교체, 상수·임계값 갱신)
- [ ] 분기·검증·테스트 추가 **없음** — 기존 동작 그대로, 표면 텍스트나 값만 교체
- [ ] 원본 명세(DoD / AC / ErrorCode)에 의미 변경 **없음**

**졸업 신호 → fix/feature-story로**:
- 수정 라인이 2+개로 늘어남
- 한 메서드 안에서 분기·검증·테스트가 함께 추가됨
- 원본 AC가 갱신되어야 함 (한 값 갱신 ≠ AC 갱신)

---

## 양식 골격

```
# [Fix] {수정 요지 — 동사로 시작} ({YYYY-MM-DD})

## 대상 (원본)
- 원본 PR / Story / Product / SDD: {식별자 + 링크}
- 본 fix 발견 시점: {YYYY-MM-DD}

## 트리거
- {왜 이 한 줄을 바꾸는가 — 리뷰 코멘트 / 표류 / 클로드 코드 인터랙션 / 외부 입력}

## 변경 내역
- {파일:라인} `before` → `after` (한 줄)

## 검증
- {확인 신호 1줄 — 빌드 / 테스트 / 수동}

## 영향
- N/A (원본 명세에 의미 변경 없음)
```

### 섹션 가이드

| 섹션 | 채울 것 |
| --- | --- |
| **대상** | 원본 작업의 식별자. PR #N / `workflow/task/pes/sdd/Product.md` Story 5-3 / `pes/sdd/product-auth.md` 등 |
| **트리거** | 한 문장. "리뷰 코멘트: 메시지 톤 정정", "테스트 실행 중 상수 미스매치 발견" 같은 사실 |
| **변경 내역** | 파일:라인 단위. `before` → `after`만. 사유 줄거리 X (트리거에서 이미 다룸) |
| **검증** | 한 줄. `./gradlew build` 성공 / 응답 메시지가 변경됨 / 토스트 텍스트 갱신됨 |
| **영향** | 보통 N/A. 명세 갱신 트리거가 있으면 졸업 신호 (→ feature-story) |

---

## 예시

```
# [Fix] CARD_KEYWORD_MIN_REQUIRED 메시지 문구 통일 (2026-06-25)

## 대상 (원본)
- 원본 PR: #172 (User UserUpdateRequestDTO 정리)
- 본 fix 발견 시점: 2026-06-25 (이후 코드리뷰)

## 트리거
- 코드리뷰: "키워드는 최소 1개 이상이어야 합니다." 메시지가 `Card BC` 다른 ErrorCode 톤과 어긋남. "최소 1개의 키워드가 필요합니다."로 통일 요청.

## 변경 내역
- `src/main/java/com/example/thirdtool/Common/Exception/ErrorCode/ErrorCode.java:62` `"키워드는 최소 1개 이상이어야 합니다."` → `"최소 1개의 키워드가 필요합니다."`

## 검증
- `./gradlew test --tests "*CardControllerTest*"` → 메시지 assertion 갱신 후 그린

## 영향
- N/A (도메인 의미 동일, FE 토스트 텍스트만 변경)
```

---

## 참조

- 빈 스켈레톤: `./template.md`
- pes 대응 (planning): `../../../../pes/workspectrum/one-line-spec/version/0.0.1v/one-line-spec.md`
- 다음 fix 단계: `../../../feature-story/version/0.0.1v/feature-story.md`
- 양식 진화: SemVer 진화. 변경 시 `../0.0.2v/`에 새 버전
