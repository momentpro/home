# 딜하르방 지도 버전

제주도민 전용 혜택을 지도 기반으로 제공하는 웹 애플리케이션입니다.

## 📁 파일 구조

```
map-test/
├── index.html          # 메인 HTML 파일
├── styles.css          # 스타일시트 (디자인 변경은 여기서)
├── config.js           # 설정 파일 (색상, 텍스트, 기능 설정)
├── data.js             # 매장 데이터 (새 매장 추가는 여기서)
├── app.js              # 메인 애플리케이션 로직
└── README.md           # 이 파일
```

## 🎨 디자인 변경 방법

### 1. 색상 변경
`config.js` 파일의 `theme.colors` 섹션에서 색상을 변경하세요:

```javascript
colors: {
    primary: '#005A9C',        // 메인 브랜드 색상
    secondary: '#F7C852',      // 강조 색상 (노란색)
    background: '#F9F9F9',     // 배경색
    // ...
}
```

### 2. 텍스트 변경
`config.js` 파일의 `text` 섹션에서 텍스트를 변경하세요:

```javascript
text: {
    title: '딜하르방',
    subtitle: '제주도에서 만나는 특별한 할인 혜택',
    // ...
}
```

### 3. 레이아웃 변경
`styles.css` 파일에서 CSS를 수정하여 레이아웃을 변경할 수 있습니다.

## 🏪 매장 데이터 관리

### 새 매장 추가
`data.js` 파일의 `STORE_DATA` 배열에 새로운 매장 객체를 추가하세요:

```javascript
{
    id: 7,                          // 고유 ID
    name: "새로운 매장",
    category: "restaurant",         // restaurant, cafe, shopping, hotel, activity
    lat: 33.4996,                  // 위도
    lng: 126.5312,                 // 경도
    discount: "30%",               // 할인율
    isHotdeal: true,               // 핫딜 여부
    description: "매장 설명",
    phone: "064-123-4567",
    address: "제주시 주소",
    deadline: "2025-12-31",        // 마감일 (YYYY-MM-DD)
    originalPrice: "20,000원",
    discountPrice: "14,000원",
    // ...
}
```

### 기존 매장 수정
`data.js` 파일에서 해당 매장의 정보를 직접 수정하면 됩니다.

## 🗺️ 네이버 지도 API 연동

현재는 임시 구현 상태입니다. 실제 지도를 사용하려면:

1. 네이버 클라우드 플랫폼에서 Maps API 키 발급
2. `config.js`의 `api.naver.clientId`에 발급받은 키 입력
3. `app.js`의 `createMap()` 함수에서 주석 처리된 실제 지도 코드 활성화

## 🔧 기능 설정

`config.js`의 `features` 섹션에서 기능을 켜고 끌 수 있습니다:

```javascript
features: {
    enableSearch: true,              // 검색 기능
    enableCategories: true,          // 카테고리 필터
    enableHotdeals: true,           // 핫딜 목록
    enableDDay: true,               // D-Day 표시
    enableFloatingDecorations: true, // 플로팅 장식
    enableAnimations: true          // 애니메이션
}
```

## 📱 반응형 디자인

- PC: 사이드바 + 지도 (좌우 분할)
- 모바일: 사이드바 + 지도 (상하 분할)

## 🚀 배포 방법

1. 네이버 Maps API 키 발급 및 설정
2. 모든 파일을 웹 서버에 업로드
3. `index.html`로 접속

## 🔍 디버깅

브라우저 개발자 도구 콘솔에서:

```javascript
// 현재 상태 확인
dealharubangMap.getStatus()

// 특정 매장 정보 확인
StoreManager.getById(1)

// 핫딜 매장만 확인
StoreManager.getHotdeals()
```

## 📝 주의사항

1. 매장 데이터 수정 후 브라우저 새로고침 필요
2. API 키 없이는 임시 지도만 표시됩니다
3. 모든 이미지 경로는 `images/` 폴더 기준입니다

## 🆘 문제 해결

### 지도가 표시되지 않을 때
- 네이버 Maps API 키가 올바르게 설정되었는지 확인
- 브라우저 콘솔에서 오류 메시지 확인

### 매장이 표시되지 않을 때
- `data.js` 파일의 매장 데이터 형식 확인
- 브라우저 개발자 도구에서 JavaScript 오류 확인

### 스타일이 적용되지 않을 때
- `styles.css` 파일 경로 확인
- CSS 문법 오류 확인
