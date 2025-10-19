package com.swiftest.app.ui;

import androidx.appcompat.app.AppCompatActivity;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.swiftest.app.R;
import com.swiftest.app.services.SpeedTestService;
import com.swiftest.core.interfaces.ProgressListener;
import com.swiftest.core.interfaces.SpeedTestCallback;

public class MainActivity extends AppCompatActivity implements SpeedTestCallback, ProgressListener {
    private SpeedTestService speedTestService;
    private Handler ui = new Handler(Looper.getMainLooper());

    private ImageView ball;
    private FrameLayout ballFrame;
    private TextView startText;
    private TextView bandwidthText;
    private LinearLayout testInfoView;
    private TextView testDurationView;
    private TextView testTrafficView;
    private ProgressBar progressBar;
    private LinearLayout progressLayout;

    private ObjectAnimator ballRotation;
    private boolean isTesting = false;
    private long startAtMs = 0;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        speedTestService = new SpeedTestService();
        bindViews();
        ballFrame.setOnClickListener(v -> {
            if (!isTesting) startTest(); else stopTest();
        });
    }

    private void bindViews() {
        ball = findViewById(R.id.ball);
        ballFrame = findViewById(R.id.ball_frame);
        startText = findViewById(R.id.start_text);
        bandwidthText = findViewById(R.id.bandwidth_text);
        testInfoView = findViewById(R.id.test_info_layout);
        testDurationView = findViewById(R.id.test_duration);
        testTrafficView = findViewById(R.id.test_traffic);
        progressBar = findViewById(R.id.progress_bar);
        progressLayout = findViewById(R.id.progress_layout);
    }

    private void startTest() {
        isTesting = true;
        startAtMs = System.currentTimeMillis();
        speedTestService.startSpeedTest(this, this);
        startText.setText("测速中...");
        progressLayout.setVisibility(View.VISIBLE);
        testInfoView.setVisibility(View.GONE);
        ballRotation = ObjectAnimator.ofFloat(ball, "rotation", 0f, 360f);
        ballRotation.setDuration(2000);
        ballRotation.setRepeatCount(ValueAnimator.INFINITE);
        ballRotation.setInterpolator(new LinearInterpolator());
        ballRotation.start();
    }

    private void stopTest() {
        isTesting = false;
        speedTestService.stopSpeedTest();
        if (ballRotation != null) ballRotation.cancel();
        progressLayout.setVisibility(View.GONE);
        startText.setText("开始测速");
    }

    // SpeedTestCallback
    @Override public void onTestStarted() { ui.post(() -> startText.setText("正在连接...")); }

    @Override public void onTestCompleted(float download, float upload, float ping, double traffic) {
        ui.post(() -> {
            isTesting = false;
            if (ballRotation != null) ballRotation.cancel();
            progressLayout.setVisibility(View.GONE);
            bandwidthText.setText(String.format("%.1f Mbps", download));
            testTrafficView.setText(String.format("%.2f MB", traffic));
            long sec = (System.currentTimeMillis() - startAtMs) / 1000;
            testDurationView.setText(sec + "秒");
            testInfoView.setVisibility(View.VISIBLE);
            startText.setText("重新测试");
            Toast.makeText(this, "测速完成", Toast.LENGTH_SHORT).show();
        });
    }

    @Override public void onTestFailed(int code, String msg) {
        ui.post(() -> {
            isTesting = false;
            if (ballRotation != null) ballRotation.cancel();
            progressLayout.setVisibility(View.GONE);
            startText.setText("开始测速");
            Toast.makeText(this, "失败: " + msg, Toast.LENGTH_LONG).show();
        });
    }

    @Override public void onTestCancelled() { ui.post(this::stopTest); }
    @Override public void onConnectionStateChanged(boolean c) { ui.post(() -> startText.setText(c?"测速准备中...":"正在连接...")); }

    // ProgressListener
    @Override public void onProgressUpdate(int p, String stage) { ui.post(() -> { progressBar.setProgress(p); startText.setText(stage); }); }
    @Override public void onSpeedUpdate(float cur, float avg) { ui.post(() -> bandwidthText.setText(String.format("%.1f Mbps", cur))); }
    @Override public void onStageChanged(TestStage s, String d) { ui.post(() -> startText.setText(d)); }
    @Override public void onQualityUpdate(NetworkQuality q, int score) { /* no-op */ }

    @Override protected void onDestroy() { super.onDestroy(); if (ballRotation != null) ballRotation.cancel(); speedTestService.cleanup(); }
}
