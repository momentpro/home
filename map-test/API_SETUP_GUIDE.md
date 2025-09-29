# 네이버 지도 API 연동 가이드

딜하르방 지도 버전에서 실제 네이버 지도를 사용하기 위한 설정 가이드입니다.

## 🔑 1단계: 네이버 클라우드 플랫폼 회원가입

### 1.1 회원가입
1. [네이버 클라우드 플랫폼](https://www.ncloud.com) 접속
2. 회원가입 진행 (네이버 계정 사용 가능)
3. 본인인증 완료

### 1.2 결제 수단 등록
- **중요**: 무료 사용이어도 결제 수단 등록이 필수입니다
- 신용카드 또는 계좌 등록
- 무료 한도 내에서는 실제 결제되지 않습니다

## 🗺️ 2단계: Maps API 신청

### 2.1 콘솔 접속
1. 네이버 클라우드 플랫폼 콘솔 로그인
2. Services → AI·Application Service → Maps 선택

### 2.2 Application 등록
1. "Application 등록" 클릭
2. 애플리케이션 정보 입력:
   - **애플리케이션 이름**: 딜하르방 지도
   - **사용 목적**: 웹사이트 지도 서비스
   - **서비스 URL**: 실제 도메인 또는 localhost

### 2.3 Web Dynamic Map 선택
- Maps 서비스 중 "Web Dynamic Map" 선택
- 이는 JavaScript로 동적 지도를 구현하는 서비스입니다

### 2.4 Client ID 발급
- 등록 완료 후 **Client ID** 발급
- 이 ID를 복사해두세요 (예: `abc123def456`)

## ⚙️ 3단계: 프로젝트 설정

### 3.1 API 키 설정
`config.js` 파일을 열고 다음 부분을 수정하세요:

```javascript
// 현재 코드 (수정 전)
api: {
    naver: {
        clientId: 'YOUR_CLIENT_ID', // ← 이 부분을 수정
        submodules: ['geocoder']
    }
}

// 수정 후 (발급받은 ID로 변경)
api: {
    naver: {
        clientId: 'abc123def456', // ← 실제 발급받은 Client ID
        submodules: ['geocoder']
    }
}
```

### 3.2 index.html 파일 확인
네이버 지도 API 스크립트 태그가 올바른지 확인:

```html
<script type="text/javascript" src="https://openapi.map.naver.com/openapi/v3/maps.js?ncpClientId=YOUR_CLIENT_ID&submodules=geocoder"></script>
```

이 부분은 자동으로 `config.js`의 설정을 사용하도록 업데이트할 예정입니다.

## 📊 4단계: 무료 사용량 확인

### 4.1 무료 한도
- **Web Dynamic Map**: 월 100,000건 무료
- **Geocoding**: 월 10,000건 무료
- 딜하르방 규모로는 충분한 양입니다

### 4.2 사용량 모니터링
- 네이버 클라우드 플랫폼 콘솔에서 실시간 확인 가능
- 사용량이 90% 도달 시 알림 설정 권장

## 🚀 5단계: 테스트 및 확인

### 5.1 브라우저 테스트
1. 설정 완료 후 브라우저에서 `index.html` 열기
2. 콘솔에서 오류 메시지 확인
3. 실제 제주도 지도가 표시되는지 확인

### 5.2 기능 테스트
- ✅ 지도 표시
- ✅ 매장 마커 표시
- ✅ 마커 클릭 시 정보창
- ✅ 카테고리 필터링
- ✅ 검색 기능

## ❗ 주의사항

### 보안
- **Client ID는 공개되어도 상관없습니다** (Frontend 전용)
- 하지만 도메인 제한을 설정하는 것이 좋습니다

### 도메인 제한 설정 (권장)
1. 네이버 클라우드 플랫폼 콘솔에서 Application 설정 수정
2. "Web Service URL"에 실제 도메인만 등록
3. 예: `https://yourdomain.com`, `http://localhost`

### 비용 절약 팁
- 지도 로드를 최소화하세요
- 불필요한 API 호출 방지
- 캐싱 활용 권장

## 🔧 트러블슈팅

### 1. 지도가 표시되지 않을 때
```javascript
// 브라우저 콘솔에서 확인
console.log(typeof naver); // "object"가 나와야 함
console.log(naver.maps);   // Maps 객체가 나와야 함
```

### 2. "Unauthorized" 오류
- Client ID가 올바른지 확인
- 도메인 제한 설정 확인
- 결제 수단 등록 확인

### 3. 마커가 표시되지 않을 때
```javascript
// 매장 데이터 확인
console.log(StoreManager.getAll());

// 지도 객체 확인
console.log(dealharubangMap.map);
```

## 📞 지원

### 공식 문서
- [네이버 지도 API 가이드](https://navermaps.github.io/maps.js/)
- [네이버 클라우드 플랫폼 문서](https://guide.ncloud-docs.com/docs/naveropenapiv3-maps-overview)

### 개발자 커뮤니티
- 네이버 개발자 센터 Q&A
- Stack Overflow (naver-maps 태그)

---

## 💡 설정 완료 후 이점

✅ **실제 제주도 지도 표시**
✅ **정확한 매장 위치 마커**
✅ **상세한 지도 컨트롤**
✅ **확대/축소 및 지도 이동**
✅ **위성/거리뷰 전환**
✅ **모바일 터치 지원**

API 키 설정만 완료하면 즉시 실제 지도가 동작합니다! 🎯
