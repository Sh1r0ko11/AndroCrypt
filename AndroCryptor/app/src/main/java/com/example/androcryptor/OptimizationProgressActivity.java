package com.example.androcryptor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class OptimizationProgressActivity extends AppCompatActivity {

    public static final String ACTION_UPDATE_PROGRESS = "com.example.androcryptor.UPDATE_PROGRESS";
    public static final String ACTION_CLOSE = "com.example.androcryptor.CLOSE_PROGRESS";

    private ProgressBar progressBar;
    private TextView statusText;
    private TextView percentText;
    private BroadcastReceiver progressReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_optimization_progress);
        
        // Keep screen on during optimization
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Initialize UI elements
        progressBar = findViewById(R.id.optimizationProgressBar);
        statusText = findViewById(R.id.optimizationStatusText);
        percentText = findViewById(R.id.optimizationPercentText);
        
        // Get initial values
        int current = getIntent().getIntExtra("current", 0);
        int total = getIntent().getIntExtra("total", 100);
        
        // Update UI with initial values
        updateProgress(current, total);
        
        // Register broadcast receiver for progress updates
        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_UPDATE_PROGRESS.equals(intent.getAction())) {
                    int current = intent.getIntExtra("current", 0);
                    int total = intent.getIntExtra("total", 100);
                    updateProgress(current, total);
                } else if (ACTION_CLOSE.equals(intent.getAction())) {
                    finish();
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_PROGRESS);
        filter.addAction(ACTION_CLOSE);
        // Specify RECEIVER_NOT_EXPORTED for apps targeting Android 12+
        registerReceiver(progressReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }
    
    private void updateProgress(int current, int total) {
        if (total <= 0) total = 1; // Prevent division by zero
        
        int percent = (int) (((float) current / total) * 100);
        
        // Update progress bar
        progressBar.setMax(total);
        progressBar.setProgress(current);
        
        // Update text
        statusText.setText("Scanning and optimizing files: " + current + " of " + total);
        percentText.setText(percent + "%");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressReceiver != null) {
            try {
                unregisterReceiver(progressReceiver);
            } catch (Exception e) {
                // Receiver might not be registered
            }
        }
    }
    
    // Prevent user from closing this activity with back button
    @Override
    public void onBackPressed() {
        // Do nothing
    }
}