// 딜하르방 광고 시스템
// 로딩 중 광고 표시 및 관리

const AD_SYSTEM = {
    // 광고 데이터
    ads: [
        {
            id: 'hyatt_jeju_2024',
            title: '그랜드 하얏트 제주',
            subtitle: '특급 호텔 특별 할인',
            discount: '최대 40% 할인',
            description: '럭셔리한 제주 여행의 시작',
            image: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80',
            backgroundColor: '#1a365d',
            textColor: '#ffffff',
            accentColor: '#F7C852',
            dday: 'D-2',
            urgent: true,
            cta: '지금 예약하기',
            storeId: 1,
            duration: 3000, // 3초 표시
            priority: 1
        }
        // 추후 다른 광고들 추가 가능
    ],

    // 현재 표시 중인 광고
    currentAd: null,
    adContainer: null,

    // 광고 시스템 초기화
    init() {
        this.createAdContainer();
        console.log('🎯 딜하르방 광고 시스템 초기화 완료');
    },

    // 광고 컨테이너 생성
    createAdContainer() {
        // 기존 광고 컨테이너 제거
        const existing = document.getElementById('dealharubang-ad-container');
        if (existing) {
            existing.remove();
        }

        // 새 광고 컨테이너 생성
        this.adContainer = document.createElement('div');
        this.adContainer.id = 'dealharubang-ad-container';
        this.adContainer.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100vw;
            height: 100vh;
            background: rgba(0, 0, 0, 0.95);
            z-index: 10000;
            display: none;
            justify-content: center;
            align-items: center;
            backdrop-filter: blur(5px);
            font-family: 'Pretendard', sans-serif;
        `;

        document.body.appendChild(this.adContainer);
    },

    // 광고 표시
    showAd(trigger = 'loading') {
        if (!this.ads.length) return;

        // 우선순위가 높은 광고 선택
        const ad = this.ads.sort((a, b) => a.priority - b.priority)[0];
        this.currentAd = ad;

        console.log(`🎯 광고 표시: ${ad.title} (${trigger})`);

        // 광고 HTML 생성
        const adHTML = this.createAdHTML(ad);
        this.adContainer.innerHTML = adHTML;
        this.adContainer.style.display = 'flex';

        // 애니메이션 시작
        setTimeout(() => {
            const adContent = this.adContainer.querySelector('.ad-content');
            if (adContent) {
                adContent.style.opacity = '1';
                adContent.style.transform = 'scale(1)';
            }
        }, 100);

        // 자동 닫기
        setTimeout(() => {
            this.hideAd();
        }, ad.duration);

        // 클릭 이벤트 추가
        this.addAdEvents(ad);
    },

    // 광고 HTML 생성
    createAdHTML(ad) {
        return `
            <div class="ad-content" style="
                background: linear-gradient(135deg, ${ad.backgroundColor} 0%, ${this.darkenColor(ad.backgroundColor, 20)} 100%);
                color: ${ad.textColor};
                border-radius: 24px;
                padding: 40px;
                max-width: 500px;
                width: 90%;
                text-align: center;
                box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
                position: relative;
                overflow: hidden;
                opacity: 0;
                transform: scale(0.9);
                transition: all 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
            ">
                <!-- 배경 패턴 -->
                <div style="
                    position: absolute;
                    top: 0;
                    left: 0;
                    right: 0;
                    bottom: 0;
                    background-image: 
                        radial-gradient(circle at 20% 30%, rgba(255, 255, 255, 0.1) 0%, transparent 50%),
                        radial-gradient(circle at 80% 70%, rgba(247, 200, 82, 0.15) 0%, transparent 40%);
                    pointer-events: none;
                "></div>

                <!-- D-Day 배지 -->
                ${ad.urgent ? `
                    <div style="
                        position: absolute;
                        top: 20px;
                        right: 20px;
                        background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
                        color: white;
                        padding: 8px 16px;
                        border-radius: 20px;
                        font-size: 14px;
                        font-weight: 700;
                        animation: pulse 2s ease-in-out infinite;
                        box-shadow: 0 4px 12px rgba(231, 76, 60, 0.4);
                    ">${ad.dday}</div>
                ` : ''}

                <!-- 메인 콘텐츠 -->
                <div style="position: relative; z-index: 2;">
                    <div style="
                        width: 80px;
                        height: 80px;
                        background: ${ad.accentColor};
                        border-radius: 50%;
                        margin: 0 auto 20px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        font-size: 32px;
                        animation: float 3s ease-in-out infinite;
                        box-shadow: 0 8px 25px rgba(247, 200, 82, 0.4);
                    ">🏨</div>

                    <h2 style="
                        font-size: 28px;
                        font-weight: 900;
                        margin-bottom: 8px;
                        text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
                    ">${ad.title}</h2>

                    <p style="
                        font-size: 16px;
                        opacity: 0.9;
                        margin-bottom: 15px;
                        font-weight: 600;
                    ">${ad.subtitle}</p>

                    <div style="
                        background: ${ad.accentColor};
                        color: #333;
                        padding: 12px 24px;
                        border-radius: 25px;
                        font-size: 20px;
                        font-weight: 900;
                        margin-bottom: 15px;
                        display: inline-block;
                        animation: dealGlow 3s ease-in-out infinite;
                        box-shadow: 0 4px 15px rgba(247, 200, 82, 0.3);
                    ">${ad.discount}</div>

                    <p style="
                        font-size: 14px;
                        opacity: 0.8;
                        margin-bottom: 25px;
                        line-height: 1.4;
                    ">${ad.description}</p>

                    <button onclick="AD_SYSTEM.onAdClick()" style="
                        background: ${ad.accentColor};
                        color: #333;
                        border: none;
                        padding: 15px 30px;
                        border-radius: 25px;
                        font-size: 16px;
                        font-weight: 700;
                        cursor: pointer;
                        transition: all 0.3s ease;
                        box-shadow: 0 6px 20px rgba(247, 200, 82, 0.3);
                        margin-right: 15px;
                    " onmouseover="this.style.transform='scale(1.05)'" onmouseout="this.style.transform='scale(1)'">
                        ${ad.cta}
                    </button>

                    <button onclick="AD_SYSTEM.hideAd()" style="
                        background: transparent;
                        color: ${ad.textColor};
                        border: 2px solid rgba(255, 255, 255, 0.3);
                        padding: 15px 30px;
                        border-radius: 25px;
                        font-size: 16px;
                        font-weight: 600;
                        cursor: pointer;
                        transition: all 0.3s ease;
                        opacity: 0.8;
                    " onmouseover="this.style.opacity='1'" onmouseout="this.style.opacity='0.8'">
                        건너뛰기
                    </button>
                </div>

                <!-- 로딩 바 -->
                <div style="
                    position: absolute;
                    bottom: 0;
                    left: 0;
                    height: 4px;
                    background: ${ad.accentColor};
                    animation: loadingBar ${ad.duration}ms linear;
                    border-radius: 0 0 24px 24px;
                "></div>
            </div>

            <style>
                @keyframes loadingBar {
                    from { width: 100%; }
                    to { width: 0%; }
                }
                
                @keyframes pulse {
                    0%, 100% { transform: scale(1); }
                    50% { transform: scale(1.05); }
                }
                
                @keyframes float {
                    0%, 100% { transform: translateY(0px); }
                    50% { transform: translateY(-10px); }
                }
                
                @keyframes dealGlow {
                    0%, 100% { box-shadow: 0 4px 15px rgba(247, 200, 82, 0.3); }
                    50% { box-shadow: 0 6px 25px rgba(247, 200, 82, 0.6); }
                }
            </style>
        `;
    },

    // 광고 클릭 이벤트
    onAdClick() {
        if (this.currentAd && this.currentAd.storeId) {
            console.log(`🎯 광고 클릭: ${this.currentAd.title}`);
            this.hideAd();
            
            // 해당 매장으로 포커스
            if (window.dealharubangMap) {
                dealharubangMap.focusStore(this.currentAd.storeId);
            }
        }
    },

    // 광고 이벤트 추가
    addAdEvents(ad) {
        // ESC 키로 광고 닫기
        const handleKeyPress = (e) => {
            if (e.key === 'Escape') {
                this.hideAd();
                document.removeEventListener('keydown', handleKeyPress);
            }
        };
        document.addEventListener('keydown', handleKeyPress);

        // 배경 클릭으로 광고 닫기
        this.adContainer.addEventListener('click', (e) => {
            if (e.target === this.adContainer) {
                this.hideAd();
            }
        });
    },

    // 광고 숨기기
    hideAd() {
        if (this.adContainer) {
            const adContent = this.adContainer.querySelector('.ad-content');
            if (adContent) {
                adContent.style.opacity = '0';
                adContent.style.transform = 'scale(0.9)';
            }
            
            setTimeout(() => {
                this.adContainer.style.display = 'none';
                this.currentAd = null;
            }, 300);
        }
        
        console.log('🎯 광고 숨김');
    },

    // 색상 어둡게 만들기 유틸리티
    darkenColor(color, percent) {
        const num = parseInt(color.replace("#", ""), 16);
        const amt = Math.round(2.55 * percent);
        const R = (num >> 16) - amt;
        const G = (num >> 8 & 0x00FF) - amt;
        const B = (num & 0x0000FF) - amt;
        return "#" + (0x1000000 + (R < 255 ? R < 1 ? 0 : R : 255) * 0x10000 +
            (G < 255 ? G < 1 ? 0 : G : 255) * 0x100 +
            (B < 255 ? B < 1 ? 0 : B : 255)).toString(16).slice(1);
    }
};

// 전역으로 사용할 수 있도록 export
window.AD_SYSTEM = AD_SYSTEM;
