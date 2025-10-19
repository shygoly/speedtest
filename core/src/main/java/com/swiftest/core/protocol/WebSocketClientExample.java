package com.swiftest.core.protocol;

import android.util.Log;

import com.swiftest.core.interfaces.WebSocketCallback;
import com.swiftest.core.models.SpeedTestConfig;

import org.json.JSONObject;

/**
 * WebSocketClient使用示例
 * 展示如何使用新的统一WebSocket客户端
 */
public class WebSocketClientExample {
    
    private static final String TAG = "WebSocketExample";
    
    /**
     * 创建App模式的测速客户端
     */
    public static WebSocketClient createAppModeClient(String serverHost, String guid, String testId) {
        SpeedTestConfig config = new SpeedTestConfig.Builder()
                .serverHost(serverHost)
                .guid(guid)
                .testId(testId)
                .appMode(true) // App模式
                .build();
        
        WebSocketCallback callback = new WebSocketCallback() {
            @Override
            public void onConnected() {
                Log.d(TAG, "App mode: WebSocket connected");
            }
            
            @Override
            public void onConnectionFailed(String error) {
                Log.e(TAG, "App mode: Connection failed - " + error);
            }
            
            @Override
            public void onMessageReceived(JSONObject message) {
                Log.d(TAG, "App mode: Received message - " + message.toString());
            }
            
            @Override
            public void onUdpPortReceived(int udpPort) {
                Log.d(TAG, "App mode: UDP port received - " + udpPort);
                // 在这里启动UDP测速逻辑
            }
            
            @Override
            public void onSpeedTestStart(int speed) {
                Log.d(TAG, "App mode: Speed test start with speed - " + speed);
                // 开始UDP测速
            }
            
            @Override
            public void onSpeedTestFinish(double traffic) {
                Log.d(TAG, "App mode: Speed test finished - traffic: " + traffic + "MB");
                // 更新UI显示测试结果
            }
            
            @Override
            public void onSpeedExceeded() {
                Log.d(TAG, "App mode: Speed exceeded limit");
                // 处理速度超限情况
            }
            
            @Override
            public void onSendTimeout() {
                Log.w(TAG, "App mode: Send timeout");
                // 处理发送超时
            }
            
            @Override
            public void onReceiveTimeout() {
                Log.w(TAG, "App mode: Receive timeout");
                // 处理接收超时
            }
            
            @Override
            public void onDisconnected(int code, String reason) {
                Log.d(TAG, "App mode: Disconnected - " + code + " " + reason);
                // 清理资源
            }
        };
        
        return new WebSocketClient(config, callback);
    }
    
    /**
     * 创建SDK模式的测速客户端
     */
    public static WebSocketClient createSDKModeClient(String serverHost, int wsPort, int udpPort) {
        SpeedTestConfig config = new SpeedTestConfig.Builder()
                .serverHost(serverHost)
                .webSocketPort(wsPort)
                .udpPort(udpPort)
                .appMode(false) // SDK模式
                .build();
        
        WebSocketCallback callback = new WebSocketCallback() {
            @Override
            public void onConnected() {
                Log.d(TAG, "SDK mode: WebSocket connected");
            }
            
            @Override
            public void onConnectionFailed(String error) {
                Log.e(TAG, "SDK mode: Connection failed - " + error);
            }
            
            @Override
            public void onMessageReceived(JSONObject message) {
                Log.d(TAG, "SDK mode: Received message - " + message.toString());
            }
            
            @Override
            public void onUdpPortReceived(int udpPort) {
                Log.d(TAG, "SDK mode: UDP port received - " + udpPort);
                // 在这里启动UDP测速逻辑
            }
            
            @Override
            public void onSpeedTestStart(int speed) {
                Log.d(TAG, "SDK mode: Speed test start with speed - " + speed);
                // 开始UDP测速
            }
            
            @Override
            public void onSpeedTestFinish(double traffic) {
                Log.d(TAG, "SDK mode: Speed test finished - traffic: " + traffic + "MB");
                // 通知SDK用户测试完成
            }
            
            @Override
            public void onSpeedExceeded() {
                Log.d(TAG, "SDK mode: Speed exceeded limit");
                // 处理速度超限情况
            }
            
            @Override
            public void onSendTimeout() {
                Log.w(TAG, "SDK mode: Send timeout");
                // 处理发送超时
            }
            
            @Override
            public void onReceiveTimeout() {
                Log.w(TAG, "SDK mode: Receive timeout");
                // 处理接收超时
            }
            
            @Override
            public void onDisconnected(int code, String reason) {
                Log.d(TAG, "SDK mode: Disconnected - " + code + " " + reason);
                // 清理资源
            }
        };
        
        return new WebSocketClient(config, callback);
    }
}