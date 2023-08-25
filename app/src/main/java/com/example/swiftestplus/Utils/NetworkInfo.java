package com.example.swiftestplus.Utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.swiftestplus.MainActivity;

public class NetworkInfo {
    private MainActivity activity;
    public static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 1;
    public static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    boolean connected;
    String network_type;    // WiFi xG Unknown
    String cellular_carrier;
    String wifi_name;
    Context context;

    public void getNetworkInfo() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
            return;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        connected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (connected) {
            switch (activeNetwork.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    network_type = "WiFi";
                    Log.d("network info", "WiFi");
                    WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    wifi_name = wifiInfo.getSSID();
                    Log.d("network info", wifi_name);
                    // wifiInfo.getSSID() 可以获取到 WiFi SSID
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    cellular_carrier = telephonyManager.getNetworkOperatorName();
                    int networkType = telephonyManager.getNetworkType();
                    Log.d("network info", "Cellular code: " + String.valueOf(networkType));
                    switch (networkType) {
                        case TelephonyManager.NETWORK_TYPE_GPRS:
                        case TelephonyManager.NETWORK_TYPE_EDGE:
                        case TelephonyManager.NETWORK_TYPE_CDMA:
                        case TelephonyManager.NETWORK_TYPE_1xRTT:
                        case TelephonyManager.NETWORK_TYPE_IDEN: // Deprecated in API 28
                            network_type = "2G";
                            break;
                        case TelephonyManager.NETWORK_TYPE_UMTS:
                        case TelephonyManager.NETWORK_TYPE_EVDO_0:
                        case TelephonyManager.NETWORK_TYPE_EVDO_A:
                        case TelephonyManager.NETWORK_TYPE_HSDPA:
                        case TelephonyManager.NETWORK_TYPE_HSUPA:
                        case TelephonyManager.NETWORK_TYPE_HSPA:
                        case TelephonyManager.NETWORK_TYPE_EVDO_B: // API 9
                        case TelephonyManager.NETWORK_TYPE_EHRPD:  // API 11
                        case TelephonyManager.NETWORK_TYPE_HSPAP:  // API 13
                            network_type = "3G";
                            break;
                        case TelephonyManager.NETWORK_TYPE_LTE:    // API 11
                        case TelephonyManager.NETWORK_TYPE_IWLAN:  // API 25
                            network_type = "4G";
                            break;
                        case TelephonyManager.NETWORK_TYPE_NR: // API 29
                            network_type = "5G";
                            break;
                        default:
                            network_type = "Unknown";
                    }
                    Log.d("network info", network_type);
                    break;
                default:
                    network_type = "Unknown";
            }
        }
    }

    public NetworkInfo(Context context, MainActivity activity) {
        this.context = context;
        this.activity = activity;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getNetwork_type() {
        return network_type;
    }

    public String getCellular_carrier() {
        return cellular_carrier;
    }

    public String getWifi_name() {
        return wifi_name;
    }
}
