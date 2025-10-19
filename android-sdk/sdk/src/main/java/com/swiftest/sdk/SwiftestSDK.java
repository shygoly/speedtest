package com.swiftest.sdk;

import android.util.Log;

import com.swiftest.core.interfaces.ProgressListener;
import com.swiftest.core.interfaces.SpeedTestCallback;
import com.swiftest.core.models.SpeedTestConfig;
import com.swiftest.core.protocol.SpeedTestProtocol;

/**
 * Public SDK entrypoint
 */
public class SwiftestSDK {
    private static final String TAG = "SwiftestSDK";

    private final SpeedTestConfig config;
    private SpeedTestProtocol protocol;

    private SwiftestSDK(Builder b) {
        this.config = new SpeedTestConfig.Builder()
                .serverHost(b.serverHost)
                .webSocketPort(b.wsPort)
                .guid(b.guid)
                .testId(b.testId)
                .appMode(false)
                .build();
    }

    public void start(SpeedTestCallback callback, ProgressListener progressListener) {
        protocol = new SpeedTestProtocol(config, new SpeedTestProtocol.ProtocolCallback() {
            @Override public void onTestStarted() { if (callback!=null) callback.onTestStarted(); }
            @Override public void onProgressUpdate(int progress, float currentSpeed) { if (progressListener!=null) progressListener.onProgressUpdate(progress, "测速中"); if (progressListener!=null) progressListener.onSpeedUpdate(currentSpeed, currentSpeed); }
            @Override public void onTestCompleted(float downloadSpeed, double traffic) { if (callback!=null) callback.onTestCompleted(downloadSpeed, 0, 0, traffic); }
            @Override public void onTestFailed(String error) { if (callback!=null) callback.onTestFailed(SpeedTestCallback.ErrorCodes.UNKNOWN_ERROR, error); }
            @Override public void onNetworkIssue(int issueType) { /* surface via onTestFailed already */ }
        });
        protocol.startSpeedTest();
    }

    public void stop() {
        if (protocol != null) {
            protocol.stopSpeedTest();
        }
    }

    public static class Builder {
        private String serverHost;
        private int wsPort = 8080;
        private String guid = "sdk";
        private String testId = "sdk_test";

        public Builder serverHost(String h) { this.serverHost = h; return this; }
        public Builder wsPort(int p) { this.wsPort = p; return this; }
        public Builder guid(String g) { this.guid = g; return this; }
        public Builder testId(String id) { this.testId = id; return this; }
        public SwiftestSDK build() { return new SwiftestSDK(this); }
    }
}
