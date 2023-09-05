package com.example.swiftestplus;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.Manifest;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.swiftestplus.Service.ControllerService;
import com.example.swiftestplus.Service.HttpCallback;
import com.example.swiftestplus.Service.WsService;
import com.example.swiftestplus.Utils.GUIDUtils;
import com.example.swiftestplus.Utils.NetworkInfo;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private final Handler handler  = new Handler(Looper.getMainLooper());
    public static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 1;
    public static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1;
    NetworkInfo network_info;
    String androidID;
//    String guid;
//    GUIDUtils guidUtils;
    ControllerService controllerService;
    String BPS;
    String testID;
    WsService wsService;
    Float testDuration;
    public Double testTraffic;
    public Float bandwidth;

    // UI组件
    ImageView ball;
    FrameLayout ballFrame;
    TextView startText;
    TextView bandwidthText;
    TextView bandwidthLabelUp;
    TextView bandwidthLabelDown;
    LinearLayout testInfoView;
    TextView networkTypeView;
    TextView networkDetailView;
    TextView testDurationView;
    TextView testTrafficView;
    LineChart lineChart;
    public ArrayList<Entry> chartValues;
    FrameLayout chartFrame;
    LinearLayout copyrightLayout;
    ProgressBar progressBar;

    ObjectAnimator ballRotation;


    // 屏幕参数
    int screenWidth;
    int screenHeight;
    float ballUpTranslation;
    float ballInitSize;
    float ballLargeSize;
    float ballSmallSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ball = findViewById(R.id.ball);
        ballFrame = findViewById(R.id.ball_frame);
        startText = findViewById(R.id.start_text);
        bandwidthText = findViewById(R.id.bandwidth_text);
        bandwidthLabelUp = findViewById(R.id.bandwidth_label_up);
        bandwidthLabelDown = findViewById(R.id.bandwidth_label_down);
        testInfoView = findViewById(R.id.test_info_layout);
        networkTypeView = findViewById(R.id.network_type);
        networkDetailView = findViewById(R.id.network_detail);
        testDurationView = findViewById(R.id.test_duration);
        testTrafficView = findViewById(R.id.test_traffic);
        chartFrame = findViewById(R.id.chart_frame);
        lineChart = findViewById(R.id.line_chart);
        copyrightLayout = findViewById(R.id.copyright_text_layout);
        progressBar = findViewById(R.id.progress_bar);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        androidID = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
//        guidUtils = new GUIDUtils(this);
//        guid = guidUtils.getGuid();

        initEverything();

        // 获取授权 && 展示网络信息
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
                // todo: 重新获取网络授权
            }
            else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
            }
        }

        // 获取位置信息
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // todo: 重新获取位置授权
            }
            else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_ACCESS_FINE_LOCATION);
            }
        }

        // 设置上滑事件：开始测速
        ballFrame.setOnTouchListener(new View.OnTouchListener() {
            float downY;
            float originalY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downY = event.getY();
                        originalY = v.getY();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float deltaY = event.getY()-downY;
                        float newY = originalY+Math.max(deltaY, -60);
                        if (deltaY<0) {
                            v.setY(newY);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        setTestStartUI();
                        break;
                }
                return true;
            }
        });

    }

    // 1. 网络信息
    // 1.1 展示网络信息
    public void getAndShowNetworkInfo() {
        network_info = new NetworkInfo(this, this);
        network_info.getNetworkInfo();
        if (network_info.isConnected()){
            if (network_info.getNetwork_type().contains("WiFi")){
                networkTypeView.setText("网络类型：" + network_info.getNetwork_type());
                networkDetailView.setText("WiFi名称：" + network_info.getWifi_name());
            }
            else {
                if (network_info.getNetwork_type().equals("Mixed")) {
                    networkTypeView.setText("网络类型：" + "混合网络");
                } else {
                    networkTypeView.setText("网络类型：" + network_info.getNetwork_type());
                    networkDetailView.setText("运营商：" + network_info.getCellular_carrier());
                }
            }
        }
        else {
            setNetworkIssueUI(1);
        }
    }
    // 1.2 授权处理
    public void onRequestPermissionResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_PHONE_STATE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getAndShowNetworkInfo();
                }
                else {
                    // todo: 用户拒绝权限
                }
            }
        }
    }
    // 1.3 测速信息展示
    public void showTestInfo() {
        testDuration = chartValues.get(chartValues.size()-1).getX();
        testDurationView.setText("测试时长：" + String.format("%.1f", testDuration) + "秒");
        testTrafficView.setText("    耗费流量：" + String.format("%.1f", testTraffic) + "MB");
    }
    // 1.4 重置测速信息
    public void resetTestInfo() {
        networkTypeView.setText("");
        networkDetailView.setText("");
        testDurationView.setText("");
        testTrafficView.setText("");
    }

    // 2. 动画效果
    // 2.1 淡入淡出
    private AlphaAnimation fadeIn(int ms) {
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(ms);
        return fadeIn;
    }
    private AlphaAnimation fadeOut(int ms) {
        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(ms);
        return fadeOut;
    }
    private AlphaAnimation fadeOutAndIn(final TextView view, int ms, String text) {
        AlphaAnimation fadeOutAndIn = new AlphaAnimation(1.0f, 0.0f);
        fadeOutAndIn.setDuration(ms/2);
        fadeOutAndIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                view.setText(text);
                view.startAnimation(fadeIn(ms/2));
                view.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationRepeat(Animation animation) { }
        });
        return fadeOutAndIn;
    }
    // 2.2 移动
    private void moveTexts() {
        float textStart = bandwidthText.getLeft();
        float labelStartX = bandwidthLabelDown.getLeft();
        float labelStartY = bandwidthLabelDown.getBottom();
        float textDest = (bandwidthText.getRootView().getWidth()-(bandwidthText.getWidth()+bandwidthLabelDown.getWidth()+20f))/2f;
        float textDelta = textDest - textStart;
        float labelDestX = textDest + bandwidthText.getWidth() + 20f;
        float labelDeltaX = labelDestX - labelStartX;
        float labelDestY = bandwidthText.getBottom();
        float labelDeltaY = labelDestY - labelStartY;
        ObjectAnimator bandwidthX = ObjectAnimator.ofFloat(bandwidthText, "translationX", textDelta);
        ObjectAnimator labelX = ObjectAnimator.ofFloat(bandwidthLabelDown, "translationX", labelDeltaX);
        ObjectAnimator labelY = ObjectAnimator.ofFloat(bandwidthLabelDown, "translationY", labelDeltaY-bandwidthText.getHeight()*0.1f);
        ObjectAnimator infoY = ObjectAnimator.ofFloat(testInfoView, "translationY", -40f);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(bandwidthX, labelX, labelY, infoY);
        animatorSet.setDuration(400);
        animatorSet.start();
    }
    private void moveTextsBack() {
        ObjectAnimator bandwidthX = ObjectAnimator.ofFloat(bandwidthText, "translationX", bandwidthText.getTranslationX(), 0);
        ObjectAnimator labelX = ObjectAnimator.ofFloat(bandwidthLabelDown, "translationX", bandwidthLabelDown.getTranslationX(), 0);
        ObjectAnimator labelY = ObjectAnimator.ofFloat(bandwidthLabelDown, "translationY", bandwidthLabelDown.getTranslationY(), 0);
        ObjectAnimator infoY = ObjectAnimator.ofFloat(testInfoView, "translationY", testInfoView.getTranslationY(), 0);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(bandwidthX, labelX, labelY);
        animatorSet.start();
    }
    // 2.3 移动 & 旋转 & 调整大小
    private void ballUpAndRotate() {
        int time;
        if (startText.getText().equals("上拉开始")) {
            time = 400;
            ballInitSize = ballFrame.getWidth();
            Log.d("UI", "ballInitSize:" + ballInitSize);
            int[] ballLocation = new int[2];
            ballFrame.getLocationOnScreen(ballLocation);
            int[] upLabelLocation = new int[2];
            bandwidthLabelUp.getLocationOnScreen(upLabelLocation);
            ballLargeSize = upLabelLocation[1] * 0.75f *2f;
            ballUpTranslation = -ballLocation[1] - ballInitSize/2f;
        } else {
            time = 800;
        }
        float startY = ballFrame.getTranslationY();
        ObjectAnimator ballMoveUp = ObjectAnimator.ofFloat(ballFrame, "translationY", startY, ballUpTranslation);
        ObjectAnimator ballRotate = ObjectAnimator.ofFloat(ballFrame, "rotation", ballFrame.getRotation(), ballFrame.getRotation()+60f);
        ObjectAnimator ballScaleX = ObjectAnimator.ofFloat(ballFrame, "scaleX", ballFrame.getScaleX(), ballLargeSize/ballInitSize);
        ObjectAnimator ballScaleY = ObjectAnimator.ofFloat(ballFrame, "scaleY", ballFrame.getScaleY(), ballLargeSize/ballInitSize);
        AnimatorSet ballAnimations = new AnimatorSet();
        ballAnimations.playTogether(ballMoveUp, ballRotate, ballScaleX, ballScaleY);
        ballAnimations.setDuration(time);
        ballAnimations.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                // 动画结束后测试开始
                testBandwidth();
            }
        });
        ballAnimations.start();
    }
    private void ballRotate() {
        ballRotation = ObjectAnimator.ofFloat(ballFrame, "rotation", ballFrame.getRotation(), ballFrame.getRotation()+720f);
        ballRotation.setDuration(4800);
        ballRotation.setRepeatCount(ValueAnimator.INFINITE);
        ballRotation.setInterpolator(new AccelerateDecelerateInterpolator());
    }
    private void ballAgain() {
        // 向上移出屏幕
        float startY = ballFrame.getTranslationY();
        float endY = -ballFrame.getHeight()-ballFrame.getTop();
        ObjectAnimator ballMoveUpTop = ObjectAnimator.ofFloat(ballFrame, "translationY",  startY, endY);
        ObjectAnimator ballRotateTop = ObjectAnimator.ofFloat(ballFrame, "Rotation", ballFrame.getRotation(), ballFrame.getRotation()+30f);
        ObjectAnimator ballScaleXTop = ObjectAnimator.ofFloat(ballFrame, "scaleX", ballFrame.getScaleX(), 0.8f);
        ObjectAnimator ballScaleYTop = ObjectAnimator.ofFloat(ballFrame, "scaleY", ballFrame.getScaleY(), 0.8f);
        AnimatorSet ballTop = new AnimatorSet();
        ballTop.playTogether(ballMoveUpTop, ballRotateTop, ballScaleXTop, ballScaleYTop);
        ballTop.setDuration(400);
        ballTop.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                updateLineChart();
                // 更新测速文字
                bandwidthLabelUp.startAnimation(fadeOutAndIn(bandwidthLabelUp, 400, "下行带宽"));
                bandwidthLabelUp.setVisibility(View.INVISIBLE);
                moveTexts();
                testInfoView.startAnimation(fadeIn(400));
                testInfoView.setVisibility(View.VISIBLE);
                // 从屏幕下方出现
                super.onAnimationEnd(animation);
                int[] chartLocation = new int[2];
                chartFrame.getLocationOnScreen(chartLocation);
                int chartBottomLocation = chartLocation[1] + chartFrame.getHeight();
                int[] copyrightLocation = new int[2];
                copyrightLayout.getLocationOnScreen(copyrightLocation);
                ballSmallSize = (copyrightLocation[1] - chartBottomLocation) * 0.7f;
                float distanceToBottom = ballFrame.getRootView().getHeight() - ballFrame.getTop();
                ballFrame.setTranslationY(distanceToBottom);
                float startY = ballFrame.getTranslationY();
                float endY = startY + (chartBottomLocation + ballSmallSize / 7f * 1.5f) - ballFrame.getRootView().getHeight() - (ballInitSize - ballSmallSize) / 2f;
                Log.d("UI", "startY:" + startY);
                Log.d("UI", "endY:" + endY);
                ObjectAnimator ballMoveBottom = ObjectAnimator.ofFloat(ballFrame, "translationY", startY, endY);
                ObjectAnimator ballRotateBottom = ObjectAnimator.ofFloat(ballFrame, "Rotation", ballFrame.getRotation(), ballFrame.getRotation()+30f);
                ObjectAnimator ballScaleXBottom = ObjectAnimator.ofFloat(ballFrame, "scaleX", ballFrame.getScaleX(), ballSmallSize/ballInitSize);
                ObjectAnimator ballScaleYBottom = ObjectAnimator.ofFloat(ballFrame, "scaleY", ballFrame.getScaleY(), ballSmallSize/ballInitSize);
                AnimatorSet ballBottom = new AnimatorSet();
                ballBottom.playTogether(ballMoveBottom, ballRotateBottom, ballScaleXBottom, ballScaleYBottom);
                ballBottom.setDuration(400);
                ballBottom.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        ballFrame.setEnabled(true);
                    }
                });
                ballBottom.start();
                startTextAgain();
            }
        });
        ballTop.start();
    }
    private void startTextAgain() {
        startText.setText("上拉重测");
        startText.startAnimation(fadeIn(400));
        startText.setVisibility(View.VISIBLE);
        float ballRotation = ballFrame.getRotation();
        startText.setRotation(-ballRotation-30);
    }
    // 2.3 折线图
    private void initLineChart() {
        // 坐标轴不可见
//        lineChart.getXAxis().setEnabled(false);
        lineChart.getAxisLeft().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        Description description = new Description();
        description.setEnabled(false); // 禁用描述
        if (lineChart.getData() == null || lineChart.getData().getEntryCount() == 0) {
            lineChart.setTouchEnabled(false);
        } else {
            lineChart.setTouchEnabled(true);
        }

        lineChart.setDescription(description);


        int chartColor = ContextCompat.getColor(this, R.color.text_h2);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisLineColor(chartColor);
        xAxis.setTextColor(chartColor);

        // 数据
        chartValues = new ArrayList<>();

        LineDataSet initDataset = new LineDataSet(chartValues, "下行带宽");
        initDataset.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        initDataset.setDrawCircles(false);

        // todo: 渐变网格线

        ArrayList<ILineDataSet> initDataSets = new ArrayList<>();
        initDataSets.add(initDataset);
        LineData initData = new LineData(initDataSets);
        lineChart.setData(initData);
        lineChart.invalidate();
    }
    public void updateLineChart() {
        handler.post(() -> {
            Log.d("Line Chart", chartValues.toString());
           LineDataSet bandwidthDataset = new LineDataSet(chartValues,"下行带宽");

           // 折线样式
           bandwidthDataset.setMode(LineDataSet.Mode.CUBIC_BEZIER);
           bandwidthDataset.setCubicIntensity(0.2f);
           bandwidthDataset.setLineWidth(3f);
           bandwidthDataset.setColor(ContextCompat.getColor(this, R.color.text_h2));
           bandwidthDataset.setDrawCircles(false);
           bandwidthDataset.setDrawValues(false);
            if (lineChart.getData() == null || lineChart.getData().getEntryCount() == 0) {
                lineChart.setTouchEnabled(false);
            } else {
                lineChart.setTouchEnabled(true);
            }

           ArrayList<ILineDataSet> bandwidthDatasets = new ArrayList<>();
           bandwidthDatasets.add(bandwidthDataset);
           LineData bandwidthData = new LineData(bandwidthDatasets);
           lineChart.setData(bandwidthData);
           lineChart.invalidate();
            lineChart.animateX(400);
        });
    }
    // todo: 2.4 进度条
    private void initProgressBar() {

    }

    // 3. 测速
    // 3.1 创建ws线程
    public void createWsService(String BPS, String guid, String testID) {
        wsService = new WsService(this, BPS, guid, testID);
    }
    // 3.2 测速
    public void testBandwidth() {
        setTestingUI();
        getAndShowNetworkInfo();
        controllerService = new ControllerService(this);
        String postUrl = "http://" + controllerService.getControllerIp() + ":" + controllerService.getControllerPort() + "/speedtest/new";
        try {
            JSONObject postJson = new JSONObject();
            postJson.put("network_type", network_info.getNetwork_type());
            postJson.put("GUID", androidID);
            postJson.put("client", "Android");  //todo: 获取其他操作系统类型
            controllerService.post(postUrl, postJson.toString(), new HttpCallback() {
                @Override
                public void onSuccess(JSONObject json) throws JSONException, InterruptedException {
                    BPS = json.get("BPS").toString();
                    testID = json.get("testID").toString();
                    Log.d("controller", "BPS: " + BPS);
                    Log.d("controller", "testID: " + testID);

                    //建立WebSocket连接并获取udp端口
                    if (wsService!=null && wsService.isAlive()) {
                        wsService.stopService();
                        wsService.join();
                    }
                    createWsService(BPS, androidID, testID);
                    wsService.start();
                }
                @Override
                public void onFailure(Exception e) {
                    // todo:错误处理
                    setNetworkIssueUI(1);
                }
            });
        } catch (JSONException e) {
            // todo:错误处理
            throw new RuntimeException(e);
        }
    }
    // 3.3 上传测速结果给Controller
    public void uploadResult() {
        String postUrl = "http://" + controllerService.getControllerIp() + ":" + controllerService.getControllerPort() + "/speedtest/record";
        try {
            JSONObject postJson = new JSONObject();
            // todo: 品牌信息需要用户同意
            Log.d("Result", Build.BRAND);
            postJson.put("brand", Build.BRAND);
            postJson.put("network_type", network_info.getNetwork_type());
            postJson.put("GUID", androidID);
            postJson.put("download", bandwidth);
            Log.d("Post", postJson.toString());
            controllerService.post(postUrl, postJson.toString(), new HttpCallback() {
                @Override
                public void onSuccess(JSONObject json) throws JSONException, InterruptedException {
                    Log.d("Controller", json.toString());
                }
                @Override
                public void onFailure(Exception e) {
                    Log.d("Controller", "上传测速结果失败");
                }
            });
        } catch (JSONException e) {
            // todo:错误处理
            throw new RuntimeException(e);
        }
    }

    // 4. 更新UI
    // 4.1 所有组件初始化UI
    public void initEverything() {
        // 图表
        int chartWidth = (int)(screenWidth * 0.9);
        int chartHeight = (int)(screenHeight * 0.2);
        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(chartWidth, chartHeight);
        chartFrame.setLayoutParams(chartParams);
        networkDetailView.setMaxWidth(chartWidth);
        initLineChart();
    }
    // 4.2 测速开始UI
    public void setTestStartUI() {
        ballFrame.setEnabled(false);
        chartValues.clear();
        chartValues.add(new Entry(0, 0));
        startText.startAnimation(fadeOut(400));
        startText.setVisibility(View.INVISIBLE);
        if (bandwidthText.getVisibility() == View.VISIBLE) {
            AlphaAnimation textFadeOut = fadeOut(400);
            textFadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) { }

                @Override
                public void onAnimationEnd(Animation animation) {
                    resetTestInfo();
                    moveTextsBack();
                }
                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            bandwidthLabelUp.startAnimation(fadeOut(400));
            bandwidthLabelUp.setVisibility(View.INVISIBLE);
            bandwidthLabelDown.startAnimation(textFadeOut);
            bandwidthLabelDown.setVisibility(View.INVISIBLE);
            bandwidthText.startAnimation(fadeOut(400));
            bandwidthText.setVisibility(View.INVISIBLE);
            testInfoView.startAnimation(fadeOut(400));
            testInfoView.setVisibility(View.INVISIBLE);
            chartFrame.startAnimation(fadeOut(400));
            chartFrame.setVisibility(View.INVISIBLE);

        }
        ballUpAndRotate();
    }
    // 4.3 更新测速结果UI
    public void updateBandwidthText(float bandwidth) {
        handler.post(() -> {
            String formattedBandwidth = String.format(Locale.getDefault(), "%.2f", bandwidth);
            if (bandwidth < 500) {
                bandwidthText.setText(formattedBandwidth);
            } else {
                bandwidthText.setText("500+");
            }
        });
    }
    // 4.4 更新测速中UI
    public void setTestingUI() {
        handler.post(() -> {
            //显示测速文本
            bandwidthLabelUp.setText("测速中");
            bandwidthText.startAnimation(fadeIn(400));
            bandwidthText.setVisibility(View.VISIBLE);
            bandwidthLabelUp.startAnimation(fadeIn(400));
            bandwidthLabelUp.setVisibility(View.VISIBLE);
            bandwidthLabelDown.startAnimation(fadeIn(400));
            bandwidthLabelDown.setVisibility(View.VISIBLE);
            chartFrame.startAnimation(fadeIn(400));
            chartFrame.setVisibility(View.VISIBLE);

            //转球
            ballRotate();
            ballRotation.start();
        });
    }
    // 4.5 更新测速结束UI
    public void setTestEndUI() {
        handler.post(() -> {
            ballRotation.cancel();
            ballAgain();
        });
    }
    // 4.6 更新测试成功结束UI
    public void setTestSuccessUI() {
        handler.post(() -> {
            uploadResult();
           showTestInfo();
           setTestEndUI();
        });
    }
    // 4.7 更新网络问题UI
    public void setNetworkIssueUI(int errorCode) {
        handler.post(() -> {
            bandwidthText.setText("0.00");
            networkDetailView.setText("");
            testDurationView.setText("");
            testTrafficView.setText("");
            if (errorCode == 1){
                networkDetailView.setText("网络未连接或信号差，请检查您的网络连接后重试");
            }
            else if (errorCode == 2){
                networkDetailView.setText("服务器开小差，请稍后重试");
            }
            else if (errorCode == 3) {
                networkDetailView.setText("网络信息请求失败，请在”设置“中授权我们获取您的电话信息和位置信息");
            }
            setTestEndUI();
        });
    }
}