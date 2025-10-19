package com.swiftest.sample;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.swiftest.core.interfaces.ProgressListener;
import com.swiftest.core.interfaces.SpeedTestCallback;
import com.swiftest.sdk.SwiftestSDK;

public class MainActivity extends AppCompatActivity implements SpeedTestCallback, ProgressListener {
    private SwiftestSDK sdk;
    private Handler ui = new Handler(Looper.getMainLooper());

    private TextView status;
    private Button start;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        status = new TextView(this);
        start = new Button(this);
        start.setText("Start Test");
        start.setOnClickListener(v -> startTest());
        setContentView(status);
        addContentView(start, new android.widget.FrameLayout.LayoutParams(-2, -2));

        sdk = new SwiftestSDK.Builder().serverHost("swiftest.thucloud.com").build();
    }

    private void startTest() { sdk.start(this, this); }

    @Override public void onTestStarted() { ui.post(() -> status.setText("Starting...")); }
    @Override public void onTestCompleted(float d, float u, float p, double t) { ui.post(() -> status.setText("Done: " + d + " Mbps")); }
    @Override public void onTestFailed(int code, String msg) { ui.post(() -> status.setText("Failed: " + msg)); }
    @Override public void onTestCancelled() { ui.post(() -> status.setText("Cancelled")); }
    @Override public void onConnectionStateChanged(boolean c) { }
    @Override public void onProgressUpdate(int progress, String stage) { }
    @Override public void onSpeedUpdate(float currentSpeed, float averageSpeed) { }
    @Override public void onStageChanged(TestStage stage, String description) { }
    @Override public void onQualityUpdate(NetworkQuality quality, int score) { }
}
