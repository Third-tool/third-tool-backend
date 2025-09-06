# third-tool-backend
third tool backend 서버입니다.


# 두 팀이 동시에 작업하고 병합하는 방법

흔히 “동료 작업이 끝날 때까지 기다려야 한다”고 생각하기 쉽지만, **기다릴 필요가 없습니다.**  
두 명이 각자 브랜치에서 작업하고, 이후 병합하는 흐름은 아래와 같습니다.

---

## 실제 작업 흐름

### 1) 각자 브랜치 생성
- 당신은 `main`에서 `feature/내-작업` 브랜치를 만듭니다.  
- 동료는 `feature/동료-작업` 브랜치를 만들어 각자 작업합니다.

# you
git checkout main
git pull
git checkout -b feature/내-작업

# 동료
git checkout main
git pull
git checkout -b feature/동료-작업


2) 모두 동시에 작업

두 사람 모두 각자의 브랜치에서 커밋합니다. 서로에게 영향을 주지 않습니다.

3) 동료가 먼저 병합

동료가 먼저 작업을 완료하고 feature/동료-작업을 main으로 병합합니다.
이제 main에는 동료의 변경사항이 반영되어 있습니다.

4) 자신의 브랜치 동기화 (핵심)

본인의 feature/내-작업 브랜치에서 PR을 올리기 전에, 최신 main 변경사항을 가져와 반영합니다.


git checkout feature/내-작업
git pull origin main

5) 충돌 해결 후 PR 제출

충돌이 발생하면 해결 후 커밋합니다.
그다음 본인 브랜치를 푸시하고 Pull Request 를 생성합니다.
