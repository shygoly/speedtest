package com.swiftest.core.interfaces;

/**
 * UDP测速回调接口
 * 用于处理UDP测速过程中的各种事件
 */
public interface UdpTestCallback {
    
    /**
     * 触发包发送超时时调用
     */
    void onTriggerTimeout();
    
    /**
     * UDP测试开始时调用
     * @param loop 当前循环次数
     * @param speed 当前测试速度
     */
    void onTestStart(int loop, int speed);
    
    /**
     * 收到第一个UDP包时调用
     */
    void onFirstPacketReceived();
    
    /**
     * 单次测试完成时调用
     * @param speed 本次测试的速度结果
     * @param isSaturated 是否达到饱和状态
     * @param isTestEnd 是否所有测试结束
     */
    void onSingleTestComplete(float speed, boolean isSaturated, boolean isTestEnd);
    
    /**
     * 所有测试完成时调用
     * @param finalSpeed 最终平均速度
     */
    void onAllTestsComplete(float finalSpeed);
    
    /**
     * UDP测试过程中发生错误时调用
     * @param error 错误信息
     */
    void onUdpError(String error);
    
    /**
     * 没有收到任何数据时调用
     */
    void onNoDataReceived();
}