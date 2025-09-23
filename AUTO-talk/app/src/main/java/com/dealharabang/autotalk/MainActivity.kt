package com.dealharabang.autotalk

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.dealharabang.autotalk.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViews()
        checkAccessibilityService()
    }
    
    private fun setupViews() {
        binding.apply {
            // 카카오톡 자동 메시지 시작 버튼
            btnStartAutoMessage.setOnClickListener {
                if (isAccessibilityServiceEnabled()) {
                    startKakaoAutoMessageService()
                } else {
                    requestAccessibilityPermission()
                }
            }
            
            // 카카오톡 자동 메시지 중지 버튼
            btnStopAutoMessage.setOnClickListener {
                stopKakaoAutoMessageService()
            }
            
            // 설정 버튼
            btnSettings.setOnClickListener {
                openAccessibilitySettings()
            }
        }
    }
    
    private fun checkAccessibilityService() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "접근성 서비스를 활성화해주세요", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains("${packageName}/.KakaoTalkAccessibilityService") == true
    }
    
    private fun requestAccessibilityPermission() {
        Toast.makeText(this, "접근성 서비스를 활성화해주세요", Toast.LENGTH_LONG).show()
        openAccessibilitySettings()
    }
    
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
    
    private fun startKakaoAutoMessageService() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val autoMessageWork = PeriodicWorkRequestBuilder<KakaoAutoMessageWorker>(
            15, TimeUnit.MINUTES // 최소 15분 간격
        )
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "kakao_auto_message_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                autoMessageWork
            )
            
        binding.tvStatus.text = "카카오톡 자동 메시지 서비스가 시작되었습니다"
        Toast.makeText(this, "카카오톡 자동 메시지 서비스 시작", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopKakaoAutoMessageService() {
        WorkManager.getInstance(this).cancelUniqueWork("kakao_auto_message_work")
        binding.tvStatus.text = "카카오톡 자동 메시지 서비스가 중지되었습니다"
        Toast.makeText(this, "카카오톡 자동 메시지 서비스 중지", Toast.LENGTH_SHORT).show()
    }
}
