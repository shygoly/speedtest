package com.swiftest.core.protocol;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.swiftest.core.interfaces.WebSocketCallback;
import com.swiftest.core.models.SpeedTestConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * 统一的WebSocket客户端实现
 * 抽取了原始WsService和WebSocketHandler的共同逻辑
 * 支持App模式和SDK模式的不同协议
 */
public class WebSocketClient extends Thread {
    
    private static final String TAG = "WebSocketClient";
    
    private final SpeedTestConfig config;
    private final WebSocketCallback callback;
    
    private volatile boolean isRunning = false;
    private OkHttpClient httpClient;
    private WebSocket webSocket;
    private ScheduledExecutorService executorService;
    
    public WebSocketClient(SpeedTestConfig config, WebSocketCallback callback) {
        this.config = config;
        this.callback = callback;
        this.executorService = Executors.newScheduledThreadPool(2);
    }
    
    @Override
    public void run() {
        isRunning = true;
        
        // 创建OkHttp客户端
        httpClient = new OkHttpClient.Builder()
                .readTimeout(config.getReceiveTimeoutSeconds(), TimeUnit.SECONDS)
                .build();
        
        // 构建WebSocket请求
        Request request = new Request.Builder()
                .url(config.getWebSocketUrl())
                .build();
        
        Log.d(TAG, "Connecting to " + config.getWebSocketUrl());
        
        // 创建WebSocket连接
        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            
            private ScheduledFuture<?> currentTimeoutTask;
            
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.d(TAG, "WebSocket connected");
                callback.onConnected();
                
                // 发送初始握手消息
                sendInitialMessage(webSocket);
            }
            
            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.d(TAG, "Received: " + text);
                
                // 取消接收超时任务
                if (currentTimeoutTask != null) {
                    currentTimeoutTask.cancel(false);
                }
                
                try {
                    JSONObject message = new JSONObject(text);
                    processMessage(webSocket, message);
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to parse message: " + text, e);
                }
            }
            
            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                Log.e(TAG, "WebSocket connection failed", t);
                callback.onConnectionFailed(t.getMessage());
            }
            
            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                Log.d(TAG, "WebSocket closed: " + code + " " + reason);
                callback.onDisconnected(code, reason);
            }
            
            
            /**
             * 发送初始握手消息
             */
            private void sendInitialMessage(WebSocket webSocket) {
                try {
                    JSONObject initMessage = new JSONObject();
                    
                    if (config.isAppMode()) {
                        // App模式：发送GUID和testID
                        initMessage.put("GUID", config.getGuid());
                        initMessage.put("testID", config.getTestId());
                    } else {
                        // SDK模式：发送hello消息
                        initMessage.put("msg", "hello");
                    }
                    
                    sendMessageWithTimeout(webSocket, initMessage);
                    
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to create initial message", e);
                    callback.onConnectionFailed("Failed to create initial message");
                }
            }
            
            /**
             * 处理收到的消息
             */
            private void processMessage(WebSocket webSocket, JSONObject message) {
                try {
                    // 通知回调接口
                    callback.onMessageReceived(message);
                    
                    // 处理UDP端口分配
                    if (message.has("udp_port")) {
                        int udpPort = message.getInt("udp_port");
                        Log.d(TAG, "Received UDP port: " + udpPort);
                        callback.onUdpPortReceived(udpPort);
                        return;
                    }
                    
                    // 处理消息类型
                    if (message.has("msg")) {
                        String msgType = message.getString("msg");
                        
                        switch (msgType) {
                            case "hello there":
                                // SDK模式的握手响应
                                Log.d(TAG, "Received hello response");
                                break;
                                
                            case "start":
                                // 测速开始
                                int speed = message.getInt("speed");
                                Log.d(TAG, "Speed test start with speed: " + speed);
                                callback.onSpeedTestStart(speed);
                                
                                // 发送开始确认
                                sendStartAck(webSocket);
                                break;
                                
                            case "exceed":
                                // 速度超限
                                Log.d(TAG, "Speed exceeded");
                                callback.onSpeedExceeded();
                                
                                // 发送结束消息
                                sendFinishMessage(webSocket, 500);
                                break;
                        }
                    }
                    
                    // 处理流量统计
                    if (message.has("traffic")) {
                        double traffic = message.getDouble("traffic") / 1024 / 1024; // 转换为MB
                        Log.d(TAG, "Test finished with traffic: " + traffic + "MB");
                        callback.onSpeedTestFinish(traffic);
                    }
                    
                } catch (JSONException e) {
                    Log.e(TAG, "Error processing message", e);
                }
            }
            
            /**
             * 发送开始确认消息
             */
            private void sendStartAck(WebSocket webSocket) {
                try {
                    JSONObject ackMessage = new JSONObject();
                    ackMessage.put("msg", "start");
                    sendMessageWithTimeout(webSocket, ackMessage);
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to send start ack", e);
                }
            }
            
            /**
             * 带超时机制的消息发送
             */
            private void sendMessageWithTimeout(WebSocket webSocket, JSONObject message) {
                // 设置发送超时
                ScheduledFuture<?> sendTimeoutTask = executorService.schedule(() -> {
                    Log.w(TAG, "Send timeout for message: " + message.toString());
                    callback.onSendTimeout();
                }, config.getSendTimeoutSeconds(), TimeUnit.SECONDS);
                
                boolean sent = webSocket.send(message.toString());
                if (sent) {
                    sendTimeoutTask.cancel(false);
                    Log.d(TAG, "Sent: " + message.toString());
                    
                    // 设置接收超时
                    currentTimeoutTask = executorService.schedule(() -> {
                        Log.w(TAG, "Receive timeout after sending: " + message.toString());
                        callback.onReceiveTimeout();
                    }, config.getReceiveTimeoutSeconds(), TimeUnit.SECONDS);
                } else {
                    sendTimeoutTask.cancel(false);
                    Log.e(TAG, "Failed to send message: " + message.toString());
                }
            }
        });
    }
    
    /**
     * 发送测速控制消息
     * @param command 命令类型："continue"(继续), "repeat"(重复), "finish"(结束)
     */
    public void sendSpeedControlMessage(String command) {
        if (webSocket == null) {
            Log.w(TAG, "WebSocket not connected");
            return;
        }
        
        try {
            JSONObject controlMessage = new JSONObject();
            
            // 根据不同模式使用不同的命令格式
            if (config.isAppMode()) {
                controlMessage.put("msg", command);
            } else {
                // SDK模式使用不同的命令名称
                String sdkCommand = command;
                if ("continue".equals(command)) {
                    sdkCommand = "increase";
                }
                controlMessage.put("msg", sdkCommand);
            }
            
            sendMessageWithTimeout(controlMessage);
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to send control message", e);
        }
    }
    
    /**
     * 发送测速结束消息
     * @param downloadSpeed 下载速度
     */
    public void sendFinishMessage(double downloadSpeed) {
        if (webSocket == null) {
            Log.w(TAG, "WebSocket not connected");
            return;
        }
        
        sendFinishMessage(webSocket, downloadSpeed);
    }
    
    private void sendFinishMessage(WebSocket webSocket, double downloadSpeed) {
        try {
            JSONObject finishMessage = new JSONObject();
            finishMessage.put("msg", "finish");
            finishMessage.put("download", downloadSpeed);
            
            sendMessageWithTimeout(webSocket, finishMessage);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to send finish message", e);
        }
    }
    
    private void sendMessageWithTimeout(JSONObject message) {
        if (webSocket != null) {
            // 设置发送超时
            ScheduledFuture<?> sendTimeoutTask = executorService.schedule(() -> {
                Log.w(TAG, "Send timeout for message: " + message.toString());
                callback.onSendTimeout();
            }, config.getSendTimeoutSeconds(), TimeUnit.SECONDS);
            
            boolean sent = webSocket.send(message.toString());
            if (sent) {
                sendTimeoutTask.cancel(false);
                Log.d(TAG, "Sent: " + message.toString());
            } else {
                sendTimeoutTask.cancel(false);
                Log.e(TAG, "Failed to send message: " + message.toString());
            }
        }
    }
    
    /**
     * 停止WebSocket连接
     */
    public void stopConnection() {
        Log.d(TAG, "Stopping WebSocket connection");
        isRunning = false;
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (webSocket != null) {
            webSocket.close(1000, "Client closing connection");
            webSocket = null;
        }
    }
    
    public boolean isRunning() {
        return isRunning;
    }
}
