package com.swiftest.core.interfaces;

import org.json.JSONObject;

/**
 * WebSocket连接的回调接口
 * 用于处理WebSocket连接的各种状态和消息事件
 */
public interface WebSocketCallback {
    
    /**
     * 连接成功建立时调用
     */
    void onConnected();
    
    /**
     * 连接失败时调用
     * @param error 错误信息
     */
    void onConnectionFailed(String error);
    
    /**
     * 收到服务器消息时调用
     * @param message 服务器发送的JSON消息
     */
    void onMessageReceived(JSONObject message);
    
    /**
     * 收到UDP端口信息时调用
     * @param udpPort 服务器分配的UDP端口
     */
    void onUdpPortReceived(int udpPort);
    
    /**
     * 收到测速开始指令时调用
     * @param speed 初始测速速度
     */
    void onSpeedTestStart(int speed);
    
    /**
     * 收到测速结束指令时调用
     * @param traffic 总流量数据
     */
    void onSpeedTestFinish(double traffic);
    
    /**
     * 收到超出限制消息时调用
     */
    void onSpeedExceeded();
    
    /**
     * 发送消息超时时调用
     */
    void onSendTimeout();
    
    /**
     * 接收消息超时时调用
     */
    void onReceiveTimeout();
    
    /**
     * 连接断开时调用
     * @param code 断开代码
     * @param reason 断开原因
     */
    void onDisconnected(int code, String reason);
}