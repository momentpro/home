import tkinter as tk
from tkinter import ttk, messagebox, filedialog
import json
import time
import random
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.action_chains import ActionChains
from selenium.webdriver.common.keys import Keys
from selenium.common.exceptions import TimeoutException, NoSuchElementException
from webdriver_manager.chrome import ChromeDriverManager
import threading
import os

class ThreadAutomation:
    def __init__(self):
        self.driver = None
        self.is_running = False
        self.config = self.load_config()
        
    def load_config(self):
        """설정 파일 로드"""
        try:
            with open('config.json', 'r', encoding='utf-8') as f:
                return json.load(f)
        except FileNotFoundError:
            return {
                "delay_range": [2, 5],
                "max_posts_per_keyword": 10,
                "scroll_count": 10,
                "max_follow_count": 200,
                "search_criteria": "인기글",
                "secret_mode": False,
                "auto_like": True,
                "auto_repost": False,
                "auto_follow": False,
                "auto_comment": False,
                "comments": ["좋은 글이네요!", "공감합니다!", "멋진 내용이에요!"]
            }
    
    def save_config(self):
        """설정 파일 저장"""
        with open('config.json', 'w', encoding='utf-8') as f:
            json.dump(self.config, f, ensure_ascii=False, indent=2)
    
    def setup_driver(self):
        """Chrome WebDriver 설정 - Chrome DevTools Protocol 사용"""
        chrome_options = Options()
        
        # ChromeDriver 경로 직접 지정
        driver_path = "/Users/hwangjuyong/.wdm/drivers/chromedriver/mac64/138.0.7204.183/chromedriver-mac-arm64/chromedriver"
        
        # 실행 권한 확인 및 설정
        import os
        if not os.access(driver_path, os.X_OK):
            os.chmod(driver_path, 0o755)
            print(f"ChromeDriver 실행 권한 설정 완료: {driver_path}")
        
        # Chrome DevTools Protocol로 기존 크롬 세션 연결 시도
        try:
            chrome_options.add_experimental_option("debuggerAddress", "127.0.0.1:9222")
            service = Service(driver_path)
            self.driver = webdriver.Chrome(service=service, options=chrome_options)
            print("Chrome DevTools Protocol로 기존 크롬 세션에 연결되었습니다.")
            return self.driver
        except Exception as e:
            print(f"기존 크롬 세션 연결 실패: {e}")
            print("새로운 크롬 창을 열어서 진행합니다...")
            
            # 새로운 크롬 창으로 진행 (DevTools Protocol 사용)
            chrome_options = Options()
            chrome_options.add_argument("--no-sandbox")
            chrome_options.add_argument("--disable-dev-shm-usage")
            chrome_options.add_argument("--disable-blink-features=AutomationControlled")
            chrome_options.add_argument("--disable-web-security")
            chrome_options.add_argument("--allow-running-insecure-content")
            
            if self.config.get('secret_mode', False):
                chrome_options.add_argument("--incognito")
            
            service = Service(driver_path)
            self.driver = webdriver.Chrome(service=service, options=chrome_options)
            
            # 자동화 감지 방지
            self.driver.execute_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")
            self.driver.execute_script("Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]})")
            self.driver.execute_script("Object.defineProperty(navigator, 'languages', {get: () => ['ko-KR', 'ko']})")
            
            return self.driver
    
    def login_check(self):
        """스레드 로그인 상태 확인"""
        try:
            self.driver.get("https://www.threads.net")
            time.sleep(5)
            
            # 로그인 상태 확인 (여러 방법 시도)
            selectors = [
                "[data-testid='profile-button']",
                "[aria-label*='프로필']",
                "[aria-label*='Profile']",
                "a[href*='/profile']",
                "button[aria-label*='프로필']",
                "button[aria-label*='Profile']"
            ]
            
            for selector in selectors:
                try:
                    WebDriverWait(self.driver, 3).until(
                        EC.presence_of_element_located((By.CSS_SELECTOR, selector))
                    )
                    return True
                except TimeoutException:
                    continue
            
            return False
        except Exception as e:
            print(f"로그인 확인 중 오류: {e}")
            return False
    
    def search_keyword(self, keyword):
        """키워드 검색 - 스레드 실제 구조에 맞게 완전 개선"""
        try:
            # 스레드 검색 페이지로 직접 이동
            search_url = f"https://www.threads.com/search?q={keyword}&serp_type=default"
            print(f"검색 URL로 직접 이동: {search_url}")
            self.driver.get(search_url)
            
            # 페이지가 완전히 로드될 때까지 대기
            print("페이지 로딩 대기 중...")
            time.sleep(15)  # 더 긴 초기 로딩 대기
            
            # 스레드가 실제로 사용하는 다른 선택자들 시도
            print("스레드 실제 구조 확인 중...")
            
            # 1. 먼저 일반적인 텍스트 요소들 찾기
            text_elements = self.driver.find_elements(By.CSS_SELECTOR, "div, span, p, article")
            print(f"텍스트 요소들: {len(text_elements)}개 발견")
            
            # 2. 실제 포스트처럼 보이는 요소들 찾기
            post_like_elements = []
            for element in text_elements:
                try:
                    text_content = element.text.strip()
                    # 포스트처럼 보이는 텍스트 필터링
                    if (len(text_content) > 20 and 
                        not text_content.startswith('http') and
                        not text_content.startswith('@') and
                        not text_content.startswith('#') and
                        len(text_content.split()) > 3):  # 최소 3단어 이상
                        
                        post_like_elements.append(element)
                        print(f"포스트 후보 발견: {text_content[:100]}...")
                        
                        if len(post_like_elements) >= 5:  # 최대 5개까지만
                            break
                except:
                    continue
            
            if post_like_elements:
                print(f"포스트 후보 {len(post_like_elements)}개 발견!")
                return True
            else:
                print("포스트 후보를 찾을 수 없습니다.")
                return False
                
        except Exception as e:
            print(f"검색 중 오류: {e}")
            return False
    
    def get_posts(self, max_posts=10):
        """검색 결과에서 포스트 가져오기 - 텍스트 기반 포스트 찾기"""
        posts = []
        try:
            print("=== 텍스트 기반 포스트 찾기 시작 ===")
            
            # 모든 텍스트 요소들 찾기
            all_text_elements = self.driver.find_elements(By.CSS_SELECTOR, "div, span, p, article, section")
            print(f"전체 텍스트 요소들: {len(all_text_elements)}개")
            
            # 포스트처럼 보이는 요소들 필터링
            post_candidates = []
            for element in all_text_elements:
                try:
                    text_content = element.text.strip()
                    
                    # 포스트 조건 확인
                    if (len(text_content) > 30 and  # 최소 30자 이상
                        not text_content.startswith('http') and
                        not text_content.startswith('@') and
                        not text_content.startswith('#') and
                        not text_content.startswith('Search') and
                        not text_content.startswith('검색') and
                        len(text_content.split()) > 5 and  # 최소 5단어 이상
                        not any(keyword in text_content.lower() for keyword in ['cookie', 'privacy', 'terms', 'policy', 'login', 'sign up', 'follow', 'like', 'reply'])):
                        
                        # 중복 제거
                        if text_content not in [candidate['text'] for candidate in post_candidates]:
                            post_candidates.append({
                                'element': element,
                                'text': text_content
                            })
                            print(f"포스트 후보 {len(post_candidates)}: {text_content[:100]}...")
                            
                            if len(post_candidates) >= max_posts:
                                break
                except:
                    continue
            
            print(f"포스트 후보 {len(post_candidates)}개 발견")
            
            # 실제 포스트로 변환
            for i, candidate in enumerate(post_candidates[:max_posts]):
                try:
                    post_data = {
                        'element': candidate['element'],
                        'index': i
                    }
                    posts.append(post_data)
                    print(f"포스트 {i+1} 추가됨")
                except Exception as e:
                    print(f"포스트 {i+1} 처리 중 오류: {e}")
                    continue
            
            print(f"=== 완료: 총 {len(posts)}개의 포스트를 찾았습니다. ===")
            return posts
                    
        except Exception as e:
            print(f"포스트 가져오기 중 오류: {e}")
            return []
    
    def find_interactive_fields(self, post_element):
        """필드 찾기 - DOM 구조 실시간 분석"""
        print("=== 필드 찾기 시작 ===")
        
        fields = {
            'like': None,
            'repost': None,
            'follow': None,
            'comment': None
        }
        
        try:
            # 모든 요소를 스캔하여 필드 찾기
            all_elements = post_element.find_elements(By.CSS_SELECTOR, "*")
            print(f"총 {len(all_elements)}개 요소 분석 중...")
            
            for i, element in enumerate(all_elements):
                try:
                    # 요소 속성 분석
                    tag_name = element.tag_name
                    aria_label = element.get_attribute("aria-label") or ""
                    text_content = element.text.strip()
                    class_attr = element.get_attribute("class") or ""
                    role_attr = element.get_attribute("role") or ""
                    
                    # 클릭 가능한 요소인지 확인
                    is_clickable = (tag_name in ['button', 'div', 'a', 'svg'] and 
                                  element.is_displayed() and element.is_enabled())
                    
                    if is_clickable:
                        # 좋아요 필드 찾기
                        if not fields['like'] and any(keyword in aria_label.lower() or keyword in text_content.lower() 
                                                    for keyword in ['좋아요', 'like', '하트', 'heart']):
                            if "취소" not in aria_label and "unlike" not in aria_label.lower():
                                fields['like'] = element
                                print(f"좋아요 필드 발견: {aria_label or text_content or tag_name}")
                        
                        # 리포스트 필드 찾기
                        if not fields['repost'] and any(keyword in aria_label.lower() or keyword in text_content.lower() 
                                                      for keyword in ['리포스트', 'repost', '재게시', '공유']):
                            fields['repost'] = element
                            print(f"리포스트 필드 발견: {aria_label or text_content or tag_name}")
                        
                        # 팔로우 필드 찾기
                        if not fields['follow'] and any(keyword in aria_label.lower() or keyword in text_content.lower() 
                                                      for keyword in ['팔로우', 'follow', '구독']):
                            if "취소" not in aria_label and "unfollow" not in aria_label.lower():
                                fields['follow'] = element
                                print(f"팔로우 필드 발견: {aria_label or text_content or tag_name}")
                        
                        # 댓글 필드 찾기
                        if not fields['comment'] and any(keyword in aria_label.lower() or keyword in text_content.lower() 
                                                       for keyword in ['댓글', 'reply', '답글', 'comment']):
                            fields['comment'] = element
                            print(f"댓글 필드 발견: {aria_label or text_content or tag_name}")
                    
                    # 모든 필드를 찾았으면 종료
                    if all(fields.values()):
                        break
                        
                except Exception as e:
                    continue
            
            # 찾은 필드 요약
            found_fields = [name for name, field in fields.items() if field is not None]
            print(f"필드 찾기 완료: {found_fields}")
            
            return fields
            
        except Exception as e:
            print(f"필드 찾기 중 오류: {e}")
            return fields

    def safe_click(self, element, action_name):
        """안전한 클릭 방식"""
        try:
            # 방법 1: 일반 클릭
            try:
                element.click()
                print(f"{action_name} 성공! (일반 클릭)")
                return True
            except Exception as e1:
                print(f"일반 클릭 실패: {e1}")
            
            # 방법 2: ActionChains 클릭
            try:
                actions = ActionChains(self.driver)
                actions.move_to_element(element).click().perform()
                print(f"{action_name} 성공! (ActionChains 클릭)")
                return True
            except Exception as e2:
                print(f"ActionChains 클릭 실패: {e2}")
            
            # 방법 3: JavaScript 이벤트 디스패치
            try:
                self.driver.execute_script("""
                    var element = arguments[0];
                    if (element) {
                        var event = new MouseEvent('click', {
                            view: window,
                            bubbles: true,
                            cancelable: true
                        });
                        element.dispatchEvent(event);
                    }
                """, element)
                print(f"{action_name} 성공! (JavaScript 이벤트)")
                return True
            except Exception as e3:
                print(f"JavaScript 이벤트 실패: {e3}")
            
            # 방법 4: 부모 요소 클릭
            try:
                parent = element.find_element(By.XPATH, "./..")
                parent.click()
                print(f"{action_name} 성공! (부모 요소 클릭)")
                return True
            except Exception as e4:
                print(f"부모 요소 클릭 실패: {e4}")
            
            return False
            
        except Exception as e:
            print(f"{action_name} 안전 클릭 중 오류: {e}")
            return False

    def like_post(self, post_element):
        """포스트 좋아요 - 필드 찾기 기반"""
        try:
            print("=== 좋아요 실행 ===")
            
            # 필드 찾기
            fields = self.find_interactive_fields(post_element)
            like_button = fields.get('like')
            
            if like_button:
                # 스크롤해서 버튼이 보이도록 하기
                self.driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", like_button)
                time.sleep(1)
                
                # 안전한 클릭
                result = self.safe_click(like_button, "좋아요")
                time.sleep(random.uniform(1, 2))
                return result
            else:
                print("좋아요 필드를 찾을 수 없습니다.")
                return False
                
        except Exception as e:
            print(f"좋아요 중 오류: {e}")
            return False
    
    def repost_post(self, post_element):
        """포스트 리포스트 - 필드 찾기 기반"""
        try:
            print("=== 리포스트 실행 ===")
            
            # 필드 찾기
            fields = self.find_interactive_fields(post_element)
            repost_button = fields.get('repost')
            
            if repost_button:
                # 스크롤해서 버튼이 보이도록 하기
                self.driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", repost_button)
                time.sleep(1)
                
                # 안전한 클릭
                result = self.safe_click(repost_button, "리포스트")
                time.sleep(random.uniform(1, 2))
                return result
            else:
                print("리포스트 필드를 찾을 수 없습니다.")
                return False
                
        except Exception as e:
            print(f"리포스트 중 오류: {e}")
            return False
    
    def follow_user(self, post_element):
        """사용자 팔로우 - 필드 찾기 기반"""
        try:
            print("=== 팔로우 실행 ===")
            
            # 필드 찾기
            fields = self.find_interactive_fields(post_element)
            follow_button = fields.get('follow')
            
            if follow_button:
                # 스크롤해서 버튼이 보이도록 하기
                self.driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", follow_button)
                time.sleep(1)
                
                # 안전한 클릭
                result = self.safe_click(follow_button, "팔로우")
                time.sleep(random.uniform(1, 2))
                return result
            else:
                print("팔로우 필드를 찾을 수 없습니다.")
                return False
                
        except Exception as e:
            print(f"팔로우 중 오류: {e}")
            return False
    
    def comment_post(self, post_element, comment_text):
        """포스트 댓글 작성 - 기존 프로그램과 동일한 순서"""
        try:
            print("=== 댓글 실행 ===")
            
            # 필드 찾기
            fields = self.find_interactive_fields(post_element)
            comment_button = fields.get('comment')
            
            if comment_button:
                # 댓글 버튼 클릭
                print("댓글 버튼 찾기 시도...")
                self.driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", comment_button)
                time.sleep(1)
                
                # 안전한 클릭
                if self.safe_click(comment_button, "댓글 버튼"):
                    print("댓글 버튼 성공")
                    time.sleep(3)
                    
                    # 댓글 입력 필드 찾기
                    print("댓글 입력 필드 찾기 시도...")
                    input_selectors = [
                        "textarea[placeholder*='댓글']",
                        "textarea[placeholder*='Reply']", 
                        "textarea[placeholder*='답글']",
                        "div[contenteditable='true']",
                        "input[placeholder*='댓글']",
                        "input[placeholder*='Reply']",
                        "input[placeholder*='답글']",
                        "textarea",
                        "input[type='text']"
                    ]
                    
                    comment_input = None
                    for selector in input_selectors:
                        try:
                            comment_input = WebDriverWait(self.driver, 5).until(
                                EC.presence_of_element_located((By.CSS_SELECTOR, selector))
                            )
                            print(f"댓글 입력 필드 찾기 성공: {selector}")
                            break
                        except TimeoutException:
                            continue
                    
                    if comment_input:
                        # 댓글 선택 (입력)
                        print("댓글 선택")
                        comment_input.clear()
                        comment_input.send_keys(comment_text)
                        time.sleep(1)
                        
                        # 게시 버튼 찾기
                        print("게시 버튼 찾기")
                        
                        # 먼저 Enter 키로 시도
                        try:
                            comment_input.send_keys(Keys.RETURN)
                            print("게시 완료 (Enter 키)")
                            time.sleep(2)
                            return True
                        except:
                            # Enter가 안되면 게시 버튼 찾기
                            post_button_selectors = [
                                "button[type='submit']",
                                "button:contains('게시')",
                                "button:contains('Post')",
                                "button:contains('Reply')",
                                "div[role='button']:contains('게시')",
                                "div[role='button']:contains('Post')"
                            ]
                            
                            for selector in post_button_selectors:
                                try:
                                    post_button = self.driver.find_element(By.CSS_SELECTOR, selector)
                                    if post_button.is_displayed() and post_button.is_enabled():
                                        self.safe_click(post_button, "게시 버튼")
                                        print("게시 완료 (게시 버튼)")
                                        time.sleep(2)
                                        return True
                                except:
                                    continue
                            
                            print("게시 버튼을 찾을 수 없습니다.")
                            return False
                    else:
                        print("댓글 입력 필드를 찾을 수 없습니다.")
                        return False
                else:
                    print("댓글 버튼 클릭 실패")
                    return False
            else:
                print("댓글 필드를 찾을 수 없습니다.")
                return False
                
        except Exception as e:
            print(f"댓글 작성 중 오류: {e}")
            return False
    
    def find_account_elements(self):
        """메인 페이지에서 계정 요소들 찾기"""
        try:
            print("=== 계정 요소 찾기 시작 ===")
            
            # 실제 스레드 페이지 구조 분석
            print("페이지 구조 분석 중...")
            
            account_elements = []
            
            # 방법 1: 모든 링크 요소에서 @ 포함된 것 찾기
            try:
                all_links = self.driver.find_elements(By.TAG_NAME, "a")
                print(f"전체 링크 수: {len(all_links)}")
                
                for link in all_links:
                    try:
                        href = link.get_attribute("href") or ""
                        text = link.text.strip()
                        
                        if (("@" in href or "@" in text) and 
                            link.is_displayed() and 
                            len(text) > 0):
                            account_elements.append(link)
                            print(f"계정 링크 발견: {text[:20]}... (href: {href[:50]}...)")
                            
                            if len(account_elements) >= 10:
                                break
                    except:
                        continue
            except Exception as e:
                print(f"링크 방식 실패: {e}")
            
            # 방법 2: 사용자명 패턴 찾기 (hankki09252 같은)
            if len(account_elements) < 5:
                try:
                    all_elements = self.driver.find_elements(By.CSS_SELECTOR, "*")
                    print(f"전체 요소 수: {len(all_elements)}")
                    
                    for element in all_elements:
                        try:
                            text = element.text.strip()
                            # 사용자명 패턴: 영문+숫자 조합, 3-20자
                            if (len(text) >= 3 and len(text) <= 20 and 
                                text.replace('_', '').replace('.', '').isalnum() and
                                not text.isdigit() and  # 순수 숫자는 제외
                                element.is_displayed() and
                                element.tag_name in ['span', 'div', 'a']):
                                
                                # 클릭 가능한지 확인
                                if (element.tag_name == 'a' or 
                                    element.get_attribute('onclick') or
                                    element.get_attribute('role') == 'button'):
                                    
                                    account_elements.append(element)
                                    print(f"사용자명 패턴 발견: {text}")
                                    
                                    if len(account_elements) >= 10:
                                        break
                        except:
                            continue
                except Exception as e:
                    print(f"패턴 방식 실패: {e}")
            
            # 방법 3: 프로필 관련 요소 찾기
            if len(account_elements) < 5:
                try:
                    profile_selectors = [
                        "[data-testid*='profile']",
                        "[data-testid*='user']", 
                        "[aria-label*='프로필']",
                        "[aria-label*='Profile']",
                        "div:contains('프로필')",
                        "span:contains('시간')",  # "14시간" 같은 시간 표시 근처
                    ]
                    
                    for selector in profile_selectors:
                        try:
                            elements = self.driver.find_elements(By.CSS_SELECTOR, selector)
                            for element in elements:
                                if element.is_displayed():
                                    # 부모나 형제 요소에서 클릭 가능한 것 찾기
                                    parent = element.find_element(By.XPATH, "./..")
                                    if parent and parent.is_displayed():
                                        account_elements.append(parent)
                                        print(f"프로필 관련 요소 발견: {element.text[:20]}...")
                                        
                                        if len(account_elements) >= 10:
                                            break
                        except:
                            continue
                            
                        if len(account_elements) >= 10:
                            break
                except Exception as e:
                    print(f"프로필 방식 실패: {e}")
            
            # 중복 제거
            unique_elements = []
            for element in account_elements:
                if element not in unique_elements:
                    unique_elements.append(element)
            
            print(f"발견된 계정 요소 수: {len(unique_elements)}")
            
            # 발견된 요소들의 정보 출력
            for i, element in enumerate(unique_elements[:5]):
                try:
                    text = element.text.strip()[:30]
                    tag = element.tag_name
                    print(f"계정 {i+1}: {tag} - '{text}...'")
                except:
                    print(f"계정 {i+1}: 정보 가져오기 실패")
            
            return unique_elements
            
        except Exception as e:
            print(f"계정 요소 찾기 중 오류: {e}")
            return []

    def click_account_for_popup(self, account_element):
        """계정을 클릭해서 팝업 열기"""
        try:
            print("계정 클릭으로 팝업 열기 시도...")
            
            # 현재 윈도우 핸들들 저장
            original_windows = set(self.driver.window_handles)
            
            # 계정 요소 클릭
            self.driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", account_element)
            time.sleep(1)
            
            if self.safe_click(account_element, "계정"):
                time.sleep(3)
                
                # 새 윈도우가 열렸는지 확인
                current_windows = set(self.driver.window_handles)
                new_windows = current_windows - original_windows
                
                if new_windows:
                    # 새 윈도우로 전환
                    new_window = list(new_windows)[0]
                    self.driver.switch_to.window(new_window)
                    print("계정 팝업 열기 성공 (새 윈도우)")
                    return True
                else:
                    print("계정 페이지 열기 성공 (같은 윈도우)")
                    return True
            
            return False
            
        except Exception as e:
            print(f"계정 팝업 열기 실패: {e}")
            return False

    def follow_in_popup(self):
        """팔로우 버튼 찾기 - 스크린샷 기반 정확한 탐지"""
        try:
            print("팔로우 버튼 정확한 탐지 시작...")
            
            # 스크린샷에서 확인된 팔로우 버튼 특징:
            # - 흰색 배경의 버튼
            # - "팔로우" 텍스트
            # - 프로필 영역 내 위치
            
            follow_selectors = [
                # 팔로우 관련 aria-label
                "button[aria-label*='팔로우']",
                "button[aria-label*='Follow']",
                # 일반적인 버튼에서 텍스트 확인
                "button",
                "div[role='button']"
            ]
            
            follow_found = False
            
            for selector in follow_selectors:
                try:
                    buttons = self.driver.find_elements(By.CSS_SELECTOR, selector)
                    print(f"선택자 '{selector}'로 {len(buttons)}개 버튼 발견")
                    
                    for i, button in enumerate(buttons):
                        try:
                            if not (button.is_displayed() and button.is_enabled()):
                                continue
                                
                            button_text = button.text.strip()
                            aria_label = button.get_attribute("aria-label") or ""
                            
                            print(f"버튼 {i+1}: text='{button_text}', aria-label='{aria_label}'")
                            
                            # 정확히 "팔로우"인 버튼 찾기
                            if (button_text == "팔로우" or 
                                "팔로우" in aria_label or 
                                button_text == "Follow" or
                                "Follow" in aria_label):
                                
                                # 이미 팔로우 중인지 확인
                                if ("팔로잉" not in button_text and 
                                    "Following" not in button_text and
                                    "언팔로우" not in aria_label and
                                    "Unfollow" not in aria_label):
                                    
                                    print(f"팔로우 버튼 발견! 클릭 시도: '{button_text}'")
                                    
                                    if self.safe_click(button, "팔로우"):
                                        print("팔로우 성공!")
                                        time.sleep(2)
                                        return True
                                else:
                                    print("이미 팔로우 중입니다.")
                                    return False
                        except Exception as e:
                            print(f"버튼 {i+1} 처리 중 오류: {e}")
                            continue
                            
                except Exception as e:
                    print(f"선택자 '{selector}' 처리 중 오류: {e}")
                    continue
            
            print("팔로우 버튼을 찾을 수 없습니다.")
            return False
            
        except Exception as e:
            print(f"팔로우 버튼 찾기 중 오류: {e}")
            return False

    def find_and_click_post_in_popup(self):
        """팝업에서 게시물 찾아서 클릭"""
        try:
            print("팝업에서 게시물 찾기...")
            
            # 현재 윈도우 핸들들 저장
            original_windows = set(self.driver.window_handles)
            
            # 게시물을 찾는 다양한 방법들
            post_selectors = [
                "article",
                "div[role='article']", 
                "a[href*='/post/']",
                "div[data-testid*='post']"
            ]
            
            for selector in post_selectors:
                try:
                    posts = self.driver.find_elements(By.CSS_SELECTOR, selector)
                    for post in posts[:3]:  # 첫 3개만 시도
                        try:
                            if post.is_displayed():
                                print("게시물 클릭 시도...")
                                
                                if self.safe_click(post, "게시물"):
                                    time.sleep(3)
                                    
                                    # 새 윈도우가 열렸는지 확인
                                    current_windows = set(self.driver.window_handles)
                                    new_windows = current_windows - original_windows
                                    
                                    if new_windows:
                                        # 새 윈도우로 전환
                                        new_window = list(new_windows)[0]
                                        self.driver.switch_to.window(new_window)
                                        print("게시물 팝업 열기 성공")
                                        return True
                                    else:
                                        print("게시물 페이지 열기 성공")
                                        return True
                        except:
                            continue
                except:
                    continue
            
            print("클릭 가능한 게시물을 찾을 수 없습니다.")
            return False
            
        except Exception as e:
            print(f"게시물 팝업 열기 실패: {e}")
            return False

    def interact_in_post_popup(self, comment_text=None):
        """게시물 팝업에서 좋아요, 댓글 처리"""
        try:
            print("게시물 팝업에서 상호작용 시작...")
            
            success = False
            
            # 좋아요 처리 - 스크린샷 기반 정확한 탐지
            if self.config.get('auto_like', True):
                print("좋아요 버튼 정확한 탐지 시작...")
                
                # 스크린샷에서 확인된 좋아요 버튼 특징:
                # - 하트 모양 아이콘 (♥)
                # - 숫자와 함께 표시 (25, 114, 98, 80, 45)
                # - 게시물 하단 좌측에 위치
                
                # 좋아요 버튼을 찾기 위한 다양한 방법
                like_selectors = [
                    # SVG 하트 아이콘
                    "svg[aria-label*='좋아요']",
                    "svg[aria-label*='Like']",
                    # 버튼 내 하트 아이콘
                    "button[aria-label*='좋아요']",
                    "button[aria-label*='Like']",
                    # div 역할 버튼
                    "div[role='button'][aria-label*='좋아요']",
                    "div[role='button'][aria-label*='Like']",
                    # 일반적인 좋아요 패턴
                    "button",
                    "div[role='button']"
                ]
                
                like_found = False
                
                for selector in like_selectors:
                    try:
                        elements = self.driver.find_elements(By.CSS_SELECTOR, selector)
                        print(f"선택자 '{selector}'로 {len(elements)}개 요소 발견")
                        
                        for i, element in enumerate(elements):
                            try:
                                if not (element.is_displayed() and element.is_enabled()):
                                    continue
                                
                                text = element.text.strip()
                                aria_label = element.get_attribute("aria-label") or ""
                                tag_name = element.tag_name
                                
                                # 좋아요 관련 키워드 확인
                                is_like_button = (
                                    "좋아요" in aria_label or
                                    "Like" in aria_label or
                                    "♥" in text or
                                    "❤" in text or
                                    ("좋아요" in text and text.isdigit() == False)  # 숫자만 있는 건 제외
                                )
                                
                                # 이미 좋아요를 눌렀는지 확인
                                is_already_liked = (
                                    "좋아요 취소" in aria_label or
                                    "Unlike" in aria_label or
                                    "좋아요됨" in aria_label
                                )
                                
                                if is_like_button and not is_already_liked:
                                    print(f"좋아요 버튼 발견! {tag_name} - text: '{text}', aria-label: '{aria_label}'")
                                    
                                    if self.safe_click(element, "좋아요"):
                                        print("좋아요 성공!")
                                        time.sleep(2)
                                        success = True
                                        like_found = True
                                        break
                                elif is_already_liked:
                                    print("이미 좋아요를 눌렀습니다.")
                                    like_found = True
                                    break
                                    
                            except Exception as e:
                                print(f"요소 {i+1} 처리 중 오류: {e}")
                                continue
                        
                        if like_found:
                            break
                            
                    except Exception as e:
                        print(f"선택자 '{selector}' 처리 중 오류: {e}")
                        continue
                
                if not like_found:
                    print("좋아요 버튼을 찾을 수 없습니다.")
            
            # 리포스트 처리 - 스크린샷 기반 정확한 탐지
            if self.config.get('auto_repost', True):
                print("리포스트 버튼 정확한 탐지 시작...")
                
                # 스크린샷에서 확인된 리포스트 버튼 특징:
                # - 화살표 순환 모양 아이콘
                # - 숫자와 함께 표시 (3, 80, 51, 16, 15)
                # - 게시물 하단 좋아요 옆에 위치
                
                repost_selectors = [
                    # SVG 리포스트 아이콘
                    "svg[aria-label*='리포스트']",
                    "svg[aria-label*='Repost']",
                    "svg[aria-label*='공유']",
                    "svg[aria-label*='Share']",
                    # 버튼 내 리포스트 아이콘
                    "button[aria-label*='리포스트']",
                    "button[aria-label*='Repost']",
                    "button[aria-label*='공유']",
                    "button[aria-label*='Share']",
                    # div 역할 버튼
                    "div[role='button'][aria-label*='리포스트']",
                    "div[role='button'][aria-label*='Repost']",
                    # 일반적인 리포스트 패턴
                    "button",
                    "div[role='button']"
                ]
                
                repost_found = False
                
                for selector in repost_selectors:
                    try:
                        elements = self.driver.find_elements(By.CSS_SELECTOR, selector)
                        print(f"리포스트 선택자 '{selector}'로 {len(elements)}개 요소 발견")
                        
                        for i, element in enumerate(elements):
                            try:
                                if not (element.is_displayed() and element.is_enabled()):
                                    continue
                                
                                text = element.text.strip()
                                aria_label = element.get_attribute("aria-label") or ""
                                tag_name = element.tag_name
                                
                                # 리포스트 관련 키워드 확인
                                is_repost_button = (
                                    "리포스트" in aria_label or
                                    "Repost" in aria_label or
                                    "공유" in aria_label or
                                    "Share" in aria_label or
                                    ("리포스트" in text and text.isdigit() == False)  # 숫자만 있는 건 제외
                                )
                                
                                # 이미 리포스트했는지 확인
                                is_already_reposted = (
                                    "리포스트 취소" in aria_label or
                                    "Unrepost" in aria_label or
                                    "리포스트됨" in aria_label
                                )
                                
                                if is_repost_button and not is_already_reposted:
                                    print(f"리포스트 버튼 발견! {tag_name} - text: '{text}', aria-label: '{aria_label}'")
                                    
                                    if self.safe_click(element, "리포스트"):
                                        print("리포스트 성공!")
                                        time.sleep(2)
                                        success = True
                                        repost_found = True
                                        break
                                elif is_already_reposted:
                                    print("이미 리포스트했습니다.")
                                    repost_found = True
                                    break
                                    
                            except Exception as e:
                                print(f"리포스트 요소 {i+1} 처리 중 오류: {e}")
                                continue
                        
                        if repost_found:
                            break
                            
                    except Exception as e:
                        print(f"리포스트 선택자 '{selector}' 처리 중 오류: {e}")
                        continue
                
                if not repost_found:
                    print("리포스트 버튼을 찾을 수 없습니다.")
            
            # 댓글 처리 - 스크린샷 기반 정확한 탐지
            if self.config.get('auto_comment', False) and comment_text:
                print("댓글 입력 시스템 시작...")
                
                # 스크린샷에서 확인된 댓글 시스템 특징:
                # - "답글" 헤더가 있는 팝업 형태
                # - "취소" 버튼과 함께 상단에 위치
                # - 텍스트 입력 가능한 영역
                # - 기존 댓글들이 위에 표시됨
                
                # 1단계: 페이지 최하단의 "답글 남기기" 버튼 찾기
                print("페이지 최하단 답글 남기기 버튼 찾기...")
                
                # 페이지 끝까지 스크롤
                self.driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
                time.sleep(2)
                
                comment_button_selectors = [
                    # 일반적인 버튼에서 텍스트 확인
                    "button",
                    "div[role='button']"
                ]
                
                comment_button_clicked = False
                
                for selector in comment_button_selectors:
                    try:
                        buttons = self.driver.find_elements(By.CSS_SELECTOR, selector)
                        print(f"답글 버튼 선택자 '{selector}'로 {len(buttons)}개 발견")
                        
                        for i, button in enumerate(buttons):
                            try:
                                if not (button.is_displayed() and button.is_enabled()):
                                    continue
                                    
                                button_text = button.text.strip()
                                aria_label = button.get_attribute("aria-label") or ""
                                
                                print(f"답글 버튼 후보 {i+1}: text='{button_text}', aria-label='{aria_label}'")
                                
                                # "답글 남기기" 또는 "000님에게 답글" 패턴 찾기
                                is_reply_button = (
                                    "답글 남기기" in button_text or
                                    "답글" in button_text or
                                    ("님에게" in button_text and "답글" in button_text) or
                                    "Reply" in button_text or
                                    "답글" in aria_label
                                )
                                
                                if is_reply_button:
                                    print(f"답글 남기기 버튼 발견! 클릭 시도: '{button_text}'")
                                    
                                    if self.safe_click(button, "답글 남기기"):
                                        print("답글 남기기 버튼 클릭 성공!")
                                        time.sleep(3)  # 팝업 로딩 대기
                                        comment_button_clicked = True
                                        break
                                        
                            except Exception as e:
                                print(f"답글 버튼 {i+1} 클릭 실패: {e}")
                                continue
                        
                        if comment_button_clicked:
                            break
                            
                    except Exception as e:
                        print(f"답글 버튼 선택자 '{selector}' 오류: {e}")
                        continue
                
                if not comment_button_clicked:
                    print("댓글 버튼을 찾을 수 없습니다.")
                else:
                    # 2단계: 댓글 입력창 찾기 (답글 팝업이 열린 후)
                    print("답글 팝업에서 입력창 찾기...")
                    
                    # 답글 팝업 내에서 입력창 찾기
                    input_selectors = [
                        # 답글 관련 placeholder가 있는 입력창
                        "textarea[placeholder*='답글']",
                        "textarea[placeholder*='댓글']", 
                        "textarea[placeholder*='Reply']",
                        "input[placeholder*='답글']",
                        "input[placeholder*='댓글']",
                        "input[placeholder*='Reply']",
                        # contenteditable div
                        "div[contenteditable='true']",
                        # 팝업 내 모든 입력 가능한 요소
                        "textarea",
                        "input[type='text']"
                    ]
                    
                    input_found = False
                    
                    for selector in input_selectors:
                        try:
                            inputs = self.driver.find_elements(By.CSS_SELECTOR, selector)
                            print(f"입력창 선택자 '{selector}'로 {len(inputs)}개 발견")
                            
                            for i, input_elem in enumerate(inputs):
                                try:
                                    if not (input_elem.is_displayed() and input_elem.is_enabled()):
                                        continue
                                        
                                    placeholder = input_elem.get_attribute("placeholder") or ""
                                    tag_name = input_elem.tag_name
                                    value = input_elem.get_attribute("value") or ""
                                    
                                    print(f"입력창 후보 {i+1}: {tag_name} - placeholder: '{placeholder}', value: '{value}'")
                                    
                                    # 입력창이 비어있고 사용 가능한지 확인
                                    if len(value.strip()) == 0:  # 빈 입력창만 사용
                                        print(f"댓글 입력 시도: '{comment_text}'")
                                        
                                        # 입력창 클릭해서 포커스
                                        input_elem.click()
                                        time.sleep(0.5)
                                        
                                        # 기존 내용 지우고 새 댓글 입력
                                        input_elem.clear()
                                        input_elem.send_keys(comment_text)
                                        time.sleep(1)
                                        
                                        # 게시 버튼 찾아서 클릭
                                        print("댓글 게시 버튼 찾기...")
                                        post_button_found = False
                                        
                                        # 게시 버튼은 텍스트로 직접 찾기
                                        
                                        # 모든 버튼에서 게시 버튼 찾기
                                        all_buttons = self.driver.find_elements(By.CSS_SELECTOR, "button, div[role='button']")
                                        
                                        for btn in all_buttons:
                                            try:
                                                if btn.is_displayed() and btn.is_enabled():
                                                    btn_text = btn.text.strip()
                                                    aria_label = btn.get_attribute("aria-label") or ""
                                                    
                                                    if ("게시" in btn_text or "Post" in btn_text or 
                                                        "게시" in aria_label or "Post" in aria_label):
                                                        print(f"게시 버튼 발견: '{btn_text}' / aria-label: '{aria_label}'")
                                                        
                                                        if self.safe_click(btn, "게시 버튼"):
                                                            print("댓글 게시 성공!")
                                                            time.sleep(3)
                                                            post_button_found = True
                                                            break
                                            except:
                                                continue
                                        
                                        if not post_button_found:
                                            print("게시 버튼을 찾을 수 없어서 Enter로 시도...")
                                            input_elem.send_keys(Keys.RETURN)
                                            time.sleep(2)
                                        
                                        print("댓글 작성 완료!")
                                        success = True
                                        input_found = True
                                        break
                                        
                                except Exception as e:
                                    print(f"입력창 {i+1} 사용 실패: {e}")
                                    continue
                            
                            if input_found:
                                break
                                
                        except Exception as e:
                            print(f"입력창 선택자 '{selector}' 오류: {e}")
                            continue
                    
                    if not input_found:
                        print("댓글 입력창을 찾을 수 없습니다.")
                        
                    # 댓글 팝업 닫기 (ESC 키 또는 취소 버튼)
                    try:
                        print("댓글 팝업 닫기...")
                        self.driver.find_element(By.TAG_NAME, "body").send_keys(Keys.ESCAPE)
                        time.sleep(1)
                    except:
                        pass
            
            return success
            
        except Exception as e:
            print(f"게시물 팝업 상호작용 중 오류: {e}")
            return False

    def close_popups_and_return_to_main(self):
        """팝업들을 닫고 메인 윈도우로 돌아가기"""
        try:
            print("팝업 닫고 메인으로 돌아가기...")
            
            all_windows = self.driver.window_handles
            if len(all_windows) <= 1:
                print("닫을 팝업이 없습니다.")
                return True
                
            main_window = all_windows[0]  # 첫 번째가 메인 윈도우
            
            # 모든 추가 윈도우 닫기
            for window in all_windows[1:]:
                try:
                    self.driver.switch_to.window(window)
                    self.driver.close()
                    time.sleep(0.5)
                except:
                    continue
            
            # 메인 윈도우로 돌아가기
            try:
                self.driver.switch_to.window(main_window)
                print("메인 윈도우로 복귀 완료")
                return True
            except:
                # 메인 윈도우가 닫혔다면 남은 윈도우 중 하나로 전환
                remaining_windows = self.driver.window_handles
                if remaining_windows:
                    self.driver.switch_to.window(remaining_windows[0])
                    print("남은 윈도우로 전환 완료")
                    return True
                return False
            
        except Exception as e:
            print(f"팝업 닫기 실패: {e}")
            return False

    def process_posts_popup_style(self, keyword, max_posts=10):
        """팝업 기반 포스트 처리 - 다양한 계정들 처리"""
        if not self.search_keyword(keyword):
            return False
        
        self.current_keyword = keyword  # 현재 키워드 저장
        
        processed_count = 0
        follow_count = 0
        max_follow = self.config.get('max_follow_count', 200)
        processed_accounts = set()  # 이미 처리한 계정들 추적
        
        # 스크롤하면서 다양한 계정들 찾기
        scroll_count = self.config.get('scroll_count', 10)
        
        for scroll_i in range(scroll_count):
            if not self.is_running or processed_count >= max_posts:
                break
                
            print(f"\n=== 스크롤 {scroll_i+1}/{scroll_count} ===")
            
            # 현재 화면에서 계정들 찾기
            current_accounts = self.find_diverse_accounts()
            
            # 새로운 계정들만 처리
            new_accounts = []
            for account_info in current_accounts:
                account_id = account_info.get('id', '')
                if account_id and account_id not in processed_accounts:
                    new_accounts.append(account_info)
                    processed_accounts.add(account_id)
                    
                    if len(new_accounts) >= 3:  # 스크롤당 최대 3개 계정
                        break
            
            print(f"새로운 계정 {len(new_accounts)}개 발견")
            
            # 새로운 계정들 처리
            for account_info in new_accounts:
                if not self.is_running or processed_count >= max_posts:
                    break
                    
                try:
                    print(f"\n=== 계정 '{account_info['id']}' 처리 ===")
                    print("링크 처리 시작")
                    
                    # 현재 URL 출력
                    current_url = self.driver.current_url
                    print(f"현재 URL: {current_url}")
                    
                    # 계정 처리
                    if self.process_single_account(account_info, follow_count, max_follow):
                        processed_count += 1
                        if account_info.get('followed'):
                            follow_count += 1
                    
                    # 딜레이
                    delay = random.uniform(2, 4)
                    time.sleep(delay)
                    
                except Exception as e:
                    print(f"계정 '{account_info['id']}' 처리 중 오류: {e}")
                    continue
            
            # 다음 스크롤
            if scroll_i < scroll_count - 1:
                self.driver.execute_script("window.scrollBy(0, 800);")
                time.sleep(random.uniform(2, 3))
        
        print(f"\n총 {processed_count}개의 계정을 처리했습니다.")
        return processed_count > 0

    def find_diverse_accounts(self):
        """현재 화면에서 다양한 계정들 찾기"""
        try:
            print("현재 화면에서 계정들 찾는 중...")
            
            accounts = []
            
            # 방법 1: 포스트 컨테이너에서 계정 정보 추출
            post_containers = self.driver.find_elements(By.CSS_SELECTOR, "article, div[role='article'], div[data-testid*='post']")
            
            for container in post_containers:
                try:
                    # 사용자명 찾기
                    username_elements = container.find_elements(By.CSS_SELECTOR, "a[href*='/@'], span, div")
                    
                    for elem in username_elements:
                        text = elem.text.strip()
                        href = elem.get_attribute("href") or ""
                        
                        # 사용자명 패턴 확인
                        if (len(text) >= 3 and len(text) <= 30 and 
                            (text.replace('_', '').replace('.', '').isalnum() or '@' in href) and
                            not any(word in text.lower() for word in ['시간', '분', '일', '좋아요', '댓글', '리포스트', '팔로우'])):
                            
                            account_id = text.replace('@', '')
                            
                            accounts.append({
                                'id': account_id,
                                'element': container,  # 포스트 컨테이너 전체
                                'username_element': elem,
                                'href': href
                            })
                            break  # 컨테이너당 하나의 계정만
                            
                except Exception as e:
                    continue
            
            # 방법 2: 직접 링크에서 계정 찾기
            if len(accounts) < 3:
                links = self.driver.find_elements(By.CSS_SELECTOR, "a[href*='/@']")
                
                for link in links:
                    try:
                        href = link.get_attribute("href") or ""
                        if "/@" in href and "/post/" not in href:  # 계정 링크만, 포스트 링크 제외
                            username = href.split("/@")[-1].split("/")[0]
                            
                            if len(username) >= 3 and len(username) <= 30:
                                accounts.append({
                                    'id': username,
                                    'element': link,
                                    'username_element': link,
                                    'href': href
                                })
                    except:
                        continue
            
            # 중복 제거
            unique_accounts = []
            seen_ids = set()
            
            for account in accounts:
                if account['id'] not in seen_ids:
                    unique_accounts.append(account)
                    seen_ids.add(account['id'])
                    
                    if len(unique_accounts) >= 5:  # 최대 5개
                        break
            
            print(f"발견된 고유 계정 수: {len(unique_accounts)}")
            for i, account in enumerate(unique_accounts):
                print(f"계정 {i+1}: {account['id']}")
            
            return unique_accounts
            
        except Exception as e:
            print(f"계정 찾기 중 오류: {e}")
            return []

    def process_single_account(self, account_info, current_follow_count, max_follow):
        """단일 계정 처리"""
        try:
            account_id = account_info['id']
            
            # 1. 계정 프로필로 이동 (URL 직접 이동 우선)
            profile_url = None
            
            if account_info.get('href') and "/@" in account_info['href']:
                # 기존 href에서 프로필 URL 추출
                profile_url = account_info['href'].split("/post/")[0]  # 포스트 부분 제거
            else:
                # account_id로 직접 URL 생성
                profile_url = f"https://www.threads.com/@{account_id}"
            
            print(f"계정 프로필로 이동: {profile_url}")
            self.driver.get(profile_url)
            time.sleep(3)
            
            # 페이지 로딩 확인
            if "threads.com" not in self.driver.current_url:
                print(f"계정 {account_id} 페이지 로딩 실패")
                return False
            
            success = False
            followed = False
            
            # 2. 팔로우 처리
            if self.config.get('auto_follow', False) and current_follow_count < max_follow:
                if self.follow_in_popup():
                    followed = True
                    print(f"팔로우 완료: {account_id}")
            
            # 3. 게시물 찾아서 상호작용
            if self.find_and_click_post_in_popup():
                comment = None
                if self.config.get('auto_comment', False):
                    comment = random.choice(self.config.get('comments', ['좋은 글이네요!']))
                
                if self.interact_in_post_popup(comment):
                    success = True
                    print(f"게시물 상호작용 완료: {account_id}")
            
            # 4. 검색 페이지로 돌아가기
            search_url = f"https://www.threads.com/search?q={self.current_keyword}&serp_type=default"
            print(f"검색 페이지로 돌아가기: {search_url}")
            self.driver.get(search_url)
            time.sleep(2)
            
            account_info['followed'] = followed
            return success
            
        except Exception as e:
            print(f"계정 {account_info['id']} 처리 중 오류: {e}")
            # 오류 시 검색 페이지로 돌아가기
            try:
                search_url = f"https://www.threads.com/search?q={self.current_keyword}&serp_type=default"
                self.driver.get(search_url)
                time.sleep(2)
            except:
                pass
            return False

    def process_posts(self, keyword, max_posts=10):
        """포스트 처리 메인 로직 - 팝업 방식 사용"""
        # 팝업 기반 처리 방식 사용
        return self.process_posts_popup_style(keyword, max_posts)
    
    def start_automation(self, keywords):
        """자동화 시작"""
        try:
            self.driver = self.setup_driver()
            
            if not self.login_check():
                messagebox.showerror("오류", "스레드에 로그인되어 있지 않습니다. 먼저 로그인해주세요.")
                return
            
            total_processed = 0
            for keyword in keywords:
                if not self.is_running:
                    break
                    
                print(f"키워드 '{keyword}' 처리 중...")
                processed = self.process_posts(keyword, self.config.get('max_posts_per_keyword', 10))
                total_processed += processed
                
                # 키워드 간 딜레이
                time.sleep(random.uniform(5, 10))
            
            print(f"총 {total_processed}개의 포스트를 처리했습니다.")
            
        except Exception as e:
            print(f"자동화 중 오류: {e}")
        finally:
            if self.driver:
                self.driver.quit()
    
    def stop_automation(self):
        """자동화 중지"""
        self.is_running = False
        if self.driver:
            self.driver.quit()

class ThreadAutomationGUI:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("스레드 자동화 도구")
        self.root.geometry("600x500")
        
        self.automation = ThreadAutomation()
        self.setup_gui()
    
    def setup_gui(self):
        """GUI 설정"""
        # 메인 프레임
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # Chrome 설정 프레임
        chrome_frame = ttk.LabelFrame(main_frame, text="Chrome 설정", padding="5")
        chrome_frame.grid(row=0, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))
        
        self.secret_mode_var = tk.BooleanVar(value=False)
        ttk.Checkbutton(chrome_frame, text="시크릿 크롬 모드", variable=self.secret_mode_var).grid(row=0, column=0, sticky=tk.W)
        
        # 크롬드라이버 설정 (실제 프로그램 방식)
        ttk.Label(chrome_frame, text="크롬드라이버:").grid(row=1, column=0, sticky=tk.W, pady=(5, 0))
        driver_frame = ttk.Frame(chrome_frame)
        driver_frame.grid(row=1, column=1, sticky=tk.W, padx=(10, 0), pady=(5, 0))
        
        self.chrome_driver_var = tk.StringVar(value="자동 설정")
        ttk.Entry(driver_frame, textvariable=self.chrome_driver_var, width=40).grid(row=0, column=0)
        ttk.Button(driver_frame, text="찾아보기", command=self.browse_chrome_driver).grid(row=0, column=1, padx=(5, 0))
        
        # 사용 순서 안내
        ttk.Label(chrome_frame, text="사용 순서:").grid(row=2, column=0, columnspan=2, sticky=tk.W, pady=(5, 0))
        ttk.Label(chrome_frame, text="1. '크롬 연결' 버튼 클릭").grid(row=3, column=0, columnspan=2, sticky=tk.W)
        ttk.Label(chrome_frame, text="2. 열린 크롬에서 스레드에 로그인").grid(row=4, column=0, columnspan=2, sticky=tk.W)
        ttk.Label(chrome_frame, text="3. 키워드 입력 후 '자동화 시작'").grid(row=5, column=0, columnspan=2, sticky=tk.W)
        ttk.Label(chrome_frame, text="⚠️ 기존 크롬은 그대로 유지됩니다", foreground="blue").grid(row=6, column=0, columnspan=2, sticky=tk.W)
        
        ttk.Button(chrome_frame, text="크롬 연결", command=self.connect_chrome).grid(row=7, column=0, pady=(5, 0))
        
        # 키워드 입력
        ttk.Label(main_frame, text="검색 키워드 (한 줄에 하나씩):").grid(row=1, column=0, sticky=tk.W, pady=(0, 5))
        
        self.keyword_text = tk.Text(main_frame, height=3, width=50)
        self.keyword_text.grid(row=2, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))
        
        # 설정 프레임
        settings_frame = ttk.LabelFrame(main_frame, text="설정", padding="10")
        settings_frame.grid(row=3, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))
        
        # 자동화 옵션
        self.auto_like_var = tk.BooleanVar(value=True)
        ttk.Checkbutton(settings_frame, text="자동 좋아요", variable=self.auto_like_var).grid(row=0, column=0, sticky=tk.W)
        
        self.auto_repost_var = tk.BooleanVar(value=False)
        ttk.Checkbutton(settings_frame, text="자동 리포스트", variable=self.auto_repost_var).grid(row=0, column=1, sticky=tk.W)
        
        self.auto_follow_var = tk.BooleanVar(value=False)
        ttk.Checkbutton(settings_frame, text="자동 팔로우", variable=self.auto_follow_var).grid(row=1, column=0, sticky=tk.W)
        
        self.auto_comment_var = tk.BooleanVar(value=False)
        ttk.Checkbutton(settings_frame, text="자동 댓글", variable=self.auto_comment_var).grid(row=1, column=1, sticky=tk.W)
        
        # 딜레이 설정
        ttk.Label(settings_frame, text="딜레이 범위 (초):").grid(row=2, column=0, sticky=tk.W, pady=(10, 0))
        
        delay_frame = ttk.Frame(settings_frame)
        delay_frame.grid(row=3, column=0, columnspan=2, sticky=tk.W, pady=(0, 10))
        
        self.delay_min_var = tk.StringVar(value="2")
        self.delay_max_var = tk.StringVar(value="5")
        
        ttk.Label(delay_frame, text="최소:").grid(row=0, column=0)
        ttk.Entry(delay_frame, textvariable=self.delay_min_var, width=5).grid(row=0, column=1, padx=(5, 10))
        
        ttk.Label(delay_frame, text="최대:").grid(row=0, column=2)
        ttk.Entry(delay_frame, textvariable=self.delay_max_var, width=5).grid(row=0, column=3, padx=(5, 0))
        
        # 스크롤 설정
        ttk.Label(settings_frame, text="스크롤 횟수:").grid(row=4, column=0, sticky=tk.W)
        self.scroll_count_var = tk.StringVar(value="10")
        ttk.Entry(settings_frame, textvariable=self.scroll_count_var, width=10).grid(row=4, column=1, sticky=tk.W, padx=(10, 0))
        
        # 팔로우 요청 수
        ttk.Label(settings_frame, text="최대 팔로우 수:").grid(row=5, column=0, sticky=tk.W)
        self.max_follow_var = tk.StringVar(value="200")
        ttk.Entry(settings_frame, textvariable=self.max_follow_var, width=10).grid(row=5, column=1, sticky=tk.W, padx=(10, 0))
        
        # 최대 포스트 수
        ttk.Label(settings_frame, text="키워드당 최대 포스트 수:").grid(row=6, column=0, sticky=tk.W)
        self.max_posts_var = tk.StringVar(value="10")
        ttk.Entry(settings_frame, textvariable=self.max_posts_var, width=10).grid(row=6, column=1, sticky=tk.W, padx=(10, 0))
        
        # 검색 기준
        ttk.Label(settings_frame, text="검색 기준:").grid(row=7, column=0, sticky=tk.W)
        self.search_criteria_var = tk.StringVar(value="인기글")
        search_criteria_combo = ttk.Combobox(settings_frame, textvariable=self.search_criteria_var, 
                                           values=["인기글", "최신글", "관련성"], width=10)
        search_criteria_combo.grid(row=7, column=1, sticky=tk.W, padx=(10, 0))
        
        # 댓글 템플릿 설정
        ttk.Label(settings_frame, text="댓글 템플릿 (한 줄에 하나씩):").grid(row=8, column=0, columnspan=2, sticky=tk.W, pady=(10, 0))
        
        self.comment_text = tk.Text(settings_frame, height=4, width=50)
        self.comment_text.grid(row=9, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(5, 0))
        self.comment_text.insert(tk.END, "좋은 글이네요!\n공감합니다!\n멋진 내용이에요!\n정말 유용한 정보네요!\n도움이 많이 됩니다!")
        
        # 버튼 프레임
        button_frame = ttk.Frame(main_frame)
        button_frame.grid(row=4, column=0, columnspan=2, pady=(20, 0))
        
        self.relogin_button = ttk.Button(button_frame, text="재로그인", command=self.relogin)
        self.relogin_button.grid(row=0, column=0, padx=(0, 10))
        
        self.log_button = ttk.Button(button_frame, text="실시간 로그 보기", command=self.show_log_window)
        self.log_button.grid(row=0, column=1, padx=(0, 10))
        
        self.start_button = ttk.Button(button_frame, text="자동화 시작", command=self.start_automation)
        self.start_button.grid(row=0, column=2, padx=(0, 10))
        
        self.stop_button = ttk.Button(button_frame, text="중지", command=self.stop_automation, state=tk.DISABLED)
        self.stop_button.grid(row=0, column=3)
        
        # 로그 프레임
        log_frame = ttk.LabelFrame(main_frame, text="로그", padding="5")
        log_frame.grid(row=5, column=0, columnspan=2, sticky=(tk.W, tk.E, tk.N, tk.S), pady=(10, 0))
        
        self.log_text = tk.Text(log_frame, height=8, width=70)
        self.log_text.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        scrollbar = ttk.Scrollbar(log_frame, orient=tk.VERTICAL, command=self.log_text.yview)
        scrollbar.grid(row=0, column=1, sticky=(tk.N, tk.S))
        self.log_text.configure(yscrollcommand=scrollbar.set)
        
        # 그리드 가중치 설정
        self.root.columnconfigure(0, weight=1)
        self.root.rowconfigure(0, weight=1)
        main_frame.columnconfigure(1, weight=1)
        main_frame.rowconfigure(5, weight=1)
        log_frame.columnconfigure(0, weight=1)
        log_frame.rowconfigure(0, weight=1)
    
    def log_message(self, message):
        """로그 메시지 추가"""
        timestamp = time.strftime('%H:%M:%S')
        log_entry = f"{timestamp} - {message}\n"
        
        # 메인 로그 창에 추가
        self.log_text.insert(tk.END, log_entry)
        self.log_text.see(tk.END)
        self.root.update()
        
        # 별도 로그 창이 있다면 거기도 업데이트
        if hasattr(self, 'log_window') and self.log_window.winfo_exists():
            try:
                self.log_window_text.insert(tk.END, log_entry)
                self.log_window_text.see(tk.END)
                self.log_window.update()
            except:
                pass
    
    def browse_chrome_driver(self):
        """크롬드라이버 찾아보기"""
        filename = filedialog.askopenfilename(
            title="크롬드라이버 선택",
            filetypes=[("실행 파일", "chromedriver*"), ("모든 파일", "*.*")]
        )
        if filename:
            self.chrome_driver_var.set(filename)
            self.log_message(f"크롬드라이버 설정: {filename}")
    
    def browse_comment_file(self):
        """댓글 파일 찾아보기 (사용하지 않음)"""
        pass
    
    def process_comments(self, comment_text):
        """댓글 텍스트 처리 - 한 줄씩 분리"""
        if not comment_text:
            return ["좋은 글이네요!"]
        
        comments = []
        lines = comment_text.split('\n')
        
        for line in lines:
            line = line.strip()
            if line:
                comments.append(line)
        
        return comments if comments else ["좋은 글이네요!"]
    
    def load_comment_file(self, filename):
        """댓글 파일 로드"""
        try:
            with open(filename, 'r', encoding='utf-8') as f:
                comments = f.read().strip()
                self.comment_text.delete("1.0", tk.END)
                self.comment_text.insert(tk.END, comments)
            self.log_message(f"댓글 파일을 로드했습니다: {filename}")
        except Exception as e:
            self.log_message(f"댓글 파일 로드 실패: {e}")
    
    def relogin(self):
        """재로그인"""
        try:
            if self.automation.driver:
                self.automation.driver.quit()
            
            self.log_message("재로그인을 시도합니다...")
            self.automation.driver = self.automation.setup_driver()
            
            if self.automation.login_check():
                self.log_message("재로그인 성공!")
            else:
                self.log_message("재로그인 실패. Chrome에서 수동으로 로그인해주세요.")
        except Exception as e:
            self.log_message(f"재로그인 중 오류: {e}")
    
    def connect_chrome(self):
        """크롬 연결 (기존 세션)"""
        try:
            import subprocess
            import os
            import time
            import requests
            
            # 디버그 모드 크롬이 이미 실행 중인지 확인
            try:
                response = requests.get("http://127.0.0.1:9222/json/version", timeout=2)
                if response.status_code == 200:
                    self.log_message("디버그 모드 크롬이 이미 실행 중입니다.")
                    self.log_message("이제 크롬에서 스레드에 로그인한 후 자동화를 시작하세요.")
                    
                    # 기존 크롬 세션에 연결 시도
                    try:
                        if not self.automation.driver:
                            self.automation.driver = self.automation.setup_driver()
                        self.log_message("기존 크롬 세션에 연결되었습니다!")
                    except Exception as e:
                        self.log_message(f"크롬 세션 연결 실패: {e}")
                    return
            except:
                pass
            
            # 기존 크롬 프로세스는 종료하지 않고, 새로운 디버그 모드 크롬만 실행
            self.log_message("새로운 디버그 모드 크롬을 실행합니다...")
            
            # 크롬을 디버그 모드로 실행 (기존 크롬과 분리)
            chrome_path = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
            if os.path.exists(chrome_path):
                # 임시 디렉토리 생성
                import tempfile
                temp_dir = tempfile.mkdtemp(prefix="chrome_debug_")
                
                cmd = [
                    chrome_path, 
                    "--remote-debugging-port=9222", 
                    f"--user-data-dir={temp_dir}",
                    "--no-first-run",
                    "--no-default-browser-check",
                    "--disable-extensions"
                ]
                
                # 백그라운드에서 크롬 실행
                subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                time.sleep(3)  # 크롬이 완전히 시작될 때까지 대기
                
                self.log_message("크롬을 디버그 모드로 실행했습니다.")
                self.log_message("이제 크롬에서 스레드에 로그인한 후 자동화를 시작하세요.")
                self.log_message("크롬이 열리면 https://www.threads.net 에 접속하여 로그인해주세요.")
            else:
                self.log_message("크롬을 찾을 수 없습니다. 수동으로 크롬을 디버그 모드로 실행해주세요.")
        except Exception as e:
            self.log_message(f"크롬 연결 중 오류: {e}")
    
    def show_log_window(self):
        """실시간 로그 창 표시"""
        if not hasattr(self, 'log_window') or not self.log_window.winfo_exists():
            self.log_window = tk.Toplevel(self.root)
            self.log_window.title("실시간 로그")
            self.log_window.geometry("800x600")
            
            # 로그 텍스트 위젯
            self.log_window_text = tk.Text(self.log_window, wrap=tk.WORD)
            self.log_window_text.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)
            
            # 스크롤바
            scrollbar = ttk.Scrollbar(self.log_window, orient=tk.VERTICAL, command=self.log_window_text.yview)
            scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
            self.log_window_text.configure(yscrollcommand=scrollbar.set)
            
            # 기존 로그 내용 복사
            self.log_window_text.insert(tk.END, self.log_text.get("1.0", tk.END))
            self.log_window_text.see(tk.END)
        else:
            self.log_window.lift()  # 창을 앞으로 가져오기
    
    def start_automation(self):
        """자동화 시작"""
        try:
            # 설정 저장
            self.automation.config.update({
                'auto_like': self.auto_like_var.get(),
                'auto_repost': self.auto_repost_var.get(),
                'auto_follow': self.auto_follow_var.get(),
                'auto_comment': self.auto_comment_var.get(),
                'delay_range': [float(self.delay_min_var.get()), float(self.delay_max_var.get())],
                'max_posts_per_keyword': int(self.max_posts_var.get()),
                'scroll_count': int(self.scroll_count_var.get()),
                'max_follow_count': int(self.max_follow_var.get()),
                'search_criteria': self.search_criteria_var.get(),
                'secret_mode': self.secret_mode_var.get(),
                'comments': self.process_comments(self.comment_text.get("1.0", tk.END).strip())
            })
            self.automation.save_config()
        except ValueError as e:
            messagebox.showerror("입력 오류", f"숫자 입력 필드에 잘못된 값이 있습니다: {e}")
            return
        except Exception as e:
            messagebox.showerror("오류", f"설정 저장 중 오류가 발생했습니다: {e}")
            return
        
        # 키워드 가져오기
        keywords = [line.strip() for line in self.keyword_text.get("1.0", tk.END).strip().split('\n') if line.strip()]
        
        if not keywords:
            messagebox.showerror("오류", "검색 키워드를 입력해주세요.")
            return
        
        # 크롬 연결 상태 확인
        try:
            if not self.automation.driver:
                self.log_message("크롬 드라이버를 초기화합니다...")
                self.automation.driver = self.automation.setup_driver()
            
            # 스레드 사이트로 이동
            self.log_message("스레드 사이트로 이동합니다...")
            self.automation.driver.get("https://www.threads.net")
            time.sleep(3)
            
            # 로그인 상태 확인
            if not self.automation.login_check():
                messagebox.showwarning("로그인 확인", "스레드에 로그인되어 있지 않습니다.\n크롬에서 스레드에 로그인한 후 다시 시도해주세요.")
                return
            else:
                self.log_message("로그인 상태 확인 완료!")
                
        except Exception as e:
            messagebox.showerror("오류", f"크롬 연결 중 오류가 발생했습니다: {e}")
            return
        
        # UI 상태 변경
        self.start_button.config(state=tk.DISABLED)
        self.stop_button.config(state=tk.NORMAL)
        self.automation.is_running = True
        
        # 자동화 스레드 시작
        self.automation_thread = threading.Thread(
            target=self.run_automation,
            args=(keywords,)
        )
        self.automation_thread.daemon = True
        self.automation_thread.start()
    
    def run_automation(self, keywords):
        """자동화 실행 (별도 스레드)"""
        try:
            self.log_message("자동화를 시작합니다...")
            self.log_message(f"처리할 키워드: {', '.join(keywords)}")
            
            # 자동화 객체의 로그 메시지를 GUI로 전달
            original_print = print
            def custom_print(*args, **kwargs):
                message = ' '.join(str(arg) for arg in args)
                self.root.after(0, lambda: self.log_message(message))
                original_print(*args, **kwargs)
            
            # print 함수를 임시로 교체
            import builtins
            builtins.print = custom_print
            
            try:
                self.automation.start_automation(keywords)
                self.log_message("자동화가 완료되었습니다.")
            finally:
                # print 함수 복원
                builtins.print = original_print
                
        except Exception as e:
            self.log_message(f"오류 발생: {e}")
        finally:
            # UI 상태 복원
            self.root.after(0, self.reset_ui)
    
    def stop_automation(self):
        """자동화 중지"""
        self.automation.stop_automation()
        self.log_message("자동화를 중지합니다...")
        self.reset_ui()
    
    def reset_ui(self):
        """UI 상태 복원"""
        self.start_button.config(state=tk.NORMAL)
        self.stop_button.config(state=tk.DISABLED)
    
    def run(self):
        """GUI 실행"""
        self.root.mainloop()

if __name__ == "__main__":
    app = ThreadAutomationGUI()
    app.run() 