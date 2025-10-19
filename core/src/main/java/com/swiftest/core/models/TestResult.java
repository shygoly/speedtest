package com.swiftest.core.models;

import java.util.Date;

/**
 * 测速结果数据模型
 * 包含完整的测速结果信息
 */
public class TestResult {
    
    // 基础信息
    private final String testId;
    private final Date startTime;
    private final Date endTime;
    private final long duration; // 测试持续时间（毫秒）
    
    // 速度信息
    private final float downloadSpeed; // Mbps
    private final float uploadSpeed;   // Mbps (预留)
    private final float ping;          // ms (预留)
    
    // 流量信息
    private final double totalTraffic; // MB
    private final long totalBytes;     // bytes
    
    // 服务器信息
    private final String serverHost;
    private final int serverPort;
    
    // 网络质量
    private final String networkQuality;
    private final int qualityScore; // 0-100
    
    // 测试详情
    private final int testLoops;      // 测试循环次数
    private final boolean isSuccessful;
    private final String errorMessage;
    
    private TestResult(Builder builder) {
        this.testId = builder.testId;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.duration = builder.endTime.getTime() - builder.startTime.getTime();
        
        this.downloadSpeed = builder.downloadSpeed;
        this.uploadSpeed = builder.uploadSpeed;
        this.ping = builder.ping;
        
        this.totalTraffic = builder.totalTraffic;
        this.totalBytes = builder.totalBytes;
        
        this.serverHost = builder.serverHost;
        this.serverPort = builder.serverPort;
        
        this.networkQuality = builder.networkQuality;
        this.qualityScore = builder.qualityScore;
        
        this.testLoops = builder.testLoops;
        this.isSuccessful = builder.isSuccessful;
        this.errorMessage = builder.errorMessage;
    }
    
    // Getters
    public String getTestId() { return testId; }
    public Date getStartTime() { return startTime; }
    public Date getEndTime() { return endTime; }
    public long getDuration() { return duration; }
    
    public float getDownloadSpeed() { return downloadSpeed; }
    public float getUploadSpeed() { return uploadSpeed; }
    public float getPing() { return ping; }
    
    public double getTotalTraffic() { return totalTraffic; }
    public long getTotalBytes() { return totalBytes; }
    
    public String getServerHost() { return serverHost; }
    public int getServerPort() { return serverPort; }
    
    public String getNetworkQuality() { return networkQuality; }
    public int getQualityScore() { return qualityScore; }
    
    public int getTestLoops() { return testLoops; }
    public boolean isSuccessful() { return isSuccessful; }
    public String getErrorMessage() { return errorMessage; }
    
    /**
     * 获取格式化的速度描述
     */
    public String getFormattedSpeed() {
        if (downloadSpeed >= 1000) {
            return String.format("%.1f Gbps", downloadSpeed / 1000);
        } else {
            return String.format("%.1f Mbps", downloadSpeed);
        }
    }
    
    /**
     * 获取格式化的持续时间
     */
    public String getFormattedDuration() {
        long seconds = duration / 1000;
        if (seconds < 60) {
            return seconds + "秒";
        } else {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return minutes + "分" + remainingSeconds + "秒";
        }
    }
    
    /**
     * 转换为JSON字符串（简化版）
     */
    public String toJsonString() {
        return String.format(
                "{\"testId\":\"%s\",\"downloadSpeed\":%.2f,\"uploadSpeed\":%.2f," +
                "\"ping\":%.2f,\"traffic\":%.2f,\"duration\":%d,\"successful\":%s}",
                testId, downloadSpeed, uploadSpeed, ping, totalTraffic, duration, isSuccessful
        );
    }
    
    @Override
    public String toString() {
        return String.format(
                "TestResult{id='%s', download=%.1fMbps, traffic=%.1fMB, duration=%dms, successful=%s}",
                testId, downloadSpeed, totalTraffic, duration, isSuccessful
        );
    }
    
    /**
     * 构建器模式用于创建TestResult对象
     */
    public static class Builder {
        private String testId;
        private Date startTime;
        private Date endTime;
        
        private float downloadSpeed = 0;
        private float uploadSpeed = 0;
        private float ping = 0;
        
        private double totalTraffic = 0;
        private long totalBytes = 0;
        
        private String serverHost = "";
        private int serverPort = 0;
        
        private String networkQuality = "unknown";
        private int qualityScore = 0;
        
        private int testLoops = 0;
        private boolean isSuccessful = false;
        private String errorMessage = "";
        
        public Builder testId(String testId) {
            this.testId = testId;
            return this;
        }
        
        public Builder startTime(Date startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder endTime(Date endTime) {
            this.endTime = endTime;
            return this;
        }
        
        public Builder downloadSpeed(float downloadSpeed) {
            this.downloadSpeed = downloadSpeed;
            return this;
        }
        
        public Builder uploadSpeed(float uploadSpeed) {
            this.uploadSpeed = uploadSpeed;
            return this;
        }
        
        public Builder ping(float ping) {
            this.ping = ping;
            return this;
        }
        
        public Builder totalTraffic(double totalTraffic) {
            this.totalTraffic = totalTraffic;
            return this;
        }
        
        public Builder totalBytes(long totalBytes) {
            this.totalBytes = totalBytes;
            return this;
        }
        
        public Builder serverHost(String serverHost) {
            this.serverHost = serverHost;
            return this;
        }
        
        public Builder serverPort(int serverPort) {
            this.serverPort = serverPort;
            return this;
        }
        
        public Builder networkQuality(String networkQuality) {
            this.networkQuality = networkQuality;
            return this;
        }
        
        public Builder qualityScore(int qualityScore) {
            this.qualityScore = qualityScore;
            return this;
        }
        
        public Builder testLoops(int testLoops) {
            this.testLoops = testLoops;
            return this;
        }
        
        public Builder successful(boolean successful) {
            this.isSuccessful = successful;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public TestResult build() {
            // 基本验证
            if (testId == null || testId.trim().isEmpty()) {
                throw new IllegalArgumentException("Test ID cannot be null or empty");
            }
            if (startTime == null) {
                throw new IllegalArgumentException("Start time cannot be null");
            }
            if (endTime == null) {
                endTime = new Date(); // 使用当前时间作为结束时间
            }
            
            return new TestResult(this);
        }
    }
}