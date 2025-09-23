package com.example.groupmessenger.util

import android.content.Context
import android.telephony.SmsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    fun sendBulkSms(phoneNumbers: List<String>, message: String): Boolean {
        return try {
            val smsManager = SmsManager.getDefault()
            
            phoneNumbers.forEach { phoneNumber ->
                // SMS가 160자를 초과하는 경우 여러 개로 분할
                val parts = smsManager.divideMessage(message)
                
                if (parts.size == 1) {
                    smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                } else {
                    smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun sendSingleSms(phoneNumber: String, message: String): Boolean {
        return try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            
            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

