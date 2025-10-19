package com.swiftest.core.protocol;

import android.util.Log;

import com.swiftest.core.interfaces.UdpTestCallback;
import com.swiftest.core.interfaces.WebSocketCallback;
import com.swiftest.core.models.SpeedTestConfig;

import org.json.JSONObject;

/**
 * 统一的测速协议实现
 * 整合WebSocket和UDP的交互逻辑，提供完整的测速流程控制
 * 支持App模式和SDK模式的不同协议
 */
public class SpeedTestProtocol {
    
    private static final String TAG = "SpeedTestProtocol";
    
    // 核心组件
    private final SpeedTestConfig config;
    private final ProtocolCallback protocolCallback;
    
    // WebSocket和UDP组件
    private WebSocketClient webSocketClient;
    private UdpTester udpTester;
    
    // 测速状态
    private volatile boolean isTestRunning = false;
    private volatile boolean isConnected = false;
    private int currentUdpPort = 0;
    
    /**
     * 协议回调接口，用于向上层通知测速进度和结果
     */
    public interface ProtocolCallback {
        /**
         * 测速开始
         */
        void onTestStarted();
        
        /**
         * 测速进度更新
         * @param progress 进度百分比 (0-100)
         * @param currentSpeed 当前速度
         */
        void onProgressUpdate(int progress, float currentSpeed);
        
        /**
         * 测速完成
         * @param downloadSpeed 最终下载速度
         * @param traffic 总流量
         */
        void onTestCompleted(float downloadSpeed, double traffic);
        
        /**
         * 测速失败
         * @param error 错误信息
         */
        void onTestFailed(String error);
        
        /**
         * 网络问题
         * @param issueType 1=发送超时, 2=接收超时, 3=连接失败
         */
        void onNetworkIssue(int issueType);
    }
    
    public SpeedTestProtocol(SpeedTestConfig config, ProtocolCallback callback) {
        this.config = config;
        this.protocolCallback = callback;
    }
    
    /**
     * 开始测速
     */
    public synchronized void startSpeedTest() {
        if (isTestRunning) {
            Log.w(TAG, "Speed test is already running");
            return;
        }
        
        Log.d(TAG, "Starting speed test");
        isTestRunning = true;
        protocolCallback.onTestStarted();
        
        // 创建WebSocket客户端
        createWebSocketClient();
        
        // 启动WebSocket连接
        webSocketClient.start();
    }
    
    /**
     * 停止测速
     */
    public synchronized void stopSpeedTest() {
        if (!isTestRunning) {
            return;
        }
        
        Log.d(TAG, "Stopping speed test");
        isTestRunning = false;
        isConnected = false;
        
        // 停止UDP测试
        if (udpTester != null) {
            udpTester.stopTest();
            try {
                udpTester.join(1000); // 等待最多1秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            udpTester = null;
        }
        
        // 停止WebSocket连接
        if (webSocketClient != null) {
            webSocketClient.stopConnection();
            try {
                webSocketClient.join(1000); // 等待最多1秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            webSocketClient = null;
        }
        
        Log.d(TAG, "Speed test stopped");
    }
    
    /**
     * 创建WebSocket客户端
     */
    private void createWebSocketClient() {
        WebSocketCallback wsCallback = new WebSocketCallback() {
            @Override
            public void onConnected() {
                Log.d(TAG, "WebSocket connected");
                isConnected = true;
            }
            
            @Override
            public void onConnectionFailed(String error) {
                Log.e(TAG, "WebSocket connection failed: " + error);
                protocolCallback.onNetworkIssue(3);
                protocolCallback.onTestFailed("Connection failed: " + error);
                isTestRunning = false;
            }
            
            @Override
            public void onMessageReceived(JSONObject message) {
                Log.d(TAG, "Received WebSocket message");
                // 这个回调可以用于调试或扩展功能
            }
            
            @Override
            public void onUdpPortReceived(int udpPort) {
                Log.d(TAG, "Received UDP port: " + udpPort);
                currentUdpPort = udpPort;
                
                // 创建并启动UDP测试器
                createUdpTester();
                udpTester.start();
            }
            
            @Override
            public void onSpeedTestStart(int speed) {
                Log.d(TAG, "Speed test start command received: " + speed);
                if (udpTester != null) {
                    udpTester.setSendSpeed(speed);
                    udpTester.setReceive(true);
                }
            }
            
            @Override
            public void onSpeedTestFinish(double traffic) {
                Log.d(TAG, "Speed test finished with traffic: " + traffic + "MB");
                
                // 获取最终速度
                float finalSpeed = udpTester != null ? udpTester.getDownloadSpeed() : 0;
                protocolCallback.onTestCompleted(finalSpeed, traffic);
                
                // 清理资源
                stopSpeedTest();
            }
            
            @Override
            public void onSpeedExceeded() {
                Log.d(TAG, "Speed exceeded server limit");
                // 服务器限制，使用固定值500Mbps
                protocolCallback.onTestCompleted(500.0f, 0);
                stopSpeedTest();
            }
            
            @Override
            public void onSendTimeout() {
                Log.w(TAG, "WebSocket send timeout");
                protocolCallback.onNetworkIssue(1);
            }
            
            @Override
            public void onReceiveTimeout() {
                Log.w(TAG, "WebSocket receive timeout");
                protocolCallback.onNetworkIssue(2);
            }
            
            @Override
            public void onDisconnected(int code, String reason) {
                Log.d(TAG, "WebSocket disconnected: " + code + " " + reason);
                if (isTestRunning) {
                    protocolCallback.onTestFailed("Connection lost: " + reason);
                    stopSpeedTest();
                }
            }
        };
        
        webSocketClient = new WebSocketClient(config, wsCallback);
    }
    
    /**
     * 创建UDP测试器
     */
    private void createUdpTester() {
        if (currentUdpPort == 0) {
            Log.e(TAG, "UDP port not available");
            return;
        }
        
        UdpTestCallback udpCallback = new UdpTestCallback() {
            @Override
            public void onTriggerTimeout() {
                Log.w(TAG, "UDP trigger timeout");
                protocolCallback.onNetworkIssue(2);
                protocolCallback.onTestFailed("UDP trigger timeout");
                stopSpeedTest();
            }
            
            @Override
            public void onTestStart(int loop, int speed) {
                Log.d(TAG, "UDP test start - Loop: " + loop + ", Speed: " + speed);
                // 计算进度 (每个循环约33%进度)
                int progress = Math.min(loop * 33, 90);
                protocolCallback.onProgressUpdate(progress, speed);
            }
            
            @Override
            public void onFirstPacketReceived() {
                Log.d(TAG, "First UDP packet received");
            }
            
            @Override
            public void onSingleTestComplete(float speed, boolean isSaturated, boolean isTestEnd) {
                Log.d(TAG, "Single test complete - Speed: " + speed + ", Saturated: " + isSaturated + ", End: " + isTestEnd);
                
                // 更新进度
                protocolCallback.onProgressUpdate(isTestEnd ? 95 : 70, speed);
                
                // 根据测试结果发送相应的控制消息
                if (webSocketClient != null) {
                    if (isTestEnd) {
                        // 测试完成，发送结束消息
                        webSocketClient.sendFinishMessage(speed);
                    } else if (isSaturated) {
                        // 饱和状态，重复测试
                        webSocketClient.sendSpeedControlMessage("repeat");
                    } else {
                        // 未饱和，继续加速
                        webSocketClient.sendSpeedControlMessage("continue");
                    }
                }
            }
            
            @Override
            public void onAllTestsComplete(float finalSpeed) {
                Log.d(TAG, "All UDP tests completed - Final speed: " + finalSpeed);
                protocolCallback.onProgressUpdate(100, finalSpeed);
            }
            
            @Override
            public void onUdpError(String error) {
                Log.e(TAG, "UDP error: " + error);
                protocolCallback.onTestFailed("UDP test error: " + error);
                stopSpeedTest();
            }
            
            @Override
            public void onNoDataReceived() {
                Log.w(TAG, "No UDP data received");
                protocolCallback.onNetworkIssue(1);
                protocolCallback.onTestFailed("No data received");
                stopSpeedTest();
            }
        };
        
        udpTester = new UdpTester(config.getServerHost(), currentUdpPort, udpCallback);
    }
    
    // 公共方法
    
    /**
     * 检查测速是否正在运行
     */
    public boolean isTestRunning() {
        return isTestRunning;
    }
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * 获取当前UDP端口
     */
    public int getCurrentUdpPort() {
        return currentUdpPort;
    }
    
    /**
     * 获取当前测速状态信息
     */
    public String getStatusInfo() {
        if (!isTestRunning) return "Stopped";
        if (!isConnected) return "Connecting";
        if (currentUdpPort == 0) return "Waiting for UDP port";
        if (udpTester == null) return "Initializing UDP";
        return "Testing (Loop " + udpTester.getRepeatCounter() + "/3)";
    }
}