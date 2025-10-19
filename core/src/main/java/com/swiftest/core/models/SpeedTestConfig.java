package com.swiftest.core.models;

/**
 * 测速配置参数
 * 包含测速所需的所有配置信息
 */
public class SpeedTestConfig {
    
    private final String serverHost;
    private final int webSocketPort;
    private final int udpPort;
    private final String testId;
    private final String guid;
    private final int sendTimeoutSeconds;
    private final int receiveTimeoutSeconds;
    private final boolean isAppMode; // true: App模式, false: SDK模式
    
    private SpeedTestConfig(Builder builder) {
        this.serverHost = builder.serverHost;
        this.webSocketPort = builder.webSocketPort;
        this.udpPort = builder.udpPort;
        this.testId = builder.testId;
        this.guid = builder.guid;
        this.sendTimeoutSeconds = builder.sendTimeoutSeconds;
        this.receiveTimeoutSeconds = builder.receiveTimeoutSeconds;
        this.isAppMode = builder.isAppMode;
    }
    
    // Getters
    public String getServerHost() { return serverHost; }
    public int getWebSocketPort() { return webSocketPort; }
    public int getUdpPort() { return udpPort; }
    public String getTestId() { return testId; }
    public String getGuid() { return guid; }
    public int getSendTimeoutSeconds() { return sendTimeoutSeconds; }
    public int getReceiveTimeoutSeconds() { return receiveTimeoutSeconds; }
    public boolean isAppMode() { return isAppMode; }
    
    public String getWebSocketUrl() {
        return "ws://" + serverHost + ":" + webSocketPort;
    }
    
    /**
     * 构建器模式用于创建配置对象
     */
    public static class Builder {
        private String serverHost;
        private int webSocketPort = 8080; // 默认端口
        private int udpPort = 0; // 由服务器动态分配
        private String testId;
        private String guid;
        private int sendTimeoutSeconds = 1;
        private int receiveTimeoutSeconds = 3;
        private boolean isAppMode = true;
        
        public Builder serverHost(String serverHost) {
            this.serverHost = serverHost;
            return this;
        }
        
        public Builder webSocketPort(int port) {
            this.webSocketPort = port;
            return this;
        }
        
        public Builder udpPort(int port) {
            this.udpPort = port;
            return this;
        }
        
        public Builder testId(String testId) {
            this.testId = testId;
            return this;
        }
        
        public Builder guid(String guid) {
            this.guid = guid;
            return this;
        }
        
        public Builder sendTimeout(int seconds) {
            this.sendTimeoutSeconds = seconds;
            return this;
        }
        
        public Builder receiveTimeout(int seconds) {
            this.receiveTimeoutSeconds = seconds;
            return this;
        }
        
        public Builder appMode(boolean isAppMode) {
            this.isAppMode = isAppMode;
            return this;
        }
        
        public SpeedTestConfig build() {
            if (serverHost == null || serverHost.trim().isEmpty()) {
                throw new IllegalArgumentException("Server host cannot be null or empty");
            }
            return new SpeedTestConfig(this);
        }
    }
}