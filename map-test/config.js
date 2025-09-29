// 딜하르방 지도 설정 파일
// 이 파일에서 모든 설정을 관리합니다.

const CONFIG = {
    // 지도 설정
    map: {
        center: {
            lat: 33.3617,  // 제주도 중심
            lng: 126.5292
        },
        zoom: 10,
        minZoom: 8,
        maxZoom: 18
    },

    // 디자인 테마 (쉽게 변경 가능)
    theme: {
        colors: {
            primary: '#005A9C',        // 메인 브랜드 색상
            secondary: '#F7C852',      // 강조 색상 (노란색)
            background: '#F9F9F9',     // 배경색
            textDark: '#333333',       // 진한 텍스트
            textLight: '#767676',      // 연한 텍스트
            cardBackground: '#FFFFFF', // 카드 배경
            success: '#27ae60',        // 성공 색상
            warning: '#f39c12',        // 경고 색상
            danger: '#e74c3c'          // 위험 색상
        },
        
        fonts: {
            main: "'Pretendard', sans-serif",
            sizes: {
                xs: '12px',
                sm: '14px',
                md: '16px',
                lg: '18px',
                xl: '20px',
                xxl: '24px',
                huge: '28px'
            }
        },

        layout: {
            sidebarWidth: '380px',
            borderRadius: '18px',
            cardPadding: '18px',
            spacing: {
                xs: '4px',
                sm: '8px',
                md: '12px',
                lg: '16px',
                xl: '20px',
                xxl: '24px'
            }
        }
    },

    // 카테고리 설정 (쉽게 추가/제거 가능)
    categories: {
        all: { name: '전체', icon: '🏪', color: '#005A9C' },
        restaurant: { name: '음식점', icon: '🍽️', color: '#e74c3c' },
        cafe: { name: '카페', icon: '☕', color: '#8b4513' },
        shopping: { name: '쇼핑', icon: '🛍️', color: '#9b59b6' },
        hotel: { name: '숙박', icon: '🏨', color: '#3498db' },
        activity: { name: '체험', icon: '🎯', color: '#2ecc71' }
    },

    // 애니메이션 설정
    animations: {
        duration: {
            fast: '0.3s',
            normal: '0.5s',
            slow: '0.8s'
        },
        easing: 'cubic-bezier(0.175, 0.885, 0.32, 1.275)'
    },

    // 텍스트 설정 (쉽게 변경 가능)
    text: {
        title: '딜하르방',
        subtitle: '제주도에서 만나는 특별한 할인 혜택',
        searchPlaceholder: '매장명 또는 지역을 검색하세요...',
        categoryTitle: '카테고리',
        hotdealTitle: '🔥 현재 핫딜 매장',
        loadingMessage: '지도를 불러오는 중...',
        noResults: '해당 카테고리에 핫딜 매장이 없습니다.'
    },

    // API 설정 (새로운 통합 API 버전)
    api: {
        naver: {
            keyId: 'ynlmdjv2h1', // 네이버 지도 API 키 (ncpKeyId)
            submodules: ['geocoder'],
            baseUrl: 'https://oapi.map.naver.com/openapi/v3/maps.js'
        }
    },

    // 기능 설정
    features: {
        enableSearch: true,
        enableCategories: true,
        enableHotdeals: true,
        enableDDay: true,
        enableFloatingDecorations: true,
        enableAnimations: true
    }
};

// 설정을 전역으로 사용할 수 있도록 export
window.DEALHARUBANG_CONFIG = CONFIG;
