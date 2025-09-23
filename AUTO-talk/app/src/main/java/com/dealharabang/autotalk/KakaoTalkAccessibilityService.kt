package com.dealharabang.autotalk

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

class KakaoTalkAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "KakaoTalkAccessibility"
        var instance: KakaoTalkAccessibilityService? = null
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isProcessing = false
    private val humanBehavior = HumanBehaviorSimulator()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "접근성 서비스 연결됨")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        Log.d(TAG, "접근성 서비스 종료됨")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 카카오톡 앱에서 발생하는 이벤트 처리
        event?.let {
            if (it.packageName == "com.kakao.talk") {
                Log.d(TAG, "카카오톡 이벤트 감지: ${it.eventType}")
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "접근성 서비스 중단됨")
    }

    /**
     * 카카오톡에서 자동으로 메시지를 발송하는 메인 함수 (휴먼 시뮬레이션 적용)
     */
    fun sendKakaoMessages(messageList: List<KakaoMessage>) {
        if (isProcessing) {
            Log.d(TAG, "이미 처리 중입니다")
            return
        }

        serviceScope.launch {
            try {
                isProcessing = true
                Log.d(TAG, "카카오톡 자동 메시지 발송 시작: ${messageList.size}개")

                for ((index, message) in messageList.withIndex()) {
                    // 현재 시간이 메시지 발송하기 적절한지 확인
                    if (!humanBehavior.isGoodTimeToSendMessage()) {
                        Log.d(TAG, "현재 시간은 메시지 발송에 적절하지 않음. 다음 메시지로 스킵")
                        continue
                    }

                    // 의심스러운 활동 패턴 체크
                    if (humanBehavior.isSuspiciousActivity()) {
                        Log.w(TAG, "의심스러운 활동 패턴 감지. 발송 중단")
                        break
                    }

                    // 메시지 발송
                    if (sendSingleMessageWithHumanBehavior(message)) {
                        humanBehavior.incrementMessageCount()
                    }

                    // 다음 메시지까지 휴먼라이크한 지연
                    if (index < messageList.size - 1) {
                        val delayTime = humanBehavior.calculateMessageDelay()
                        Log.d(TAG, "다음 메시지까지 대기: ${delayTime/1000}초")
                        delay(delayTime)
                    }
                }

                Log.d(TAG, "모든 메시지 발송 완료")
            } catch (e: Exception) {
                Log.e(TAG, "메시지 발송 중 오류", e)
            } finally {
                isProcessing = false
            }
        }
    }

    /**
     * 개별 메시지 발송
     */
    private suspend fun sendSingleMessageWithHumanBehavior(message: KakaoMessage): Boolean {
        try {
            Log.d(TAG, "메시지 발송 시작: ${message.recipientName}")

            // 랜덤한 활동 시뮬레이션
            humanBehavior.simulateRandomActivity()

            // 1. 카카오톡 앱 실행 확인
            if (!isKakaoTalkRunning()) {
                openKakaoTalk()
                delay(kotlin.random.Random.nextLong(2000, 5000)) // 앱 로딩 시간 랜덤화
            }

            // 화면 스크롤 시뮬레이션
            humanBehavior.simulateScrolling()

            // 2. 검색 기능으로 대화상대 찾기
            if (searchRecipient(message.recipientName)) {
                delay(kotlin.random.Random.nextLong(800, 1500))

                // 3. 채팅방 진입
                if (enterChatRoom()) {
                    delay(kotlin.random.Random.nextLong(1000, 2000))

                    // 기존 메시지 읽기 시뮬레이션
                    humanBehavior.simulateMessageReading(message.messageText.length)

                    // 4. 메시지 입력 및 전송
                    if (inputAndSendMessage(message.messageText)) {
                        Log.d(TAG, "메시지 발송 성공: ${message.recipientName}")
                        
                        // 5. 뒤로가기 (사람처럼 자연스럽게)
                        delay(kotlin.random.Random.nextLong(500, 1500))
                        goBack()
                        delay(kotlin.random.Random.nextLong(800, 1500))
                        
                        return true
                    } else {
                        Log.e(TAG, "메시지 발송 실패: ${message.recipientName}")
                        goBack()
                        return false
                    }
                }
            }
            
            return false
        } catch (e: Exception) {
            Log.e(TAG, "개별 메시지 발송 실패: ${message.recipientName}", e)
            return false
        }
    }

    /**
     * 카카오톡 앱이 실행 중인지 확인
     */
    private fun isKakaoTalkRunning(): Boolean {
        val rootNode = rootInActiveWindow
        return rootNode?.packageName == "com.kakao.talk"
    }

    /**
     * 카카오톡 앱 실행
     */
    private fun openKakaoTalk() {
        val intent = packageManager.getLaunchIntentForPackage("com.kakao.talk")
        intent?.let {
            it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
        }
    }

    /**
     * 대화상대 검색
     */
    private suspend fun searchRecipient(recipientName: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false

        // 검색 버튼 찾기 (돋보기 아이콘)
        val searchButton = findNodeByText(rootNode, "검색") 
            ?: findNodeByContentDescription(rootNode, "검색")

        if (searchButton != null) {
            performClick(searchButton)
            delay(1000)

            // 검색창에 이름 입력
            val searchEditText = findNodeByClassName(rootNode, "android.widget.EditText")
            if (searchEditText != null) {
                inputText(searchEditText, recipientName)
                delay(1000)

                // 검색 결과에서 첫 번째 항목 클릭
                val searchResult = findNodeByText(rootNode, recipientName)
                if (searchResult != null) {
                    performClick(searchResult)
                    return true
                }
            }
        }

        return false
    }

    /**
     * 채팅방 진입
     */
    private fun enterChatRoom(): Boolean {
        // 검색 결과를 클릭하면 자동으로 채팅방으로 이동됨
        return true
    }

    /**
     * 메시지 입력 및 전송
     */
    private suspend fun inputAndSendMessage(messageText: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false

        // 메시지 입력창 찾기
        val messageInput = findNodeByClassName(rootNode, "android.widget.EditText")
        if (messageInput != null) {
            inputText(messageInput, messageText)
            delay(500)

            // 전송 버튼 찾기
            val sendButton = findNodeByText(rootNode, "전송") 
                ?: findNodeByContentDescription(rootNode, "전송")
                ?: findNodeByClassName(rootNode, "android.widget.ImageButton")

            if (sendButton != null) {
                performClick(sendButton)
                return true
            }
        }

        return false
    }

    /**
     * 뒤로가기
     */
    private fun goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * 텍스트로 노드 찾기
     */
    private fun findNodeByText(rootNode: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        return nodes.firstOrNull()
    }

    /**
     * ContentDescription으로 노드 찾기
     */
    private fun findNodeByContentDescription(rootNode: AccessibilityNodeInfo, description: String): AccessibilityNodeInfo? {
        if (rootNode.contentDescription?.contains(description) == true) {
            return rootNode
        }

        for (i in 0 until rootNode.childCount) {
            rootNode.getChild(i)?.let { child ->
                val result = findNodeByContentDescription(child, description)
                if (result != null) return result
            }
        }

        return null
    }

    /**
     * 클래스명으로 노드 찾기
     */
    private fun findNodeByClassName(rootNode: AccessibilityNodeInfo, className: String): AccessibilityNodeInfo? {
        if (rootNode.className?.toString() == className) {
            return rootNode
        }

        for (i in 0 until rootNode.childCount) {
            rootNode.getChild(i)?.let { child ->
                val result = findNodeByClassName(child, className)
                if (result != null) return result
            }
        }

        return null
    }

    /**
     * 노드 클릭
     */
    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /**
     * 텍스트 입력
     */
    private fun inputText(node: AccessibilityNodeInfo, text: String): Boolean {
        val bundle = android.os.Bundle()
        bundle.putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
    }
}

/**
 * 카카오톡 메시지 데이터 클래스
 */
data class KakaoMessage(
    val recipientName: String,  // 받는 사람 이름
    val messageText: String     // 보낼 메시지 내용
)

