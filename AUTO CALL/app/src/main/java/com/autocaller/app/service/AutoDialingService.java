package com.autocaller.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telecom.TelecomManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.autocaller.app.MainActivity;
import com.autocaller.app.R;
import com.autocaller.app.model.PhoneNumber;

import java.util.ArrayList;
import java.util.List;

public class AutoDialingService extends Service {
    private static final String TAG = "AutoDialingService";
    private static final String CHANNEL_ID = "AutoDialingChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final long CALL_TIMEOUT = 8000; // 8 seconds - very fast
    private static final long CALL_DELAY = 500; // 0.5 seconds - ultra fast

    // Intent actions for communication
    public static final String ACTION_UPDATE_STATUS = "com.autocaller.UPDATE_STATUS";
    public static final String ACTION_UPDATE_PROGRESS = "com.autocaller.UPDATE_PROGRESS";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_CURRENT_INDEX = "current_index";
    public static final String EXTRA_PHONE_NUMBER = "phone_number";

    private final IBinder binder = new LocalBinder();
    private List<PhoneNumber> phoneNumbers = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isDialing = false;
    private Handler handler;
    private Runnable timeoutRunnable;
    private TelephonyManager telephonyManager;
    private TelecomManager telecomManager;
    private PhoneStateListener phoneStateListener;
    private long callStartTime = 0;

    public enum ServiceStatus {
        IDLE, RUNNING, PAUSED, COMPLETED
    }

    private ServiceStatus currentStatus = ServiceStatus.IDLE;

    public class LocalBinder extends Binder {
        public AutoDialingService getService() {
            return AutoDialingService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telecomManager = (TelecomManager) getSystemService(TELECOM_SERVICE);
        createNotificationChannel();
        setupPhoneStateListener();
        Log.d(TAG, "AutoDialingService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Auto Dialing Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("자동 다이얼링 서비스가 실행 중입니다.");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.auto_dialing_service_title))
                .setContentText(getString(R.string.auto_dialing_service_content))
                .setSmallIcon(R.drawable.ic_phone)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void setupPhoneStateListener() {
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                Log.d(TAG, "Call state changed: " + state + ", phone: " + phoneNumber);
                
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Call ended - move to next number
                        onCallEnded();
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        // Incoming call - pause dialing
                        pauseDialing();
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        // Call connected - check if it's human or voicemail
                        onCallConnected();
                        break;
                }
            }
        };

        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = new ArrayList<>(phoneNumbers);
        this.currentIndex = 0;
        broadcastProgress();
    }

    public void startDialing() {
        if (phoneNumbers.isEmpty()) {
            Log.w(TAG, "No phone numbers to dial");
            return;
        }

        currentStatus = ServiceStatus.RUNNING;
        isDialing = true;
        dialNextNumber();
        broadcastStatusUpdate("자동 다이얼링 시작됨");
    }

    public void stopDialing() {
        currentStatus = ServiceStatus.IDLE;
        isDialing = false;
        
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
        
        broadcastStatusUpdate("자동 다이얼링 중지됨");
    }

    public void pauseDialing() {
        if (currentStatus == ServiceStatus.RUNNING) {
            currentStatus = ServiceStatus.PAUSED;
            isDialing = false;
            
            if (timeoutRunnable != null) {
                handler.removeCallbacks(timeoutRunnable);
                timeoutRunnable = null;
            }
            
            broadcastStatusUpdate("자동 다이얼링 일시중지됨");
        }
    }

    public void resumeDialing() {
        if (currentStatus == ServiceStatus.PAUSED) {
            currentStatus = ServiceStatus.RUNNING;
            isDialing = true;
            dialNextNumber();
            broadcastStatusUpdate("자동 다이얼링 재시작됨");
        }
    }

    private void dialNextNumber() {
        if (!isDialing || currentIndex >= phoneNumbers.size()) {
            if (currentIndex >= phoneNumbers.size()) {
                currentStatus = ServiceStatus.COMPLETED;
                broadcastStatusUpdate("모든 번호 다이얼 완료");
            }
            return;
        }

        PhoneNumber phoneNumber = phoneNumbers.get(currentIndex);
        phoneNumber.setStatus(PhoneNumber.CallStatus.DIALING);
        
        Log.d(TAG, "Dialing: " + phoneNumber.getNumber() + " (index: " + currentIndex + ")");
        
        try {
            callStartTime = System.currentTimeMillis();
            
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber.getNumber()));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(callIntent);
            
            // Set timeout to automatically move to next number
            setupTimeout();
            broadcastProgress();
            
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for phone call", e);
            phoneNumber.setStatus(PhoneNumber.CallStatus.FAILED);
            moveToNextNumber();
        }
    }

    private void setupTimeout() {
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Call timeout - moving to next number");
                
                // Try to end current call using TelecomManager
                boolean callEnded = endCurrentCall();
                Log.d(TAG, "Call end attempt result: " + callEnded);
                
                // Mark current number as completed (no answer)
                if (currentIndex < phoneNumbers.size()) {
                    phoneNumbers.get(currentIndex).setStatus(PhoneNumber.CallStatus.COMPLETED);
                    broadcastProgress();
                }
                
                // Move to next number (with delay if call couldn't be ended)
                long delay = callEnded ? CALL_DELAY : 3000; // Wait longer if call is still active
                handler.postDelayed(() -> moveToNextNumber(), delay);
            }
        };
        
        handler.postDelayed(timeoutRunnable, CALL_TIMEOUT);
    }

    private void onCallConnected() {
        long connectionTime = System.currentTimeMillis() - callStartTime;
        Log.d(TAG, "Call connected after " + connectionTime + "ms");
        
        // If connected very quickly (within 4 seconds), likely voicemail
        if (connectionTime < 4000) {
            Log.w(TAG, "VOICEMAIL DETECTED - Quick connection");
            
            // Mark as completed and move to next number after short delay
            if (currentIndex < phoneNumbers.size()) {
                phoneNumbers.get(currentIndex).setStatus(PhoneNumber.CallStatus.COMPLETED);
                broadcastProgress();
            }
            
            // Cancel timeout and move to next number
            if (timeoutRunnable != null) {
                handler.removeCallbacks(timeoutRunnable);
                timeoutRunnable = null;
            }
            
            handler.postDelayed(this::moveToNextNumber, 3000); // 3 seconds for voicemail
            
        } else {
            // Real human answered - cancel timeout
            Log.d(TAG, "HUMAN ANSWERED");
            
            if (timeoutRunnable != null) {
                handler.removeCallbacks(timeoutRunnable);
                timeoutRunnable = null;
            }
            
            if (currentIndex < phoneNumbers.size()) {
                phoneNumbers.get(currentIndex).setStatus(PhoneNumber.CallStatus.ANSWERED);
                broadcastProgress();
            }
        }
    }

    private void onCallEnded() {
        Log.d(TAG, "Call ended - moving to next number");
        
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
        
        // Move to next number after short delay
        handler.postDelayed(this::moveToNextNumber, CALL_DELAY);
    }

    private void moveToNextNumber() {
        currentIndex++;
        broadcastProgress();
        
        if (isDialing && currentIndex < phoneNumbers.size()) {
            Log.d(TAG, "Moving to next number: " + currentIndex);
            dialNextNumber();
        } else if (currentIndex >= phoneNumbers.size()) {
            currentStatus = ServiceStatus.COMPLETED;
            isDialing = false;
            broadcastStatusUpdate("모든 번호 다이얼 완료");
        }
    }

    private void broadcastStatusUpdate(String status) {
        Intent intent = new Intent(ACTION_UPDATE_STATUS);
        intent.putExtra(EXTRA_STATUS, status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastProgress() {
        Intent intent = new Intent(ACTION_UPDATE_PROGRESS);
        intent.putExtra(EXTRA_CURRENT_INDEX, currentIndex);
        if (currentIndex < phoneNumbers.size()) {
            intent.putExtra(EXTRA_PHONE_NUMBER, phoneNumbers.get(currentIndex).getNumber());
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Getters for MainActivity
    public ServiceStatus getCurrentStatus() {
        return currentStatus;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getTotalCount() {
        return phoneNumbers.size();
    }

    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (telephonyManager != null && phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
        }
        
        Log.d(TAG, "AutoDialingService destroyed");
    }

    private boolean endCurrentCall() {
        Log.d(TAG, "Attempting to end current call using TelecomManager");
        
        try {
            if (telecomManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Android 9+ method
                boolean result = telecomManager.endCall();
                Log.d(TAG, "TelecomManager.endCall() result: " + result);
                return result;
            }
        } catch (SecurityException e) {
            Log.w(TAG, "TelecomManager.endCall() - Security exception: " + e.getMessage());
        } catch (Exception e) {
            Log.w(TAG, "TelecomManager.endCall() failed: " + e.getMessage());
        }
        
        // Fallback: Try reflection method
        try {
            if (telephonyManager != null) {
                telephonyManager.getClass().getMethod("endCall").invoke(telephonyManager);
                Log.d(TAG, "TelephonyManager reflection endCall attempted");
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "TelephonyManager reflection failed: " + e.getMessage());
        }
        
        // Final fallback: Send intent to end call
        try {
            Intent intent = new Intent("android.intent.action.CALL_BUTTON");
            sendBroadcast(intent);
            Log.d(TAG, "CALL_BUTTON broadcast sent");
            return true;
        } catch (Exception e) {
            Log.w(TAG, "CALL_BUTTON broadcast failed: " + e.getMessage());
        }
        
        Log.w(TAG, "All call ending methods failed");
        return false;
    }
}