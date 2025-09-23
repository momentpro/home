package com.autocaller.app.service;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class CallEndAccessibilityService extends AccessibilityService {
    private static final String TAG = "CallEndAccessibilityService";
    private Handler handler;
    private BroadcastReceiver hangUpReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        setupBroadcastReceiver();
        Log.d(TAG, "CallEndAccessibilityService created");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "CallEndAccessibilityService connected");
        
        // Test log to confirm service is running
        Log.i(TAG, "AccessibilityService is now active and ready");
    }

    private void setupBroadcastReceiver() {
        hangUpReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.autocaller.HANG_UP_CALL".equals(intent.getAction())) {
                    String phoneNumber = intent.getStringExtra("phone_number");
                    Log.d(TAG, "Received hang up request for: " + phoneNumber);
                    performHangUp();
                }
            }
        };
        
        IntentFilter filter = new IntentFilter("com.autocaller.HANG_UP_CALL");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(hangUpReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(hangUpReceiver, filter);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // We mainly use this service for performing actions, not for listening to events
        // But we can log events for debugging
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            Log.d(TAG, "Window changed: " + packageName);
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "CallEndAccessibilityService interrupted");
    }

    private void performHangUp() {
        Log.d(TAG, "Attempting to hang up call");
        
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.w(TAG, "Root node is null, cannot perform hang up");
            return;
        }

        // Try multiple strategies to find the end call button
        boolean success = false;
        
        // Strategy 1: Look for end call button by content description
        success = findAndClickEndCallButton(rootNode);
        
        // Strategy 2: Look for red button (common for end call)
        if (!success) {
            success = findAndClickRedButton(rootNode);
        }
        
        // Strategy 3: Look for button with specific text
        if (!success) {
            success = findAndClickButtonWithText(rootNode, new String[]{
                "통화 종료", "End call", "종료", "End", "끊기", "Hang up"
            });
        }
        
        // Strategy 4: Look by resource ID (device specific)
        if (!success) {
            success = findAndClickByResourceId(rootNode, new String[]{
                "com.android.incallui:id/endButton",
                "com.android.dialer:id/incall_end_call",
                "com.samsung.android.incallui:id/endButton",
                "com.google.android.dialer:id/incall_end_call"
            });
        }
        
        if (success) {
            Log.d(TAG, "Successfully performed hang up action");
        } else {
            Log.w(TAG, "Could not find end call button");
        }
        
        rootNode.recycle();
    }

    private boolean findAndClickEndCallButton(AccessibilityNodeInfo node) {
        if (node == null) return false;

        // Check current node
        CharSequence contentDesc = node.getContentDescription();
        if (contentDesc != null) {
            String desc = contentDesc.toString().toLowerCase();
            if (desc.contains("end call") || desc.contains("통화 종료") || 
                desc.contains("hang up") || desc.contains("끊기")) {
                
                if (node.isClickable()) {
                    Log.d(TAG, "Found end call button by content description: " + desc);
                    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }

        // Recursively check child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (findAndClickEndCallButton(child)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        
        return false;
    }

    private boolean findAndClickRedButton(AccessibilityNodeInfo node) {
        if (node == null) return false;

        // This is a heuristic approach - looking for buttons that might be red
        // We can't directly check color, but we can look for patterns
        String className = node.getClassName() != null ? node.getClassName().toString() : "";
        
        if (className.contains("Button") || className.contains("ImageButton")) {
            CharSequence contentDesc = node.getContentDescription();
            CharSequence text = node.getText();
            
            // Look for typical end call indicators
            if ((contentDesc != null && (contentDesc.toString().toLowerCase().contains("end") ||
                    contentDesc.toString().toLowerCase().contains("종료"))) ||
                (text != null && (text.toString().toLowerCase().contains("end") ||
                    text.toString().toLowerCase().contains("종료")))) {
                
                if (node.isClickable()) {
                    Log.d(TAG, "Found potential end call button: " + 
                          (contentDesc != null ? contentDesc : text));
                    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }

        // Recursively check child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (findAndClickRedButton(child)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        
        return false;
    }

    private boolean findAndClickButtonWithText(AccessibilityNodeInfo node, String[] searchTexts) {
        if (node == null) return false;

        CharSequence text = node.getText();
        CharSequence contentDesc = node.getContentDescription();
        
        for (String searchText : searchTexts) {
            if ((text != null && text.toString().toLowerCase().contains(searchText.toLowerCase())) ||
                (contentDesc != null && contentDesc.toString().toLowerCase().contains(searchText.toLowerCase()))) {
                
                if (node.isClickable()) {
                    Log.d(TAG, "Found end call button with text: " + searchText);
                    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }

        // Recursively check child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (findAndClickButtonWithText(child, searchTexts)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        
        return false;
    }

    private boolean findAndClickByResourceId(AccessibilityNodeInfo node, String[] resourceIds) {
        if (node == null) return false;

        String viewId = node.getViewIdResourceName();
        if (viewId != null) {
            for (String resourceId : resourceIds) {
                if (viewId.equals(resourceId)) {
                    if (node.isClickable()) {
                        Log.d(TAG, "Found end call button by resource ID: " + resourceId);
                        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
            }
        }

        // Recursively check child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (findAndClickByResourceId(child, resourceIds)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (hangUpReceiver != null) {
            unregisterReceiver(hangUpReceiver);
        }
        
        Log.d(TAG, "CallEndAccessibilityService destroyed");
    }
}
