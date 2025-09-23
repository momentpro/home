package com.example.groupmessenger.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionUtil {
    
    const val PERMISSION_READ_CONTACTS = Manifest.permission.READ_CONTACTS
    const val PERMISSION_SEND_SMS = Manifest.permission.SEND_SMS
    
    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            PERMISSION_READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            PERMISSION_SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun getAllRequiredPermissions(): Array<String> {
        return arrayOf(
            PERMISSION_READ_CONTACTS,
            PERMISSION_SEND_SMS
        )
    }
}









