package com.swiftest.core.protocol;

import android.util.Log;

import com.swiftest.core.interfaces.UdpTestCallback;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 统一的UDP测试器实现
 * 抽取了原始UdpService和UdpHandler的共同逻辑
 * 支持自动速度测试和饱和度检测
 */
public class UdpTester extends Thread {
    
    private static final String TAG = "UdpTester";
    
    // 测试参数常量
    private static final int MAX_LOOP = 3;
    private static final float SATURATED_THRESHOLD = 1.2f;
    private static final int MAX_TRIGGER_COUNT = 10;
    private static final int SOCKET_TIMEOUT_MS = 100;
    private static final int TRIGGER_INTERVAL_MS = 50;
    private static final int WAIT_INTERVAL_MS = 10;
    private static final int SAMPLE_INTERVAL_MS = 10;
    private static final int MAX_SAMPLES = 100;
    private static final int PACKET_SIZE = 1024;
    
    // 回调接口
    private final UdpTestCallback callback;
    
    // 网络参数
    private final String serverHost;
    private final int udpPort;
    
    // 运行状态
    private volatile boolean isRunning = false;
    private volatile boolean trigger = true;
    private volatile boolean receive = false;
    
    // UDP相关
    private DatagramSocket datagramSocket;
    private InetAddress serverAddress;
    
    // 触发器相关
    private int triggerCount = 0;
    
    // 测试相关
    private int sendSpeed = 0;
    private int repeatCounter = 0;
    
    // 数据统计
    private long rcvBytesCount = 0;
    private ArrayList<Long> rcvBytesRecord;
    private ArrayList<Float> rcvSpeedSamples;
    private ArrayList<Float> rcvSpeeds;
    private Timer sampleTimer;
    private float rcvSpeed = 0;
    private boolean getSingle = false;
    private boolean saturated = false;
    private float downloadSpeed = 0;
    
    public UdpTester(String serverHost, int udpPort, UdpTestCallback callback) {
        this.serverHost = serverHost;
        this.udpPort = udpPort;
        this.callback = callback;
        
        // 初始化数据结构
        this.rcvBytesRecord = new ArrayList<>();
        this.rcvSpeedSamples = new ArrayList<>();
        this.rcvSpeeds = new ArrayList<>();
    }
    
    @Override
    public void run() {
        isRunning = true;
        
        try {
            // 初始化UDP Socket
            initializeSocket();
            
            // 发送触发包阶段
            sendTriggerPackets();
            
            // 测试循环阶段
            performSpeedTests();
            
        } catch (Exception e) {
            Log.e(TAG, "UDP test failed", e);
            callback.onUdpError(e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    /**
     * 初始化UDP Socket
     */
    private void initializeSocket() throws SocketException, UnknownHostException {
        datagramSocket = new DatagramSocket();
        datagramSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
        serverAddress = InetAddress.getByName(serverHost);
        Log.d(TAG, "UDP socket initialized for " + serverHost + ":" + udpPort);
    }
    
    /**
     * 发送触发包
     */
    private void sendTriggerPackets() throws IOException, InterruptedException {
        Log.d(TAG, "Starting trigger phase");
        
        while (trigger && isRunning) {
            triggerCount++;
            
            // 检查触发超时
            if (triggerCount > MAX_TRIGGER_COUNT) {
                Log.w(TAG, "Trigger timeout exceeded");
                callback.onTriggerTimeout();
                return;
            }
            
            // 发送触发包
            byte[] triggerBuf = "trigger".getBytes();
            DatagramPacket triggerPacket = new DatagramPacket(
                    triggerBuf, triggerBuf.length, serverAddress, udpPort);
            datagramSocket.send(triggerPacket);
            
            Log.d(TAG, "Sent trigger packet " + triggerCount);
            Thread.sleep(TRIGGER_INTERVAL_MS);
        }
        
        Log.d(TAG, "Trigger phase completed");
    }
    
    /**
     * 执行速度测试循环
     */
    private void performSpeedTests() throws IOException, InterruptedException {
        while (repeatCounter < MAX_LOOP && isRunning) {
            // 等待开始接收信号
            waitForReceiveSignal();
            
            if (!isRunning) break;
            
            // 执行单次测试
            performSingleTest();
        }
        
        // 计算最终结果
        calculateFinalResult();
    }
    
    /**
     * 等待接收信号
     */
    private void waitForReceiveSignal() throws InterruptedException {
        while (!receive && isRunning) {
            Thread.sleep(WAIT_INTERVAL_MS);
        }
    }
    
    /**
     * 执行单次速度测试
     */
    private void performSingleTest() throws IOException, InterruptedException {
        callback.onTestStart(repeatCounter, sendSpeed);
        Log.d(TAG, "Test start, Loop: " + repeatCounter + ", Speed: " + sendSpeed);
        
        // 接收缓冲区
        byte[] rcvBuf = new byte[PACKET_SIZE];
        DatagramPacket rcvPacket = new DatagramPacket(rcvBuf, rcvBuf.length);
        
        repeatCounter++;
        
        try {
            // 接收第一个包
            datagramSocket.receive(rcvPacket);
            callback.onFirstPacketReceived();
            Log.d(TAG, "Received first packet");
            
            rcvBytesCount += rcvPacket.getLength();
            
            // 启动采样定时器
            startSampling();
            
            // 继续接收包直到超时
            receivePacketsUntilTimeout(rcvPacket);
            
        } catch (SocketTimeoutException e) {
            // 正常的超时结束
            processSingleTestResult();
        }
        
        // 重置单次测试数据
        resetSingleTestData();
    }
    
    /**
     * 启动采样定时器
     */
    private void startSampling() {
        sampleTimer = new Timer();
        sampleTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (rcvBytesRecord) {
                    rcvBytesRecord.add(rcvBytesCount);
                }
            }
        }, 0, SAMPLE_INTERVAL_MS);
    }
    
    /**
     * 持续接收包直到超时
     */
    private void receivePacketsUntilTimeout(DatagramPacket rcvPacket) throws IOException {
        while (true) {
            datagramSocket.receive(rcvPacket);
            rcvBytesCount += rcvPacket.getLength();
            
            // 检查是否超过最大采样时间
            synchronized (rcvBytesRecord) {
                if (rcvBytesRecord.size() >= MAX_SAMPLES) {
                    Log.d(TAG, "Receive timeout - max samples reached");
                    throw new SocketTimeoutException("Test exceed 1 second!");
                }
            }
        }
    }
    
    /**
     * 处理单次测试结果
     */
    private void processSingleTestResult() {
        if (rcvBytesCount == 0) {
            Log.w(TAG, "No data received");
            callback.onNoDataReceived();
            return;
        }
        
        // 停止采样定时器
        if (sampleTimer != null) {
            sampleTimer.cancel();
        }
        
        // 计算速度
        calculateSpeed();
        
        // 判断饱和状态
        checkSaturation();
        
        // 通知单次测试完成
        boolean isTestEnd = isTestEnd();
        callback.onSingleTestComplete(rcvSpeed, saturated, isTestEnd);
        
        getSingle = true;
    }
    
    /**
     * 计算接收速度
     */
    private void calculateSpeed() {
        synchronized (rcvBytesRecord) {
            Log.d(TAG, "Receive Bytes Record size: " + rcvBytesRecord.size());
            
            // 移除最后9个不稳定的采样点
            for (int i = 0; i < 9 && rcvBytesRecord.size() > 0; i++) {
                rcvBytesRecord.remove(rcvBytesRecord.size() - 1);
            }
            
            // 计算速度样本
            rcvSpeedSamples.clear();
            for (int i = 0; i < rcvBytesRecord.size(); i++) {
                float speedSample;
                if (i < 5) {
                    // 前5个点使用累积计算
                    speedSample = rcvBytesRecord.get(i) * 8.0f / 1024 / 1024 / ((i + 1) * 0.01f);
                } else {
                    // 后续点使用滑动窗口计算
                    speedSample = (rcvBytesRecord.get(i) - rcvBytesRecord.get(i - 5)) * 8.0f / 1024 / 1024 / 0.05f;
                }
                rcvSpeedSamples.add(speedSample);
            }
            
            // 计算95百分位数
            if (!rcvSpeedSamples.isEmpty()) {
                Collections.sort(rcvSpeedSamples);
                int percentileIndex = Math.min(95 * rcvSpeedSamples.size() / 100, rcvSpeedSamples.size() - 1);
                rcvSpeed = rcvSpeedSamples.get(percentileIndex);
                Log.d(TAG, "Download speed (95th percentile): " + rcvSpeed + " Mbps");
            }
        }
    }
    
    /**
     * 检查饱和状态
     */
    private void checkSaturation() {
        if (rcvSpeed * SATURATED_THRESHOLD >= sendSpeed) {
            // 不饱和，重置计数器
            saturated = false;
            repeatCounter = 0;
            rcvSpeeds.clear();
            Log.d(TAG, "Not saturated, resetting counter");
        } else {
            // 饱和，记录速度
            saturated = true;
            rcvSpeeds.add(rcvSpeed);
            Log.d(TAG, "Saturated, recorded speed: " + rcvSpeed);
        }
    }
    
    /**
     * 重置单次测试数据
     */
    private void resetSingleTestData() {
        rcvBytesCount = 0;
        rcvBytesRecord.clear();
        rcvSpeedSamples.clear();
        receive = false;
        getSingle = false;
    }
    
    /**
     * 计算最终结果
     */
    private void calculateFinalResult() {
        if (!rcvSpeeds.isEmpty()) {
            float sum = 0;
            for (float speed : rcvSpeeds) {
                sum += speed;
            }
            downloadSpeed = sum / rcvSpeeds.size();
            Log.d(TAG, "Final download speed: " + downloadSpeed + " Mbps");
        }
        
        callback.onAllTestsComplete(downloadSpeed);
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        isRunning = false;
        
        if (sampleTimer != null) {
            sampleTimer.cancel();
            sampleTimer = null;
        }
        
        if (datagramSocket != null && !datagramSocket.isClosed()) {
            datagramSocket.close();
            datagramSocket = null;
        }
        
        Log.d(TAG, "UDP tester cleaned up");
    }
    
    // 公共方法
    
    /**
     * 停止UDP测试
     */
    public void stopTest() {
        Log.d(TAG, "Stopping UDP test");
        isRunning = false;
        trigger = false;
        receive = false;
        cleanup();
    }
    
    /**
     * 设置触发状态
     */
    public void setTrigger(boolean trigger) {
        this.trigger = trigger;
        Log.d(TAG, "Trigger set to: " + trigger);
    }
    
    /**
     * 设置发送速度
     */
    public void setSendSpeed(int sendSpeed) {
        this.sendSpeed = sendSpeed;
        Log.d(TAG, "Send speed set to: " + sendSpeed);
    }
    
    /**
     * 设置接收状态
     */
    public void setReceive(boolean receive) {
        this.receive = receive;
        Log.d(TAG, "Receive set to: " + receive);
    }
    
    // Getter方法
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public int getRepeatCounter() {
        return repeatCounter;
    }
    
    public float getRcvSpeed() {
        return rcvSpeed;
    }
    
    public boolean isSaturated() {
        return saturated;
    }
    
    public boolean isTestEnd() {
        return getSingle && (repeatCounter >= MAX_LOOP);
    }
    
    public float getDownloadSpeed() {
        return downloadSpeed;
    }
    
    public boolean getSingle() {
        return getSingle;
    }
}