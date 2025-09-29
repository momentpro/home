// 딜하르방 매장 데이터 관리 파일
// 새로운 매장 추가나 기존 매장 정보 수정은 이 파일에서만 하면 됩니다.

const STORE_DATA = [
    {
        id: 1,
        name: "그랜드 하얏트 제주",
        category: "hotel",
        lat: 33.2542,
        lng: 126.5603,
        discount: "40%",
        isHotdeal: true,
        description: "특급 호텔 특별 할인 혜택",
        phone: "064-741-1234",
        address: "서귀포시 중문관광로 114",
        deadline: "2025-09-30",
        originalPrice: "300,000원",
        discountPrice: "180,000원",
        image: "images/grand-hyatt-jeju.jpg",
        tags: ["특급호텔", "중문", "바다전망"],
        operatingHours: "24시간",
        website: "https://jeju.grand.hyatt.com"
    },
    {
        id: 2,
        name: "무지개 요트 투어",
        category: "activity",
        lat: 33.5012,
        lng: 126.5289,
        discount: "44%",
        isHotdeal: true,
        description: "제주 바다의 낭만, 요트 투어",
        phone: "064-742-5678",
        address: "제주시 한림읍 협재리",
        deadline: "2025-10-15",
        originalPrice: "80,000원",
        discountPrice: "45,000원",
        image: "images/mujigae-yacht.jpg",
        tags: ["요트투어", "협재해수욕장", "선셋투어"],
        operatingHours: "09:00 - 18:00",
        website: "#"
    },
    {
        id: 3,
        name: "제주 흑돼지 맛집",
        category: "restaurant",
        lat: 33.4988,
        lng: 126.5334,
        discount: "25%",
        isHotdeal: true,
        description: "정통 제주 흑돼지 전문점",
        phone: "064-743-9012",
        address: "제주시 일도2동 1312-1",
        deadline: "2025-10-31",
        originalPrice: "35,000원",
        discountPrice: "26,250원",
        image: "images/black-pork-restaurant.jpg",
        tags: ["흑돼지", "제주특산", "현지맛집"],
        operatingHours: "11:00 - 22:00",
        website: "#"
    },
    {
        id: 4,
        name: "감귤밭 체험농장",
        category: "activity",
        lat: 33.3617,
        lng: 126.5292,
        discount: "30%",
        isHotdeal: true,
        description: "감귤 따기 체험과 시식",
        phone: "064-744-3456",
        address: "서귀포시 남원읍 신례리",
        deadline: "2025-11-30",
        originalPrice: "15,000원",
        discountPrice: "10,500원",
        image: "images/tangerine-farm.jpg",
        tags: ["감귤따기", "체험농장", "가족여행"],
        operatingHours: "09:00 - 17:00",
        website: "#"
    },
    {
        id: 5,
        name: "제주 전통차 카페",
        category: "cafe",
        lat: 33.4996,
        lng: 126.5312,
        discount: "20%",
        isHotdeal: false,
        description: "제주 전통차와 디저트",
        phone: "064-745-7890",
        address: "제주시 건입동 1436",
        deadline: "2025-12-31",
        originalPrice: "12,000원",
        discountPrice: "9,600원",
        image: "images/traditional-tea-cafe.jpg",
        tags: ["전통차", "디저트", "힐링카페"],
        operatingHours: "10:00 - 21:00",
        website: "#"
    },
    {
        id: 6,
        name: "제주 기념품 전문점",
        category: "shopping",
        lat: 33.5020,
        lng: 126.5200,
        discount: "15%",
        isHotdeal: false,
        description: "제주 특산품과 기념품",
        phone: "064-746-1234",
        address: "제주시 연동 1494-2",
        deadline: "2025-12-31",
        originalPrice: "50,000원",
        discountPrice: "42,500원",
        image: "images/souvenir-shop.jpg",
        tags: ["기념품", "특산품", "선물"],
        operatingHours: "09:00 - 20:00",
        website: "#"
    }
];

// 매장 데이터 관리 함수들
const StoreManager = {
    // 모든 매장 데이터 가져오기
    getAll: () => STORE_DATA,

    // ID로 매장 찾기
    getById: (id) => STORE_DATA.find(store => store.id === id),

    // 카테고리별 매장 필터링
    getByCategory: (category) => {
        if (category === 'all') return STORE_DATA;
        return STORE_DATA.filter(store => store.category === category);
    },

    // 핫딜 매장만 가져오기
    getHotdeals: (category = 'all') => {
        const stores = category === 'all' ? STORE_DATA : STORE_DATA.filter(store => store.category === category);
        return stores.filter(store => store.isHotdeal);
    },

    // 검색 기능
    search: (query, category = 'all') => {
        const stores = category === 'all' ? STORE_DATA : STORE_DATA.filter(store => store.category === category);
        const searchTerm = query.toLowerCase();
        
        return stores.filter(store => 
            store.name.toLowerCase().includes(searchTerm) ||
            store.description.toLowerCase().includes(searchTerm) ||
            store.address.toLowerCase().includes(searchTerm) ||
            store.tags.some(tag => tag.toLowerCase().includes(searchTerm))
        );
    },

    // D-Day 계산
    getDDay: (deadline) => {
        const now = new Date();
        const deadlineDate = new Date(deadline + 'T23:59:59');
        const diffTime = deadlineDate - now;
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        
        if (diffDays > 0) {
            return { text: `D-${diffDays}`, days: diffDays };
        } else if (diffDays === 0) {
            return { text: 'D-DAY', days: 0 };
        } else {
            return { text: '마감', days: -1 };
        }
    },

    // D-Day 클래스 가져오기 (애니메이션용)
    getDDayClass: (days) => {
        if (days <= 0) return '';
        if (days <= 7) return 'urgent';
        if (days <= 14) return 'warning';
        return 'normal';
    }
};

// 전역으로 사용할 수 있도록 export
window.DEALHARUBANG_STORES = STORE_DATA;
window.StoreManager = StoreManager;
