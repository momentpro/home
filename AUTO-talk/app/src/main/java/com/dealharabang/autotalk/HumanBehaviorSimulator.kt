package com.dealharabang.autotalk

import android.util.Log
import kotlinx.coroutines.delay
import java.util.*
import kotlin.random.Random

/**
 * 사람처럼 보이는 행동 패턴을 시뮬레이션하는 클래스
 * 카카오톡의 보안 시스템 탐지를 회피하기 위한 알고리즘
 */
class HumanBehaviorSimulator {

    companion object {
        private const val TAG = "HumanBehaviorSimulator"
        
        // 시간대별 활동 패턴 (0~23시)
        private val ACTIVITY_PATTERNS = mapOf(
            0 to 0.1f,   // 자정
            1 to 0.05f,  // 새벽 1시
            2 to 0.03f,  // 새벽 2시
            3 to 0.02f,  // 새벽 3시
            4 to 0.02f,  // 새벽 4시
            5 to 0.03f,  // 새벽 5시
            6 to 0.1f,   // 아침 6시
            7 to 0.3f,   // 아침 7시
            8 to 0.5f,   // 아침 8시
            9 to 0.7f,   // 오전 9시
            10 to 0.8f,  // 오전 10시
            11 to 0.9f,  // 오전 11시
            12 to 1.0f,  // 점심 12시 (피크)
            13 to 0.8f,  // 오후 1시
            14 to 0.7f,  // 오후 2시
            15 to 0.8f,  // 오후 3시
            16 to 0.9f,  // 오후 4시
            17 to 0.8f,  // 오후 5시
            18 to 0.7f,  // 저녁 6시
            19 to 0.8f,  // 저녁 7시
            20 to 0.9f,  // 저녁 8시
            21 to 0.8f,  // 저녁 9시
            22 to 0.6f,  // 저녁 10시
            23 to 0.3f   // 저녁 11시
        )
    }

    private val random = Random(System.currentTimeMillis())
    private var lastMessageTime = 0L
    private var todayMessageCount = 0
    private var lastResetDate = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

    /**
     * 현재 시간이 메시지 발송하기 적절한 시간인지 확인
     */
    fun isGoodTimeToSendMessage(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentDay = calendar.get(Calendar.DAY_OF_YEAR)
        
        // 날짜가 바뀌면 카운트 리셋
        if (currentDay != lastResetDate) {
            todayMessageCount = 0
            lastResetDate = currentDay
        }
        
        // 하루 최대 메시지 수 제한 (20~50개 사이 랜덤)
        val maxDailyMessages = random.nextInt(20, 51)
        if (todayMessageCount >= maxDailyMessages) {
            Log.d(TAG, "일일 메시지 한도 초과: $todayMessageCount/$maxDailyMessages")
            return false
        }
        
        // 시간대별 활동 패턴 확인
        val activityRate = ACTIVITY_PATTERNS[currentHour] ?: 0.5f
        val shouldSend = random.nextFloat() < activityRate
        
        Log.d(TAG, "시간대 활동성 확인: ${currentHour}시, 확률: ${activityRate}, 결과: $shouldSend")
        return shouldSend
    }

    /**
     * 메시지 간 적절한 지연 시간 계산
     */
    fun calculateMessageDelay(): Long {
        val currentTime = System.currentTimeMillis()
        
        // 이전 메시지와의 간격이 너무 짧으면 추가 지연
        val timeSinceLastMessage = currentTime - lastMessageTime
        val minimumInterval = 30 * 1000L // 최소 30초
        
        if (timeSinceLastMessage < minimumInterval) {
            val additionalDelay = minimumInterval - timeSinceLastMessage
            Log.d(TAG, "최소 간격 보장을 위한 추가 지연: ${additionalDelay}ms")
        }
        
        // 기본 지연: 1분 ~ 10분 사이 랜덤
        val baseDelay = random.nextLong(60 * 1000L, 10 * 60 * 1000L)
        
        // 시간대에 따른 추가 지연
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val hourMultiplier = when (currentHour) {
            in 0..5 -> 3.0f    // 새벽: 3배 더 느리게
            in 6..8 -> 1.5f    // 아침: 1.5배 느리게
            in 9..11 -> 1.0f   // 오전: 정상
            in 12..13 -> 0.8f  // 점심: 약간 빠르게
            in 14..17 -> 1.0f  // 오후: 정상
            in 18..21 -> 0.9f  // 저녁: 약간 빠르게
            in 22..23 -> 1.5f  // 밤: 1.5배 느리게
            else -> 1.0f
        }
        
        val finalDelay = (baseDelay * hourMultiplier).toLong()
        lastMessageTime = currentTime + finalDelay
        
        Log.d(TAG, "메시지 지연 시간 계산: ${finalDelay}ms (${finalDelay/1000}초)")
        return finalDelay
    }

    /**
     * 사람처럼 타이핑하는 시뮬레이션
     */
    suspend fun simulateTyping(text: String, onCharTyped: (String) -> Unit) {
        Log.d(TAG, "타이핑 시뮬레이션 시작: $text")
        
        // 타이핑 속도: 50~150ms per character
        val baseTypingSpeed = random.nextLong(50, 151)
        
        var currentText = ""
        for (i in text.indices) {
            val char = text[i]
            currentText += char
            
            // 글자별 타이핑 속도 변화 (사람처럼)
            val charDelay = when {
                char == ' ' -> baseTypingSpeed * 1.5 // 공백은 더 느리게
                char.isUpperCase() -> baseTypingSpeed * 1.2 // 대문자는 약간 느리게
                i == 0 -> baseTypingSpeed * 2 // 첫 글자는 더 느리게 (생각하는 시간)
                else -> baseTypingSpeed + random.nextLong(-20, 21) // ±20ms 변화
            }.toLong()
            
            onCharTyped(currentText)
            delay(charDelay)
            
            // 가끔 실수처럼 백스페이스 후 다시 타이핑 (2% 확률)
            if (random.nextFloat() < 0.02f && i > 0) {
                delay(random.nextLong(100, 300))
                currentText = currentText.dropLast(1)
                onCharTyped(currentText)
                delay(random.nextLong(50, 150))
                currentText += char
                onCharTyped(currentText)
            }
        }
        
        // 타이핑 완료 후 전송 전 잠시 대기 (검토하는 시간)
        delay(random.nextLong(500, 2000))
        Log.d(TAG, "타이핑 시뮬레이션 완료")
    }

    /**
     * 화면 스크롤 시뮬레이션 (사람처럼 자연스럽게)
     */
    suspend fun simulateScrolling() {
        if (random.nextFloat() < 0.3f) { // 30% 확률로 스크롤
            Log.d(TAG, "화면 스크롤 시뮬레이션")
            delay(random.nextLong(500, 1500))
        }
    }

    /**
     * 메시지 읽기 시뮬레이션 (메시지 길이에 따른 읽기 시간)
     */
    suspend fun simulateMessageReading(messageLength: Int) {
        // 1글자당 50~100ms 읽기 시간
        val readingTime = messageLength * random.nextLong(50, 101)
        val finalReadingTime = readingTime.coerceAtLeast(1000L).coerceAtMost(5000L)
        
        Log.d(TAG, "메시지 읽기 시뮬레이션: ${finalReadingTime}ms")
        delay(finalReadingTime)
    }

    /**
     * 랜덤한 앱 사용 패턴 시뮬레이션
     */
    suspend fun simulateRandomActivity() {
        if (random.nextFloat() < 0.1f) { // 10% 확률로 다른 활동
            Log.d(TAG, "랜덤 활동 시뮬레이션")
            
            when (random.nextInt(3)) {
                0 -> {
                    // 잠시 다른 앱으로 전환하는 것처럼
                    delay(random.nextLong(2000, 5000))
                }
                1 -> {
                    // 메시지를 다시 읽는 것처럼
                    delay(random.nextLong(1000, 3000))
                }
                2 -> {
                    // 생각하는 시간
                    delay(random.nextLong(3000, 8000))
                }
            }
        }
    }

    /**
     * 메시지 발송 후 카운트 증가
     */
    fun incrementMessageCount() {
        todayMessageCount++
        Log.d(TAG, "오늘 발송한 메시지 수: $todayMessageCount")
    }

    /**
     * 현재 활동이 의심스러운지 확인
     */
    fun isSuspiciousActivity(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastMessage = currentTime - lastMessageTime
        
        // 너무 빠른 연속 발송은 의심스러움
        if (timeSinceLastMessage < 10 * 1000L) { // 10초 이내
            Log.w(TAG, "의심스러운 활동 감지: 너무 빠른 연속 발송")
            return true
        }
        
        // 하루에 너무 많은 메시지는 의심스러움
        if (todayMessageCount > 100) {
            Log.w(TAG, "의심스러운 활동 감지: 일일 메시지 수 과다")
            return true
        }
        
        return false
    }

    /**
     * 주말/평일에 따른 활동 패턴 조정
     */
    fun getWeekendActivityMultiplier(): Float {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        return when (dayOfWeek) {
            Calendar.SATURDAY, Calendar.SUNDAY -> 0.7f // 주말은 70% 활동
            else -> 1.0f // 평일은 100% 활동
        }
    }
}

