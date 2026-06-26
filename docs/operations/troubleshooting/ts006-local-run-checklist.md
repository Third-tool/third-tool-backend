# ts006: 로컬 실행 종합 체크리스트 — 환경변수·service 자격증명·자주 보는 부팅 에러

`./gradlew bootRun` 또는 IDE Run으로 dev/local profile 부팅 시 **반드시 준비해야 할 환경변수**와 **service 자격증명 발급 절차**, **자주 보는 service 에러 7건**을 한 곳에 모았다.

ts002(환경변수 export 방법)·ts003(profile 활성화)·ts005(Dockerfile)와 cross-link.

---

## 1. 필수 환경변수 체크리스트

profile별 필요 환경변수:

| 환경변수 | dev | prod | local override | 발급 출처 |
|---|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | (default `dev,local`) | `prod` 명시 | — | 본인이 결정 |
| `JWT_SECRET_KEY` | **필수** | **필수** | dev/prod 동일 | 본인 생성 (32자 이상 임의 문자열) |
| `KAKAO_CLIENT_ID` | **필수** | **필수** | dev=localhost callback, prod=도메인 callback | [카카오 developers 콘솔](#21-카카오-developers) |
| `KAKAO_CLIENT_SECRET` | **필수** | **필수** | 동일 | 동일 |
| `NAVER_CLIENT_ID` | **필수** | **필수** | 동일 | [네이버 developers 콘솔](#22-네이버-developers) |
| `NAVER_CLIENT_SECRET` | **필수** | **필수** | 동일 | 동일 |
| `AWS_ACCESS_KEY_ID` | **필수** | **필수** | 동일 (또는 IAM Role) | [AWS IAM 사용자](#23-aws-iam-사용자-s3-업로드용) |
| `AWS_SECRET_ACCESS_KEY` | **필수** | **필수** | 동일 | 동일 |
| `DB_HOST` | — | **필수** | — | RDS endpoint 또는 로컬 MySQL `localhost` |
| `DB_PORT` | — | (default 3306) | — | 동일 |
| `DB_NAME` | — | **필수** | — | RDS schema 이름 |
| `DB_USERNAME` | — | **필수** | — | RDS user |
| `DB_PASSWORD` | — | **필수** | — | RDS password |

**가장 흔한 실수**: dev profile 활성화 시 DB 환경변수는 불필요(H2 인메모리), 그러나 OAuth/AWS 환경변수는 필수.

---

## 2. Service 자격증명 발급 절차

### 2.1 카카오 developers

1. https://developers.kakao.com 로그인
2. **내 애플리케이션** → 애플리케이션 추가
3. **앱 설정** → **앱 키** 메뉴
   - `KAKAO_CLIENT_ID` = **REST API 키**
4. **제품 설정** → **카카오 로그인** → 활성화 ON
   - **Redirect URI** 등록:
     - dev: `http://localhost:5173/oauth/kakao/callback`
     - prod: `https://thirdstool.com/oauth/kakao/callback`
5. **보안** → **Client Secret** 생성 → `KAKAO_CLIENT_SECRET`
6. **동의항목** 설정 — `profile_nickname`, `account_email` 필수 동의로 등록

### 2.2 네이버 developers

1. https://developers.naver.com 로그인
2. **Application** → **애플리케이션 등록**
3. 사용 API: **네이버 로그인** 선택
4. **Callback URL** 등록:
   - dev: `http://localhost:5173/oauth/naver/callback`
   - prod: `https://thirdstool.com/oauth/naver/callback`
5. 등록 완료 후:
   - `NAVER_CLIENT_ID` = **Client ID**
   - `NAVER_CLIENT_SECRET` = **Client Secret**
6. **제공 정보** 설정 — 이메일·별명·프로필 사진 등 필수 항목 체크

### 2.3 AWS IAM 사용자 (S3 업로드용)

1. AWS 콘솔 → **IAM** → **사용자** → 사용자 생성
2. 사용자 이름: `third-tool-s3-uploader` (예시)
3. 권한 정책: **S3 전용 IAM 정책** 부여 — 최소 권한 원칙
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Sid": "S3BucketReadWrite",
         "Effect": "Allow",
         "Action": [
           "s3:PutObject",
           "s3:GetObject",
           "s3:DeleteObject",
           "s3:ListBucket"
         ],
         "Resource": [
           "arn:aws:s3:::third-tool-s3-server",
           "arn:aws:s3:::third-tool-s3-server/*"
         ]
       }
     ]
   }
   ```
4. **보안 자격증명** → **액세스 키** 생성 → **사용 사례: AWS 외부에서 실행되는 애플리케이션**
5. 발급 완료:
   - `AWS_ACCESS_KEY_ID` = Access Key ID
   - `AWS_SECRET_ACCESS_KEY` = Secret Access Key (1회만 표시 — 안전한 곳에 저장)
6. **S3 버킷 생성** (사전): `third-tool-s3-server` 버킷 + `ap-northeast-2` 리전 + CORS 설정

### 2.4 JWT_SECRET_KEY (본인 생성)

암호학적 안전한 32자 이상 임의 문자열. HS256 알고리즘 사용.

```bash
# Linux/Mac
openssl rand -base64 48

# PowerShell
[Convert]::ToBase64String([byte[]](1..48 | %{Get-Random -Maximum 256}))

# Node.js
node -e "console.log(require('crypto').randomBytes(48).toString('base64'))"
```

**주의**: dev와 prod는 **다른 키** 사용. dev key로 발급된 토큰이 prod에서 통하면 보안 사고.

---

## 3. 환경변수 설정 방법

### 3.1 IntelliJ Run Configuration (권장)

`Run/Debug Configurations` → `ThirdToolApplication` → **Environment variables**:

```
JWT_SECRET_KEY=<openssl로 생성한 32자+ 키>
KAKAO_CLIENT_ID=<카카오 REST API 키>
KAKAO_CLIENT_SECRET=<카카오 Client Secret>
NAVER_CLIENT_ID=<네이버 Client ID>
NAVER_CLIENT_SECRET=<네이버 Client Secret>
AWS_ACCESS_KEY_ID=<IAM access key>
AWS_SECRET_ACCESS_KEY=<IAM secret>
```

profile은 비워두면 `application.yml` default `dev,local` 사용. prod 시뮬레이션 시 `SPRING_PROFILES_ACTIVE=prod,local` + DB 환경변수 추가.

### 3.2 CLI export

```bash
# bash/zsh
export JWT_SECRET_KEY="$(openssl rand -base64 48)"
export KAKAO_CLIENT_ID=...
# ... (반복)
./gradlew bootRun

# PowerShell
$env:JWT_SECRET_KEY = "..."
$env:KAKAO_CLIENT_ID = "..."
./gradlew bootRun
```

### 3.3 `.env` 파일 + direnv (선택)

프로젝트 루트 `.env`에 변수 정의 후 `direnv allow`. `.env`는 `.gitignore` 대상.

---

## 4. 자주 보는 service 에러 + 해결

### ts006-1: `Could not resolve placeholder 'JWT_SECRET_KEY' in value "${JWT_SECRET_KEY}"`

**원인**: 환경변수 미설정. 가장 흔한 부팅 실패.

**해결**: §3 환경변수 설정 → IDE Run Config 갱신 후 재시작. CLI는 `export` 후 같은 shell에서 `./gradlew bootRun`.

**검증**: `echo $JWT_SECRET_KEY` (bash) 또는 `$env:JWT_SECRET_KEY` (PowerShell)로 값 출력 확인.

### ts006-2: `BeanInstantiationException ... Factory method 'webClient' threw exception` (KAKAO/NAVER)

**원인**: `kakao.client-id` 또는 `naver.client-id` placeholder 해석 실패. 환경변수 누락.

**해결**: §2.1, §2.2 카카오/네이버 developers에서 발급 후 환경변수 설정.

### ts006-3: `software.amazon.awssdk.services.s3.model.S3Exception: The AWS Access Key Id you provided does not exist in our records (Service: S3, Status Code: 403, Request ID: ...)`

**원인**: AWS IAM 키 만료·삭제·오타. 또는 GitHub Secret Scanning이 키를 revoke (이전 PR에서 발생 가능성).

**해결**:
1. AWS 콘솔 → IAM → 사용자 → **액세스 키 발급 상태 확인** (활성/비활성)
2. 비활성/삭제됐다면 새 키 발급 → §2.3 가이드
3. 새 키를 IDE Run Config / `.env` / CLI export로 갱신
4. 앱 재시작

### ts006-4: `software.amazon.awssdk.services.s3.model.NoSuchBucketException: The specified bucket does not exist`

**원인**: S3 버킷이 존재 안 함 또는 다른 region에 있음. 본 프로젝트 default 버킷: `third-tool-s3-server` (ap-northeast-2).

**해결**:
1. AWS 콘솔 → S3 → 버킷 목록 확인
2. `third-tool-s3-server` 없으면 생성 (리전: 서울)
3. 또는 본인 버킷을 사용하려면 `cloud.aws.s3.bucket` 값을 `application-dev.yml` (또는 prod)에서 변경 (현재는 hardcoded)

### ts006-5: `Communications link failure` / `Connection refused` (prod profile)

**원인**: prod profile 활성화했는데 `DB_HOST` 등 RDS 환경변수 미설정 또는 RDS 인스턴스 미가동.

**해결**:
- 로컬에서 prod 시뮬레이션이라면 로컬 MySQL을 띄우기 (`docker run -p 3306:3306 -e MYSQL_ROOT_PASSWORD=... mysql:8`) + `DB_HOST=localhost` 등 설정
- 또는 prod 시뮬레이션 미의도라면 `SPRING_PROFILES_ACTIVE=dev,local`로 변경 (H2 인메모리)

### ts006-6: `org.springframework.web.reactive.function.client.WebClientResponseException$Unauthorized: 401 Unauthorized` (카카오 token 발급 실패)

**원인**:
- 카카오 developers의 **Client Secret 미활성화** 또는 코드와 불일치
- 또는 **Redirect URI 미일치** (등록한 URI와 실제 호출 URI가 다름)

**해결**:
1. 카카오 developers → **보안** → Client Secret이 **사용 ON** 상태인지
2. **카카오 로그인 → Redirect URI**에 `http://localhost:5173/oauth/kakao/callback` (dev)가 정확히 등록되어 있는지
3. 등록 후 5-10분 대기 (캐시 갱신)

### ts006-7: Swagger UI 404 또는 부팅 후 endpoint 미응답

**원인**:
- prod profile 활성화: `springdoc.swagger-ui.enabled: false`로 의도적 비활성화
- 또는 SecurityConfig의 AUTH_ALLOWLIST에서 `/swagger-ui/**` 제외 (현재는 포함)

**해결**:
- dev profile에서 접근: `http://localhost:8080/swagger-ui.html`
- prod에서 의도적 차단 — 비활성화 정상

---

## 5. 최소 부팅 검증 절차

```bash
# 1. 환경변수 8종 export (위 §3 참조)

# 2. 부팅
./gradlew bootRun

# 3. 부팅 로그 확인 — "Started ThirdToolApplication in N.NNN seconds" 출력 확인

# 4. health 확인
curl -s localhost:8080/health
# 기대: 200 OK + JSON 응답

# 5. Swagger UI 접속 (dev profile)
# 브라우저: http://localhost:8080/swagger-ui.html
```

부팅 실패 시 로그의 마지막 `Caused by:` 라인을 ts006-1 ~ ts006-7과 매칭해 원인 확인.

---

## 6. 도움 안 될 때 — 진단 정보 수집

부팅 에러를 트러블슈팅 issue로 제출 시 다음 정보 첨부:

```bash
# 1. 환경변수 set 여부 확인 (값은 가리고 length만)
echo "JWT_SECRET_KEY length: ${#JWT_SECRET_KEY}"
echo "KAKAO_CLIENT_ID length: ${#KAKAO_CLIENT_ID}"
echo "KAKAO_CLIENT_SECRET length: ${#KAKAO_CLIENT_SECRET}"
echo "NAVER_CLIENT_ID length: ${#NAVER_CLIENT_ID}"
echo "NAVER_CLIENT_SECRET length: ${#NAVER_CLIENT_SECRET}"
echo "AWS_ACCESS_KEY_ID length: ${#AWS_ACCESS_KEY_ID}"
echo "AWS_SECRET_ACCESS_KEY length: ${#AWS_SECRET_ACCESS_KEY}"
echo "SPRING_PROFILES_ACTIVE: $SPRING_PROFILES_ACTIVE"

# 2. 빌드 + 부팅 (stderr 캡처)
./gradlew bootRun --info 2>&1 | tee bootrun.log

# 3. bootrun.log에서 마지막 50줄 추출
tail -50 bootrun.log
```

길이가 0이면 환경변수 미설정 — §3.1 IDE Run Config 또는 §3.2 CLI export 점검.

---

## 관련

- [`ts002-environment-variables.md`](ts002-environment-variables.md) — 환경변수 export 패턴 (IntelliJ/CLI/direnv)
- [`ts003-profile-split.md`](ts003-profile-split.md) — profile 활성화 시나리오
- [`ts005-dockerfile-build.md`](ts005-dockerfile-build.md) — Docker 빌드·실행
- 후속: AWS Secrets Manager 마이그레이션 시 본 ts 일부 (자격증명 발급) 그대로 보존, 환경변수 export 단계는 자동 fetch로 대체 (Product 7)
