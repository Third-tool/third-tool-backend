# third-tool-backend
third tool backend 서버입니다.


실제 작업 흐름
각자 브랜치 생성: 당신은 main 브랜치에서 feature/내-작업 브랜치를 만듭니다. 동시에 동료도 feature/동료-작업 브랜치를 만들고 각자 작업합니다.

Bash

# 당신
git checkout main
git pull
git checkout -b feature/내-작업

# 동료
git checkout main
git pull
git checkout -b feature/동료-작업
모두 동시에 작업: 두 명 모두 각자의 브랜치에서 작업하며 커밋합니다. 서로 영향을 주지 않습니다.

동료가 먼저 병합: 동료가 먼저 작업을 완료하고, feature/동료-작업 브랜치를 main 브랜치로 병합합니다. 이제 main 브랜치에 동료의 변경사항이 포함됩니다.

자신의 브랜치 동기화: 이 단계가 핵심입니다. 당신의 feature/내-작업 브랜치에서 Pull Request를 올리기 전에, main 브랜치의 최신 변경사항을 가져와서 자신의 브랜치에 반영해야 합니다.

Bash

git checkout feature/내-작업
git pull origin main
이 과정을 거치면 만약 동료와 같은 파일을 수정했더라도, Git이 충돌(conflict)을 알려줍니다.

충돌 해결 후 PR 제출: 충돌이 발생하면 충돌을 해결하고 커밋한 뒤, 당신의 브랜치를 푸시하고 Pull Request를 생성합니다.
