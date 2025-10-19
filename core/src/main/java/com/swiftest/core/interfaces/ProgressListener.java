package com.swiftest.core.interfaces;

/**
 * 进度监听接口
 * 用于监听测速过程中的进度更新和实时状态
 */
public interface ProgressListener {
    
    /**
     * 进度更新时调用
     * @param progress 进度百分比 (0-100)
     * @param stage 当前阶段描述
     */
    void onProgressUpdate(int progress, String stage);
    
    /**
     * 实时速度更新时调用
     * @param currentSpeed 当前瞬时速度 (Mbps)
     * @param averageSpeed 平均速度 (Mbps)
     */
    void onSpeedUpdate(float currentSpeed, float averageSpeed);
    
    /**
     * 测试阶段变化时调用
     * @param stage 测试阶段
     * @param description 阶段描述
     */
    void onStageChanged(TestStage stage, String description);
    
    /**
     * 网络质量评估更新时调用
     * @param quality 网络质量等级
     * @param score 网络质量分数 (0-100)
     */
    void onQualityUpdate(NetworkQuality quality, int score);
    
    /**
     * 测试阶段枚举
     */
    enum TestStage {
        CONNECTING("正在连接服务器"),
        PREPARING("准备测速"),
        DOWNLOADING("下载测速中"),
        UPLOADING("上传测速中"), // 预留
        ANALYZING("分析结果"),
        COMPLETED("测试完成");
        
        private final String description;
        
        TestStage(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 网络质量等级枚举
     */
    enum NetworkQuality {
        EXCELLENT(90, "优秀"),
        GOOD(70, "良好"), 
        FAIR(50, "一般"),
        POOR(30, "较差"),
        VERY_POOR(0, "很差");
        
        private final int threshold;
        private final String description;
        
        NetworkQuality(int threshold, String description) {
            this.threshold = threshold;
            this.description = description;
        }
        
        public int getThreshold() {
            return threshold;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * 根据速度获取网络质量等级
         * @param speedMbps 速度 (Mbps)
         * @return 网络质量等级
         */
        public static NetworkQuality fromSpeed(float speedMbps) {
            if (speedMbps >= 100) return EXCELLENT;
            if (speedMbps >= 50) return GOOD;
            if (speedMbps >= 20) return FAIR;
            if (speedMbps >= 5) return POOR;
            return VERY_POOR;
        }
    }
}