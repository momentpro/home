// 딜하르방 지도 메인 애플리케이션 파일
// 모든 기능은 이 파일에서 관리됩니다.

class DealharubangMap {
    constructor() {
        this.config = window.DEALHARUBANG_CONFIG;
        this.storeManager = window.StoreManager;
        this.map = null;
        this.markers = [];
        this.currentCategory = 'all';
        this.searchQuery = '';
        
        this.init();
    }

    // 애플리케이션 초기화
    init() {
        console.log('🗿 딜하르방 지도 애플리케이션 시작!');
        
        // DOM이 로드되면 실행
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.onDOMReady());
        } else {
            this.onDOMReady();
        }
    }

    // DOM 준비 완료 시 실행
    onDOMReady() {
        this.renderCategories();
        this.renderHotdeals();
        this.setupEventListeners();
        this.initMap();
        
        console.log('✅ 딜하르방 지도 페이지가 성공적으로 로드되었습니다!');
    }

    // 카테고리 버튼 렌더링
    renderCategories() {
        const container = document.getElementById('categoryButtons');
        if (!container) return;

        container.innerHTML = Object.entries(this.config.categories)
            .map(([key, category]) => `
                <button 
                    class="category-btn ${key === 'all' ? 'active' : ''}" 
                    data-category="${key}"
                    title="${category.name}"
                >
                    ${category.icon} ${category.name}
                </button>
            `).join('');
    }

    // 핫딜 목록 렌더링
    renderHotdeals() {
        const container = document.getElementById('hotdealList');
        if (!container) return;

        let hotdeals;
        
        if (this.searchQuery) {
            hotdeals = this.storeManager.search(this.searchQuery, this.currentCategory)
                .filter(store => store.isHotdeal);
        } else {
            hotdeals = this.storeManager.getHotdeals(this.currentCategory);
        }

        if (hotdeals.length === 0) {
            container.innerHTML = `
                <div style="text-align: center; color: var(--text-light); padding: 20px;">
                    ${this.config.text.noResults}
                </div>
            `;
            return;
        }

        container.innerHTML = hotdeals.map((store, index) => {
            const dday = this.storeManager.getDDay(store.deadline);
            const ddayClass = this.storeManager.getDDayClass(dday.days);
            
            return `
                <div class="hotdeal-item" onclick="dealharubangMap.focusStore(${store.id})" style="animation-delay: ${index * 0.1}s">
                    ${dday.days >= 0 ? `<div class="dday-badge ${ddayClass}">${dday.text}</div>` : ''}
                    <div class="hotdeal-name">${store.name}</div>
                    <div class="hotdeal-discount">최대 ${store.discount} 할인</div>
                    <div class="hotdeal-category">
                        ${this.config.categories[store.category]?.icon || '🏪'} 
                        ${this.config.categories[store.category]?.name || store.category}
                    </div>
                </div>
            `;
        }).join('');
    }

    // 이벤트 리스너 설정
    setupEventListeners() {
        // 카테고리 버튼 클릭
        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('category-btn')) {
                this.handleCategoryChange(e.target);
            }
        });

        // 검색 입력
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.handleSearch(e.target.value);
            });
        }

        // 키보드 접근성
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && e.target.classList.contains('category-btn')) {
                e.target.click();
            }
        });
    }

    // 카테고리 변경 처리
    handleCategoryChange(button) {
        // 이전 active 클래스 제거
        document.querySelectorAll('.category-btn').forEach(btn => 
            btn.classList.remove('active')
        );
        
        // 현재 버튼에 active 클래스 추가
        button.classList.add('active');
        
        // 카테고리 업데이트
        this.currentCategory = button.dataset.category;
        
        // 핫딜 목록 다시 렌더링
        this.renderHotdeals();
        
        // 지도 마커 업데이트 (추후 구현)
        this.updateMapMarkers();
    }

    // 검색 처리
    handleSearch(query) {
        this.searchQuery = query.trim();
        this.renderHotdeals();
        // 지도 마커 업데이트 (추후 구현)
        this.updateMapMarkers();
    }

    // 지도 초기화
    initMap() {
        // 로딩 스피너 표시 시간 시뮬레이션
        setTimeout(() => {
            this.createMap();
        }, 1500);
    }

    // 지도 생성
    createMap() {
        const mapDiv = document.getElementById('map');
        if (!mapDiv) return;

        // 네이버 지도 API 사용 가능한지 확인
        if (typeof naver !== 'undefined' && naver.maps) {
            this.createNaverMap();
        } else {
            this.createFallbackMap();
        }
    }

    // 실제 네이버 지도 생성
    createNaverMap() {
        console.log('🗺️ 네이버 지도 API 로드 완료, 실제 지도를 생성합니다.');
        
        try {
            // 지도 생성
            this.map = new naver.maps.Map('map', {
                center: new naver.maps.LatLng(this.config.map.center.lat, this.config.map.center.lng),
                zoom: this.config.map.zoom,
                minZoom: this.config.map.minZoom,
                maxZoom: this.config.map.maxZoom,
                mapTypeControl: true,
                mapTypeControlOptions: {
                    style: naver.maps.MapTypeControlStyle.BUTTON,
                    position: naver.maps.Position.TOP_RIGHT
                },
                zoomControl: true,
                zoomControlOptions: {
                    style: naver.maps.ZoomControlStyle.LARGE,
                    position: naver.maps.Position.TOP_LEFT
                },
                scaleControl: false,
                logoControl: false,
                mapDataControl: false
            });

            // 지도 로드 완료 후 마커 추가
            naver.maps.Event.addListener(this.map, 'idle', () => {
                console.log('✅ 지도 로드 완료, 매장 마커를 추가합니다.');
                this.addMarkers();
                this.hideLoadingSpinner();
            });

        } catch (error) {
            console.error('❌ 네이버 지도 생성 오류:', error);
            this.createFallbackMap();
        }
    }

    // 대체 지도 (API 키가 없거나 오류 시)
    createFallbackMap() {
        console.log('🔄 네이버 지도 API를 사용할 수 없어 대체 화면을 표시합니다.');
        
        const mapDiv = document.getElementById('map');
        const hotdealCount = this.storeManager.getHotdeals().length;
        const totalStores = this.storeManager.getAll().length;
        
        mapDiv.innerHTML = `
            <div style="
                width: 100%; 
                height: 100%; 
                background: linear-gradient(45deg, #e3f2fd 0%, #bbdefb 100%);
                display: flex;
                align-items: center;
                justify-content: center;
                flex-direction: column;
                color: #1976d2;
                font-size: 18px;
                font-weight: 600;
                position: relative;
                overflow: hidden;
            ">
                <div style="position: absolute; top: 20px; left: 20px; background: rgba(255,255,255,0.9); padding: 15px; border-radius: 10px; font-size: 14px;">
                    <div style="font-weight: 700; margin-bottom: 5px; color: #e74c3c;">⚠️ API 키 필요</div>
                    <div>전체 매장: ${totalStores}개</div>
                    <div style="color: #e74c3c; font-weight: 700;">핫딜 매장: ${hotdealCount}개</div>
                </div>

                <div style="position: absolute; top: 20px; right: 20px; background: rgba(255,255,255,0.9); padding: 15px; border-radius: 10px; font-size: 12px; max-width: 250px;">
                    <div style="font-weight: 700; margin-bottom: 8px; color: #2c3e50;">🔧 API 키 설정 방법</div>
                    <div style="line-height: 1.4; color: #34495e;">
                        1. 네이버 클라우드 플랫폼 가입<br>
                        2. Maps API 신청<br>
                        3. config.js에 clientId 입력
                    </div>
                </div>
                
                <div style="font-size: 64px; margin-bottom: 20px; animation: float 3s ease-in-out infinite;">🗺️</div>
                <div style="margin-bottom: 10px;">네이버 지도 API 연동 준비 완료</div>
                <div style="font-size: 14px; opacity: 0.7; margin-bottom: 20px;">
                    API 키 설정 후 실제 제주도 지도가 표시됩니다
                </div>

                <!-- 가상 매장 위치 표시 -->
                <div style="position: absolute; top: 30%; left: 25%; background: rgba(231, 76, 60, 0.9); color: white; padding: 8px 12px; border-radius: 20px; font-size: 12px; font-weight: 700;">
                    🔥 그랜드 하얏트
                </div>
                <div style="position: absolute; top: 45%; left: 35%; background: rgba(52, 152, 219, 0.9); color: white; padding: 8px 12px; border-radius: 20px; font-size: 12px; font-weight: 700;">
                    🍽️ 흑돼지 맛집
                </div>
                <div style="position: absolute; top: 55%; right: 30%; background: rgba(46, 204, 113, 0.9); color: white; padding: 8px 12px; border-radius: 20px; font-size: 12px; font-weight: 700;">
                    🎯 무지개 요트
                </div>
                <div style="position: absolute; bottom: 35%; left: 40%; background: rgba(155, 89, 182, 0.9); color: white; padding: 8px 12px; border-radius: 20px; font-size: 12px; font-weight: 700;">
                    ☕ 전통차 카페
                </div>
                
                <div style="display: flex; gap: 15px; flex-wrap: wrap; justify-content: center; margin-top: 20px;">
                    ${Object.entries(this.config.categories).slice(1).map(([key, category]) => `
                        <div style="
                            background: rgba(255,255,255,0.8); 
                            padding: 10px 15px; 
                            border-radius: 20px; 
                            font-size: 12px;
                            border: 2px solid ${category.color};
                            color: ${category.color};
                            font-weight: 600;
                        ">
                            ${category.icon} ${category.name}
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
        
        this.hideLoadingSpinner();
    }

    // 로딩 스피너 숨기기
    hideLoadingSpinner() {
        const loadingSpinner = document.getElementById('loadingSpinner');
        if (loadingSpinner) {
            loadingSpinner.style.display = 'none';
        }
    }

    // 매장 마커 추가
    addMarkers() {
        if (!this.map) return;

        // 기존 마커 제거
        this.clearMarkers();

        // 현재 필터에 맞는 매장들 가져오기
        let stores;
        if (this.searchQuery) {
            stores = this.storeManager.search(this.searchQuery, this.currentCategory);
        } else {
            stores = this.storeManager.getByCategory(this.currentCategory);
        }

        console.log(`📍 ${stores.length}개 매장의 마커를 추가합니다.`);

        // 각 매장에 마커 추가
        stores.forEach(store => {
            this.createStoreMarker(store);
        });
    }

    // 개별 매장 마커 생성
    createStoreMarker(store) {
        if (!this.map) return;

        const position = new naver.maps.LatLng(store.lat, store.lng);
        const categoryInfo = this.config.categories[store.category] || this.config.categories.all;
        
        // 핫딜 여부에 따른 마커 스타일
        const isHotdeal = store.isHotdeal;
        const dday = this.storeManager.getDDay(store.deadline);
        
        // 커스텀 마커 HTML 생성
        const markerContent = this.createMarkerHTML(store, categoryInfo, isHotdeal, dday);
        
        // 마커 생성
        const marker = new naver.maps.Marker({
            position: position,
            map: this.map,
            title: store.name,
            icon: {
                content: markerContent,
                size: new naver.maps.Size(isHotdeal ? 60 : 50, isHotdeal ? 80 : 70),
                anchor: new naver.maps.Point(isHotdeal ? 30 : 25, isHotdeal ? 80 : 70)
            },
            zIndex: isHotdeal ? 100 : 50
        });

        // 마커 클릭 이벤트
        naver.maps.Event.addListener(marker, 'click', () => {
            this.showStoreInfoWindow(store, marker);
        });

        // 마커 저장
        this.markers.push({
            marker: marker,
            store: store
        });
    }

    // 마커 HTML 생성
    createMarkerHTML(store, categoryInfo, isHotdeal, dday) {
        const bgColor = isHotdeal ? '#e74c3c' : categoryInfo.color;
        const size = isHotdeal ? '50px' : '40px';
        const iconSize = isHotdeal ? '20px' : '16px';
        
        return `
            <div style="
                position: relative;
                background: ${bgColor};
                color: white;
                border-radius: 50% 50% 50% 0;
                width: ${size};
                height: ${size};
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: ${iconSize};
                font-weight: 700;
                box-shadow: 0 4px 12px rgba(0,0,0,0.3);
                cursor: pointer;
                transition: all 0.3s ease;
                transform-origin: bottom center;
                ${isHotdeal ? 'animation: bounce 2s ease-in-out infinite;' : ''}
            " 
            onmouseover="this.style.transform='scale(1.1)'" 
            onmouseout="this.style.transform='scale(1)'">
                ${isHotdeal ? '🔥' : categoryInfo.icon}
                ${isHotdeal && dday.days >= 0 ? `
                    <div style="
                        position: absolute;
                        top: -8px;
                        right: -8px;
                        background: #fff;
                        color: #e74c3c;
                        border-radius: 10px;
                        padding: 2px 6px;
                        font-size: 10px;
                        font-weight: 700;
                        box-shadow: 0 2px 6px rgba(0,0,0,0.2);
                    ">${dday.text}</div>
                ` : ''}
            </div>
        `;
    }

    // 매장 정보창 표시
    showStoreInfoWindow(store, marker) {
        // 기존 정보창 닫기
        if (this.currentInfoWindow) {
            this.currentInfoWindow.close();
        }

        const dday = this.storeManager.getDDay(store.deadline);
        const categoryInfo = this.config.categories[store.category] || this.config.categories.all;

        // 정보창 내용 생성
        const content = `
            <div style="
                padding: 20px;
                min-width: 300px;
                max-width: 350px;
                font-family: 'Pretendard', sans-serif;
                line-height: 1.5;
            ">
                <div style="
                    display: flex;
                    align-items: center;
                    margin-bottom: 12px;
                    padding-bottom: 12px;
                    border-bottom: 2px solid #eee;
                ">
                    <div style="
                        background: ${categoryInfo.color};
                        color: white;
                        width: 40px;
                        height: 40px;
                        border-radius: 50%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        font-size: 18px;
                        margin-right: 12px;
                    ">${categoryInfo.icon}</div>
                    <div>
                        <h3 style="
                            font-size: 18px;
                            font-weight: 700;
                            color: #333;
                            margin: 0;
                        ">${store.name}</h3>
                        <div style="
                            font-size: 12px;
                            color: #666;
                            margin-top: 2px;
                        ">${categoryInfo.name}</div>
                    </div>
                    ${store.isHotdeal ? `
                        <div style="
                            background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
                            color: white;
                            padding: 4px 8px;
                            border-radius: 12px;
                            font-size: 10px;
                            font-weight: 700;
                            margin-left: auto;
                        ">${dday.text}</div>
                    ` : ''}
                </div>

                <div style="margin-bottom: 12px;">
                    <div style="color: #666; font-size: 14px; margin-bottom: 8px;">
                        💰 <strong style="color: #e74c3c; font-size: 16px;">${store.discount} 할인</strong>
                    </div>
                    <div style="font-size: 13px; color: #666;">
                        <span style="text-decoration: line-through;">${store.originalPrice}</span>
                        → <strong style="color: #e74c3c;">${store.discountPrice}</strong>
                    </div>
                </div>

                <div style="margin-bottom: 12px; color: #555; font-size: 14px;">
                    ${store.description}
                </div>

                <div style="font-size: 13px; color: #666; line-height: 1.6;">
                    <div style="margin-bottom: 4px;">📍 ${store.address}</div>
                    <div style="margin-bottom: 4px;">📞 ${store.phone}</div>
                    <div style="margin-bottom: 8px;">🕒 ${store.operatingHours}</div>
                </div>

                <div style="
                    display: flex;
                    gap: 8px;
                    flex-wrap: wrap;
                    margin-bottom: 12px;
                ">
                    ${store.tags.map(tag => `
                        <span style="
                            background: #f8f9fa;
                            color: #495057;
                            padding: 4px 8px;
                            border-radius: 12px;
                            font-size: 11px;
                            border: 1px solid #dee2e6;
                        ">${tag}</span>
                    `).join('')}
                </div>

                <div style="text-align: center; padding-top: 12px; border-top: 1px solid #eee;">
                    <button onclick="dealharubangMap.focusStore(${store.id})" style="
                        background: linear-gradient(135deg, var(--primary-brand) 0%, #0066b3 100%);
                        color: white;
                        border: none;
                        padding: 10px 20px;
                        border-radius: 20px;
                        font-size: 14px;
                        font-weight: 600;
                        cursor: pointer;
                        transition: all 0.3s ease;
                    " onmouseover="this.style.transform='scale(1.05)'" onmouseout="this.style.transform='scale(1)'">
                        🎯 상세 정보 보기
                    </button>
                </div>
            </div>
        `;

        // 정보창 생성
        this.currentInfoWindow = new naver.maps.InfoWindow({
            content: content,
            backgroundColor: "#fff",
            borderColor: "#ccc",
            borderWidth: 1,
            anchorSize: new naver.maps.Size(20, 20),
            anchorSkew: true,
            anchorColor: "#fff",
            pixelOffset: new naver.maps.Point(0, -10)
        });

        // 정보창 열기
        this.currentInfoWindow.open(this.map, marker.getPosition());
    }

    // 기존 마커 제거
    clearMarkers() {
        this.markers.forEach(item => {
            item.marker.setMap(null);
        });
        this.markers = [];
        
        if (this.currentInfoWindow) {
            this.currentInfoWindow.close();
            this.currentInfoWindow = null;
        }
    }

    // 지도 마커 업데이트
    updateMapMarkers() {
        if (this.map) {
            this.addMarkers();
        }
        console.log(`🔄 마커 업데이트: 카테고리(${this.currentCategory}), 검색어(${this.searchQuery})`);
    }

    // 매장에 포커스 (매장 상세 정보 표시)
    focusStore(storeId) {
        const store = this.storeManager.getById(storeId);
        if (!store) return;

        const dday = this.storeManager.getDDay(store.deadline);
        
        // 임시로 alert로 표시 (추후 모달로 개선)
        alert(`
🏪 ${store.name}

📍 ${store.address}
📞 ${store.phone}
🕒 ${store.operatingHours}

💰 할인 정보:
   원가: ${store.originalPrice}
   할인가: ${store.discountPrice} (${store.discount} 할인)

⏰ ${dday.text}
📝 ${store.description}

🏷️ 태그: ${store.tags.join(', ')}
        `);

        // 실제 지도에서 해당 매장으로 이동하는 코드 (추후 구현)
        console.log(`🎯 매장 포커스: ${store.name} (${store.lat}, ${store.lng})`);
    }

    // D-Day 업데이트 (1분마다 실행)
    updateDDays() {
        this.renderHotdeals();
    }

    // 애플리케이션 상태 로그
    getStatus() {
        return {
            totalStores: this.storeManager.getAll().length,
            hotdeals: this.storeManager.getHotdeals().length,
            currentCategory: this.currentCategory,
            searchQuery: this.searchQuery,
            hasMap: !!this.map
        };
    }
}

// 애플리케이션 인스턴스 생성
const dealharubangMap = new DealharubangMap();

// 전역으로 접근 가능하도록 설정 (디버깅용)
window.dealharubangMap = dealharubangMap;

// D-Day 업데이트 (1분마다)
setInterval(() => {
    dealharubangMap.updateDDays();
}, 60000);

// 개발자 도구에서 상태 확인 가능
console.log('🔧 개발자 도구에서 dealharubangMap.getStatus() 로 상태 확인 가능합니다.');
