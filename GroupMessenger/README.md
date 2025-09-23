# Group Messenger - 안드로이드 단체 문자 발송 앱

## 📱 앱 소개
Group Messenger는 연락처에서 여러 명을 선택하여 한 번에 단체 문자를 발송할 수 있는 안드로이드 앱입니다.

## 🚀 주요 기능
- 📞 연락처 접근 및 관리
- 👥 연락처 그룹 생성/수정/삭제
- 📨 단체 문자 발송
- 🔐 권한 관리 (연락처, SMS)

## 🛠 기술 스택
- **언어**: Kotlin
- **UI**: Jetpack Compose
- **아키텍처**: MVVM
- **의존성 주입**: Hilt
- **데이터베이스**: Room
- **권한 관리**: Accompanist Permissions

## 📁 프로젝트 구조
```
app/src/main/java/com/example/groupmessenger/
├── ui/                     # UI 관련 코드
│   ├── screens/           # 화면들
│   ├── components/        # 재사용 가능한 컴포넌트
│   └── theme/            # 테마 관련
├── viewmodel/            # ViewModel들
├── data/                 # 데이터 관련
│   ├── model/           # 데이터 모델
│   └── repository/      # Repository
├── util/                # 유틸리티 클래스
└── navigation/          # 네비게이션
```

## 🏃‍♂️ 실행 방법

### 1. 개발 환경 설정
1. Android Studio 설치
2. Kotlin 플러그인 활성화
3. API Level 24 이상 설정

### 2. 프로젝트 실행
1. Android Studio에서 프로젝트 열기
2. Gradle Sync 실행
3. 에뮬레이터 실행 또는 실제 기기 연결
4. Run 버튼 클릭

### 3. 권한 설정
앱 실행 시 다음 권한들이 필요합니다:
- `READ_CONTACTS`: 연락처 읽기
- `SEND_SMS`: SMS 발송

## 📋 필수 요구사항
- Android API Level 24 이상
- Kotlin 1.8.0 이상
- Compose BOM 2023.10.01

## 🔧 빌드 설정
```gradle
compileSdk 34
minSdk 24
targetSdk 34
```

## 📝 사용법
1. 앱 실행 후 연락처 권한 허용
2. 연락처 목록에서 원하는 연락처들 선택
3. 메시지 작성 화면에서 내용 입력
4. 발송 버튼으로 단체 문자 전송

## 🚨 주의사항
- 실제 기기에서만 SMS 발송 기능이 정상 작동합니다
- 에뮬레이터에서는 UI 테스트만 가능합니다
- SMS 발송 시 통신사 요금이 발생할 수 있습니다

## 🤝 기여하기
1. Fork the Project
2. Create your Feature Branch
3. Commit your Changes
4. Push to the Branch
5. Open a Pull Request

## 📄 라이선스
이 프로젝트는 MIT 라이선스 하에 있습니다.









