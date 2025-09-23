package com.autocaller.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class PhoneStateReceiver extends BroadcastReceiver {
    private static final String TAG = "PhoneStateReceiver";
    
    public static final String ACTION_PHONE_STATE_CHANGED = "com.autocaller.PHONE_STATE_CHANGED";
    public static final String EXTRA_PHONE_STATE = "phone_state";
    public static final String EXTRA_PHONE_NUMBER = "phone_number";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            
            Log.d(TAG, "Phone state changed: " + state + ", number: " + phoneNumber);
            
            // Broadcast to local components
            Intent localIntent = new Intent(ACTION_PHONE_STATE_CHANGED);
            localIntent.putExtra(EXTRA_PHONE_STATE, state);
            localIntent.putExtra(EXTRA_PHONE_NUMBER, phoneNumber);
            
            LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
        }
    }
}

