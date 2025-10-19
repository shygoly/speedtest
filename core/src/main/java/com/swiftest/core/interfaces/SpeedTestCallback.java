package com.swiftest.core.interfaces;

/**
 * 标准化的测速回调接口
 * 用于通知测速过程中的各种状态和结果
 */
public interface SpeedTestCallback {
    
    /**
     * 测速开始时调用
     */
    void onTestStarted();
    
    /**
     * 测速完成时调用
     * @param downloadSpeed 下载速度 (Mbps)
     * @param uploadSpeed 上传速度 (Mbps) - 当前版本暂不支持
     * @param ping 延迟 (ms) - 当前版本暂不支持
     * @param totalTraffic 总流量 (MB)
     */
    void onTestCompleted(float downloadSpeed, float uploadSpeed, float ping, double totalTraffic);
    
    /**
     * 测速失败时调用
     * @param errorCode 错误代码
     * @param errorMessage 错误信息
     */
    void onTestFailed(int errorCode, String errorMessage);
    
    /**
     * 测速被取消时调用
     */
    void onTestCancelled();
    
    /**
     * 网络连接状态变化时调用
     * @param isConnected 是否已连接
     */
    void onConnectionStateChanged(boolean isConnected);
    
    /**
     * 错误代码常量
     */
    class ErrorCodes {
        public static final int NETWORK_ERROR = 1001;
        public static final int CONNECTION_TIMEOUT = 1002;
        public static final int SERVER_ERROR = 1003;
        public static final int UDP_ERROR = 1004;
        public static final int WEBSOCKET_ERROR = 1005;
        public static final int CONFIG_ERROR = 1006;
        public static final int UNKNOWN_ERROR = 9999;
    }
}