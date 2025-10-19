package com.swiftest.core.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 速度指标数据模型
 * 用于记录和分析测速过程中的各种指标
 */
public class SpeedMetrics {
    
    // 基本指标
    private final List<Float> speedSamples;        // 速度样本列表
    private final List<Long> bytesReceived;        // 字节接收记录
    private final List<Long> timestamps;           // 时间戳记录
    
    // 统计指标
    private final float averageSpeed;              // 平均速度
    private final float maxSpeed;                  // 最大速度
    private final float minSpeed;                  // 最小速度
    private final float medianSpeed;               // 中位数速度
    private final float percentile95;              // 95百分位速度
    private final float standardDeviation;        // 标准差
    
    // 稳定性指标
    private final float stability;                 // 稳定性指数 (0-100)
    private final float consistency;               // 一致性指数 (0-100)
    private final int fluctuationCount;            // 波动次数
    
    // 质量评估
    private final String qualityGrade;             // 质量等级
    private final int qualityScore;                // 质量分数 (0-100)
    
    private SpeedMetrics(Builder builder) {
        this.speedSamples = Collections.unmodifiableList(new ArrayList<>(builder.speedSamples));
        this.bytesReceived = Collections.unmodifiableList(new ArrayList<>(builder.bytesReceived));
        this.timestamps = Collections.unmodifiableList(new ArrayList<>(builder.timestamps));
        
        // 计算统计指标
        this.averageSpeed = calculateAverage(speedSamples);
        this.maxSpeed = calculateMax(speedSamples);
        this.minSpeed = calculateMin(speedSamples);
        this.medianSpeed = calculateMedian(speedSamples);
        this.percentile95 = calculatePercentile(speedSamples, 95);
        this.standardDeviation = calculateStandardDeviation(speedSamples, averageSpeed);
        
        // 计算稳定性指标
        this.stability = calculateStability(speedSamples);
        this.consistency = calculateConsistency(speedSamples);
        this.fluctuationCount = calculateFluctuationCount(speedSamples);
        
        // 质量评估
        this.qualityScore = calculateQualityScore();
        this.qualityGrade = calculateQualityGrade(qualityScore);
    }
    
    // Getters
    public List<Float> getSpeedSamples() { return speedSamples; }
    public List<Long> getBytesReceived() { return bytesReceived; }
    public List<Long> getTimestamps() { return timestamps; }
    
    public float getAverageSpeed() { return averageSpeed; }
    public float getMaxSpeed() { return maxSpeed; }
    public float getMinSpeed() { return minSpeed; }
    public float getMedianSpeed() { return medianSpeed; }
    public float getPercentile95() { return percentile95; }
    public float getStandardDeviation() { return standardDeviation; }
    
    public float getStability() { return stability; }
    public float getConsistency() { return consistency; }
    public int getFluctuationCount() { return fluctuationCount; }
    
    public String getQualityGrade() { return qualityGrade; }
    public int getQualityScore() { return qualityScore; }
    
    // 计算方法
    
    private float calculateAverage(List<Float> samples) {
        if (samples.isEmpty()) return 0f;
        float sum = 0f;
        for (float speed : samples) {
            sum += speed;
        }
        return sum / samples.size();
    }
    
    private float calculateMax(List<Float> samples) {
        if (samples.isEmpty()) return 0f;
        return Collections.max(samples);
    }
    
    private float calculateMin(List<Float> samples) {
        if (samples.isEmpty()) return 0f;
        return Collections.min(samples);
    }
    
    private float calculateMedian(List<Float> samples) {
        if (samples.isEmpty()) return 0f;
        
        List<Float> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2f;
        } else {
            return sorted.get(size / 2);
        }
    }
    
    private float calculatePercentile(List<Float> samples, int percentile) {
        if (samples.isEmpty()) return 0f;
        
        List<Float> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        
        int index = (percentile * sorted.size()) / 100;
        if (index >= sorted.size()) index = sorted.size() - 1;
        
        return sorted.get(index);
    }
    
    private float calculateStandardDeviation(List<Float> samples, float average) {
        if (samples.size() <= 1) return 0f;
        
        float sumSquares = 0f;
        for (float speed : samples) {
            float diff = speed - average;
            sumSquares += diff * diff;
        }
        
        return (float) Math.sqrt(sumSquares / (samples.size() - 1));
    }
    
    private float calculateStability(List<Float> samples) {
        if (samples.size() <= 1) return 100f;
        
        // 计算变异系数，然后转换为稳定性指数
        float cv = (standardDeviation / averageSpeed) * 100;
        
        // 变异系数越小，稳定性越高
        // CV < 10% = 优秀(90-100), CV < 20% = 良好(70-90), 以此类推
        if (cv <= 5) return 100f;
        if (cv <= 10) return 95f - cv;
        if (cv <= 20) return 85f - (cv - 10) * 1.5f;
        if (cv <= 30) return 70f - (cv - 20) * 2f;
        return Math.max(0f, 50f - (cv - 30) * 1.5f);
    }
    
    private float calculateConsistency(List<Float> samples) {
        if (samples.size() <= 1) return 100f;
        
        // 计算相邻样本的差异程度
        float totalDiff = 0f;
        int diffCount = 0;
        
        for (int i = 1; i < samples.size(); i++) {
            float diff = Math.abs(samples.get(i) - samples.get(i - 1));
            totalDiff += diff;
            diffCount++;
        }
        
        float avgDiff = diffCount > 0 ? totalDiff / diffCount : 0f;
        float consistency = 100f - Math.min(100f, (avgDiff / averageSpeed) * 100);
        
        return Math.max(0f, consistency);
    }
    
    private int calculateFluctuationCount(List<Float> samples) {
        if (samples.size() <= 2) return 0;
        
        int fluctuations = 0;
        boolean increasing = samples.get(1) > samples.get(0);
        
        for (int i = 2; i < samples.size(); i++) {
            boolean currentIncreasing = samples.get(i) > samples.get(i - 1);
            if (currentIncreasing != increasing) {
                fluctuations++;
                increasing = currentIncreasing;
            }
        }
        
        return fluctuations;
    }
    
    private int calculateQualityScore() {
        // 综合评分：速度40% + 稳定性30% + 一致性30%
        float speedScore = Math.min(100f, (averageSpeed / 100f) * 40f); // 以100Mbps为满分基准
        float stabilityScore = stability * 0.3f;
        float consistencyScore = consistency * 0.3f;
        
        return (int) (speedScore + stabilityScore + consistencyScore);
    }
    
    private String calculateQualityGrade(int score) {
        if (score >= 90) return "优秀";
        if (score >= 80) return "良好";
        if (score >= 70) return "一般";
        if (score >= 60) return "及格";
        return "较差";
    }
    
    /**
     * 获取性能摘要
     */
    public String getPerformanceSummary() {
        return String.format(
                "平均速度: %.1f Mbps | 最大速度: %.1f Mbps | 稳定性: %.1f%% | 质量: %s(%d分)",
                averageSpeed, maxSpeed, stability, qualityGrade, qualityScore
        );
    }
    
    @Override
    public String toString() {
        return String.format(
                "SpeedMetrics{avg=%.1f, max=%.1f, stability=%.1f%%, quality=%s}",
                averageSpeed, maxSpeed, stability, qualityGrade
        );
    }
    
    /**
     * 构建器模式
     */
    public static class Builder {
        private List<Float> speedSamples = new ArrayList<>();
        private List<Long> bytesReceived = new ArrayList<>();
        private List<Long> timestamps = new ArrayList<>();
        
        public Builder addSpeedSample(float speed) {
            speedSamples.add(speed);
            return this;
        }
        
        public Builder addSpeedSamples(List<Float> speeds) {
            speedSamples.addAll(speeds);
            return this;
        }
        
        public Builder addBytesReceived(long bytes) {
            bytesReceived.add(bytes);
            return this;
        }
        
        public Builder addTimestamp(long timestamp) {
            timestamps.add(timestamp);
            return this;
        }
        
        public SpeedMetrics build() {
            return new SpeedMetrics(this);
        }
    }
    
    /**
     * 从原始数据创建SpeedMetrics的工厂方法
     */
    public static SpeedMetrics fromRawData(List<Long> bytesRecord, List<Long> timeRecord) {
        Builder builder = new Builder();
        
        // 添加时间戳
        for (Long timestamp : timeRecord) {
            builder.addTimestamp(timestamp);
        }
        
        // 添加字节记录
        for (Long bytes : bytesRecord) {
            builder.addBytesReceived(bytes);
        }
        
        // 计算速度样本
        for (int i = 0; i < bytesRecord.size() && i < timeRecord.size(); i++) {
            if (i >= 5) {
                // 使用滑动窗口计算速度
                long bytesDiff = bytesRecord.get(i) - bytesRecord.get(i - 5);
                long timeDiff = timeRecord.get(i) - timeRecord.get(i - 5);
                
                if (timeDiff > 0) {
                    float speed = (bytesDiff * 8.0f) / (timeDiff * 1024 * 1024); // 转换为Mbps
                    builder.addSpeedSample(speed);
                }
            } else if (i > 0) {
                // 前几个点使用累积计算
                long totalBytes = bytesRecord.get(i);
                long totalTime = timeRecord.get(i) - timeRecord.get(0);
                
                if (totalTime > 0) {
                    float speed = (totalBytes * 8.0f) / (totalTime * 1024 * 1024); // 转换为Mbps
                    builder.addSpeedSample(speed);
                }
            }
        }
        
        return builder.build();
    }
}