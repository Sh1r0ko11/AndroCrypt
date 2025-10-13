package com.example.androcryptor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.util.Scanner;

/**
 * Activity displayed after successful file decryption.
 * Shows decryption statistics and manages application cleanup.
 */
public class DecryptionSuccessActivity extends AppCompatActivity {
    //#region Constants and Fields
    private static final String TAG = "DecryptionSuccess";
    private TextView decryptionStats;
    private TextView successMessage;
    //#endregion

    //#region Lifecycle Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decryption_success);

        // Stop the encryption service
        stopService(new Intent(this, EncryptionService.class));
        getSharedPreferences("ac_prefs", MODE_PRIVATE).edit().putBoolean("decryption_complete", true).apply();


        // Initialize UI elements
        decryptionStats = findViewById(R.id.decryptionStats);
        successMessage = findViewById(R.id.successMessage);

        // Get the number of decrypted files from the intent
        int totalDecrypted = getIntent().getIntExtra("totalDecrypted", 0);
        int totalFiles = getIntent().getIntExtra("totalFiles", 0);

        // Update the stats text
        decryptionStats.setText("Total files decrypted: " + totalDecrypted + " of " + totalFiles);

        // Try to load additional information from the XML file
        loadDecryptionResults();

        // Automatically close the app after 10 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            finishAffinity(); // Close all activities and exit the app
        }, 10000);
    }
    //#endregion

    //#region Results Processing
    /**
     * Load decryption results from XML file if available
     */
    private void loadDecryptionResults() {
        try {
            File resultsFile = new File(getFilesDir(), "decryption_results.xml");
            if (resultsFile.exists()) {
                // Read the file content
                FileInputStream fis = new FileInputStream(resultsFile);
                Scanner scanner = new Scanner(fis);
                StringBuilder content = new StringBuilder();
                while (scanner.hasNextLine()) {
                    content.append(scanner.nextLine()).append("\n");
                }
                scanner.close();
                fis.close();

                // Log the content for debugging
                Log.d(TAG, "Loaded decryption results: " + content.toString());

                // Update the success message with additional information
                String successRate = extractXmlValue(content.toString(), "success_rate");
                if (successRate != null && !successRate.isEmpty()) {
                    successMessage.setText("Your files have been successfully decrypted with a " +
                            successRate + " success rate. Your device is now secure.");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading decryption results", e);
        }
    }

    /**
     * Extract a value from XML content
     */
    private String extractXmlValue(String xml, String tag) {
        try {
            String startTag = "<" + tag + ">";
            String endTag = "</" + tag + ">";
            int startIndex = xml.indexOf(startTag) + startTag.length();
            int endIndex = xml.indexOf(endTag);
            if (startIndex >= 0 && endIndex > startIndex) {
                return xml.substring(startIndex, endIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting XML value", e);
        }
        return null;
    }
    //#endregion

    //#region Input Handling
    @Override
    public void onBackPressed() {
        // Disable the back button
        Toast.makeText(this, "This screen will close automatically.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Disable the back button
            Toast.makeText(this, "This screen will close automatically.", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    //#endregion
}