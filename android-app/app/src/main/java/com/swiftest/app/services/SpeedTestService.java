package com.swiftest.app.services;

import android.util.Log;

import com.swiftest.core.interfaces.ProgressListener;
import com.swiftest.core.interfaces.SpeedTestCallback;
import com.swiftest.core.models.SpeedTestConfig;
import com.swiftest.core.models.TestResult;
import com.swiftest.core.protocol.SpeedTestProtocol;

import java.util.Date;

/**
 * 测速业务逻辑服务
 * 负责协调测速流程，连接UI和Core模块
 */
public class SpeedTestService implements SpeedTestProtocol.ProtocolCallback {
    
    private static final String TAG = "SpeedTestService";
    private static final String DEFAULT_SERVER = "swiftest.thucloud.com";
    private static final int DEFAULT_PORT = 8080;
    
    private SpeedTestProtocol protocol;
    private SpeedTestCallback testCallback;
    private ProgressListener progressListener;
    private TestResult currentResult;
    private Date testStartTime;
    
    /**
     * 开始测速
     */
    public void startSpeedTest(SpeedTestCallback callback, ProgressListener progressListener) {
        this.testCallback = callback;
        this.progressListener = progressListener;
        
        Log.d(TAG, "Starting speed test service");
        
        // 创建配置
        SpeedTestConfig config = new SpeedTestConfig.Builder()
                .serverHost(DEFAULT_SERVER)
                .webSocketPort(DEFAULT_PORT)
                .guid(generateGUID())
                .testId(generateTestId())
                .appMode(true)
                .build();
        
        // 创建协议处理器
        protocol = new SpeedTestProtocol(config, this);
        testStartTime = new Date();
        
        // 启动测速
        protocol.startSpeedTest();
    }
    
    /**
     * 停止测速
     */
    public void stopSpeedTest() {
        Log.d(TAG, "Stopping speed test service");
        
        if (protocol != null) {
            protocol.stopSpeedTest();
        }
        
        if (testCallback != null) {
            testCallback.onTestCancelled();
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        stopSpeedTest();
        testCallback = null;
        progressListener = null;
        protocol = null;
    }
    
    // SpeedTestProtocol.ProtocolCallback 实现
    
    @Override
    public void onTestStarted() {
        Log.d(TAG, "Protocol test started");
        if (testCallback != null) {
            testCallback.onTestStarted();
        }
        if (progressListener != null) {
            progressListener.onStageChanged(ProgressListener.TestStage.CONNECTING, "正在连接服务器");
        }
    }
    
    @Override
    public void onProgressUpdate(int progress, float currentSpeed) {
        Log.d(TAG, "Progress: " + progress + "%, Speed: " + currentSpeed + " Mbps");
        
        if (progressListener != null) {
            String stage = getStageDescription(progress);
            progressListener.onProgressUpdate(progress, stage);
            progressListener.onSpeedUpdate(currentSpeed, currentSpeed);
        }
    }
    
    @Override
    public void onTestCompleted(float downloadSpeed, double traffic) {
        Log.d(TAG, "Test completed - Speed: " + downloadSpeed + " Mbps, Traffic: " + traffic + " MB");
        
        // 构建测试结果
        currentResult = new TestResult.Builder()
                .testId(generateTestId())
                .startTime(testStartTime)
                .endTime(new Date())
                .downloadSpeed(downloadSpeed)
                .totalTraffic(traffic)
                .successful(true)
                .build();
        
        if (testCallback != null) {
            testCallback.onTestCompleted(downloadSpeed, 0, 0, traffic);
        }
        
        if (progressListener != null) {
            progressListener.onProgressUpdate(100, "测试完成");
            progressListener.onStageChanged(ProgressListener.TestStage.COMPLETED, "测试完成");
        }
    }
    
    @Override
    public void onTestFailed(String error) {
        Log.e(TAG, "Test failed: " + error);
        
        if (testCallback != null) {
            testCallback.onTestFailed(SpeedTestCallback.ErrorCodes.UNKNOWN_ERROR, error);
        }
    }
    
    @Override
    public void onNetworkIssue(int issueType) {
        Log.w(TAG, "Network issue: " + issueType);
        
        String errorMessage;
        int errorCode;
        
        switch (issueType) {
            case 1:
                errorMessage = "发送数据超时";
                errorCode = SpeedTestCallback.ErrorCodes.CONNECTION_TIMEOUT;
                break;
            case 2:
                errorMessage = "接收数据超时";
                errorCode = SpeedTestCallback.ErrorCodes.CONNECTION_TIMEOUT;
                break;
            case 3:
                errorMessage = "连接失败";
                errorCode = SpeedTestCallback.ErrorCodes.NETWORK_ERROR;
                break;
            default:
                errorMessage = "网络异常";
                errorCode = SpeedTestCallback.ErrorCodes.NETWORK_ERROR;
        }
        
        if (testCallback != null) {
            testCallback.onTestFailed(errorCode, errorMessage);
        }
    }
    
    // 辅助方法
    
    private String getStageDescription(int progress) {
        if (progress < 10) return "准备测速";
        if (progress < 30) return "建立连接";
        if (progress < 95) return "测速中";
        if (progress < 100) return "分析结果";
        return "完成";
    }
    
    private String generateGUID() {
        return "app_" + System.currentTimeMillis();
    }
    
    private String generateTestId() {
        return "test_" + System.currentTimeMillis();
    }
    
    public TestResult getCurrentResult() {
        return currentResult;
    }
    
    public boolean isTestRunning() {
        return protocol != null && protocol.isTestRunning();
    }
}