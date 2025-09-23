package com.autocaller.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.autocaller.app.adapter.PhoneNumberAdapter;
import com.autocaller.app.model.PhoneNumber;
import com.autocaller.app.service.AutoDialingService;
import com.autocaller.app.util.FileImportUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSIONS = 100;

    // UI Components
    private TextView tvStatus;
    private TextView tvProgress;
    private Button btnImportList;
    private Button btnStartStop;
    private RecyclerView recyclerView;
    private PhoneNumberAdapter adapter;

    // Service related
    private AutoDialingService dialingService;
    private boolean serviceBound = false;

    // File selection
    private ActivityResultLauncher<String[]> filePickerLauncher;

    // Broadcast receivers
    private BroadcastReceiver statusReceiver;
    private BroadcastReceiver progressReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupRecyclerView();
        setupFilePickerLauncher();
        setupBroadcastReceivers();
        
        // Load saved phone numbers
        loadSavedPhoneNumbers();
        
        // Start service immediately
        bindToService();
        
        // Check permissions on startup (only once)
        checkAndRequestPermissions();
    }

    private void initializeViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvProgress = findViewById(R.id.tvProgress);
        btnImportList = findViewById(R.id.btnImportList);
        btnStartStop = findViewById(R.id.btnStartStop);
        recyclerView = findViewById(R.id.recyclerView);

        btnImportList.setOnClickListener(v -> openFilePicker());
        btnStartStop.setOnClickListener(v -> toggleDialing());
    }

    private void setupRecyclerView() {
        adapter = new PhoneNumberAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    importPhoneNumbers(uri);
                }
            }
        );
    }

    private void setupBroadcastReceivers() {
        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String status = intent.getStringExtra(AutoDialingService.EXTRA_STATUS);
                updateStatus(status);
            }
        };

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int currentIndex = intent.getIntExtra(AutoDialingService.EXTRA_CURRENT_INDEX, 0);
                updateProgress(currentIndex);
                
                // Update adapter with current progress
                if (serviceBound && dialingService != null) {
                    List<PhoneNumber> phoneNumbers = dialingService.getPhoneNumbers();
                    adapter.setPhoneNumbers(phoneNumbers);
                }
            }
        };
    }

    private void checkAndRequestPermissions() {
        // 권한을 이미 요청했는지 확인 (최초 한 번만)
        SharedPreferences prefs = getSharedPreferences("AutoCallerPrefs", MODE_PRIVATE);
        boolean permissionsRequested = prefs.getBoolean("permissions_requested", false);
        
        if (permissionsRequested && hasAllRequiredPermissions()) {
            // 이미 권한을 요청했고 모든 권한이 있으면 바로 진행
            checkAccessibilityService();
            return;
        }
        
        List<String> permissionsNeeded = new ArrayList<>();
        
        // 핵심 권한만 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CALL_PHONE);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ANSWER_PHONE_CALLS);
        }

        if (!permissionsNeeded.isEmpty()) {
            requestPermissions(permissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS);
        } else {
            // 권한 요청 완료 표시
            prefs.edit().putBoolean("permissions_requested", true).apply();
            checkAccessibilityService();
        }
    }

    private boolean hasAllRequiredPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_PERMISSIONS) {
            // 권한 요청을 한 번 했다고 표시 (결과와 상관없이)
            SharedPreferences prefs = getSharedPreferences("AutoCallerPrefs", MODE_PRIVATE);
            prefs.edit().putBoolean("permissions_requested", true).apply();
            
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                checkAccessibilityService();
            } else {
                showPermissionDialog();
            }
        }
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.phone_permission_required)
            .setPositiveButton(R.string.go_to_settings, (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            })
            .setNegativeButton(R.string.cancel, (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    private void checkAccessibilityService() {
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityServiceDialog();
        }
        // Always bind to service regardless of accessibility service status
        // bindToService(); // Already called in onCreate
    }

    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + 
            "com.autocaller.app.service.CallEndAccessibilityService";
        
        String enabledServices = Settings.Secure.getString(
            getContentResolver(), 
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        
        return enabledServices != null && enabledServices.contains(service);
    }

    private void showAccessibilityServiceDialog() {
        new AlertDialog.Builder(this)
            .setTitle("접근성 서비스 권장")
            .setMessage("더 나은 자동화를 위해 접근성 서비스를 활성화하는 것을 권장합니다. (선택사항)")
            .setPositiveButton(R.string.go_to_settings, (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            })
            .setNegativeButton("나중에", null)
            .show();
    }

    private void bindToService() {
        try {
            Intent intent = new Intent(this, AutoDialingService.class);
            startService(intent);
            boolean bound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "Service binding result: " + bound);
            
            if (!bound) {
                Log.e(TAG, "Failed to bind to service");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error binding to service", e);
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AutoDialingService.LocalBinder binder = (AutoDialingService.LocalBinder) service;
            dialingService = binder.getService();
            serviceBound = true;
            
            Log.d(TAG, "Service connected successfully");
            
            // Load saved phone numbers to service
            List<PhoneNumber> phoneNumbers = adapter.getPhoneNumbers();
            if (!phoneNumbers.isEmpty()) {
                dialingService.setPhoneNumbers(phoneNumbers);
            }
            
            runOnUiThread(() -> {
                tvStatus.setText("준비됨");
                updateUI();
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            dialingService = null;
            Log.d(TAG, "Service disconnected");
            runOnUiThread(() -> tvStatus.setText("서비스 연결 끊어짐"));
        }
    };

    private void openFilePicker() {
        // OpenDocument expects MIME types, not file extensions
        filePickerLauncher.launch(new String[]{"text/csv", "text/plain", "text/comma-separated-values", "*/*"});
    }

    private void importPhoneNumbers(Uri uri) {
        try {
            List<PhoneNumber> phoneNumbers = FileImportUtil.importFromFile(this, uri);
            
            if (phoneNumbers.isEmpty()) {
                Toast.makeText(this, R.string.no_numbers_found, Toast.LENGTH_SHORT).show();
                return;
            }
            
            adapter.setPhoneNumbers(phoneNumbers);
            
            // Save phone numbers to SharedPreferences
            savePhoneNumbers(phoneNumbers);
            
            if (serviceBound && dialingService != null) {
                dialingService.setPhoneNumbers(phoneNumbers);
            }
            
            updateProgress(0);
            
            String message = getString(R.string.file_import_success, phoneNumbers.size());
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            
            Log.d(TAG, "Imported and saved " + phoneNumbers.size() + " phone numbers");
            
        } catch (Exception e) {
            Log.e(TAG, "Error importing file", e);
            String message = getString(R.string.file_import_error, e.getMessage());
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void toggleDialing() {
        if (!serviceBound || dialingService == null) {
            // Try to reconnect to service
            Log.w(TAG, "Service not bound, attempting to reconnect...");
            bindToService();
            Toast.makeText(this, "서비스 연결 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (adapter.getPhoneNumbers().isEmpty()) {
            Toast.makeText(this, "먼저 연락처 목록을 가져오세요", Toast.LENGTH_SHORT).show();
            return;
        }

        AutoDialingService.ServiceStatus status = dialingService.getCurrentStatus();
        
        switch (status) {
            case IDLE:
            case COMPLETED:
                dialingService.startDialing();
                btnStartStop.setText(R.string.stop_calling);
                break;
            case RUNNING:
                dialingService.stopDialing();
                btnStartStop.setText(R.string.start_calling);
                break;
            case PAUSED:
                dialingService.resumeDialing();
                btnStartStop.setText(R.string.stop_calling);
                break;
        }
    }

    private void updateStatus(String status) {
        runOnUiThread(() -> tvStatus.setText(status));
    }

    private void updateProgress(int currentIndex) {
        runOnUiThread(() -> {
            int total = adapter.getItemCount();
            tvProgress.setText(currentIndex + " / " + total);
        });
    }

    private void updateUI() {
        if (serviceBound && dialingService != null) {
            AutoDialingService.ServiceStatus status = dialingService.getCurrentStatus();
            
            switch (status) {
                case IDLE:
                    tvStatus.setText("준비됨");
                    btnStartStop.setText(R.string.start_calling);
                    break;
                case RUNNING:
                    tvStatus.setText("다이얼링 중");
                    btnStartStop.setText(R.string.stop_calling);
                    break;
                case PAUSED:
                    tvStatus.setText("일시중지됨");
                    btnStartStop.setText(R.string.start_calling);
                    break;
                case COMPLETED:
                    tvStatus.setText("완료됨");
                    btnStartStop.setText(R.string.start_calling);
                    break;
            }
            
            updateProgress(dialingService.getCurrentIndex());
        }
    }

    private void loadSavedPhoneNumbers() {
        SharedPreferences prefs = getSharedPreferences("AutoCallerPrefs", MODE_PRIVATE);
        String savedNumbers = prefs.getString("phone_numbers", "");
        
        if (!savedNumbers.isEmpty()) {
            List<PhoneNumber> phoneNumbers = new ArrayList<>();
            String[] numbers = savedNumbers.split("\n");
            
            for (int i = 0; i < numbers.length; i++) {
                if (!numbers[i].trim().isEmpty()) {
                    phoneNumbers.add(new PhoneNumber(numbers[i].trim(), i));
                }
            }
            
            adapter.setPhoneNumbers(phoneNumbers);
            updateProgress(0);
            
            Log.d(TAG, "Loaded " + phoneNumbers.size() + " saved phone numbers");
        }
    }

    private void savePhoneNumbers(List<PhoneNumber> phoneNumbers) {
        SharedPreferences prefs = getSharedPreferences("AutoCallerPrefs", MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        
        for (PhoneNumber phoneNumber : phoneNumbers) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(phoneNumber.getNumber());
        }
        
        prefs.edit().putString("phone_numbers", sb.toString()).apply();
        Log.d(TAG, "Saved " + phoneNumbers.size() + " phone numbers");
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Register broadcast receivers
        LocalBroadcastManager.getInstance(this).registerReceiver(
            statusReceiver, new IntentFilter(AutoDialingService.ACTION_UPDATE_STATUS));
        LocalBroadcastManager.getInstance(this).registerReceiver(
            progressReceiver, new IntentFilter(AutoDialingService.ACTION_UPDATE_PROGRESS));
        
        // Check accessibility service again when resuming
        if (isAccessibilityServiceEnabled() && !serviceBound) {
            bindToService();
        }
        
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        // Unregister broadcast receivers
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}