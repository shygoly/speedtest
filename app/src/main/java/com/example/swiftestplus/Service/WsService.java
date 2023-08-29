package com.example.swiftestplus.Service;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.swiftestplus.MainActivity;
import com.github.mikephil.charting.data.Entry;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WsService extends Thread{
    public MainActivity activity;
    public boolean isRunning;
    private OkHttpClient wsClient;
    private WebSocket webSocket;
    private String wsUrl;
    private String wsPort = "8080";
    private String testID;
    private String guid;
    private String udpUrl;
    private int udpPort;
    private UdpService udpService;
    private ArrayList<Entry> chartValues;
    private long startTime;
    private long endTime;


    public WsService(MainActivity activity, String BPS, String guid, String testID){
        this.activity = activity;
        this.wsUrl = "ws://" + BPS + ":" + wsPort;
        this.guid = guid;
        this.testID = testID;
        this.udpUrl = BPS;
        this.startTime = 0;
    }

    @Override
    public void run() {
        isRunning = true;
        WsService wsService = this;
        wsClient = new OkHttpClient.Builder()
                .readTimeout(3, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(wsUrl)
                .build();

        webSocket = wsClient.newWebSocket(request, new WebSocketListener() {

            ScheduledFuture<?> receiveTimeoutTask;

            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.d("WebSocket", "Connected");
                JSONObject startIDs = new JSONObject();
                try {
                    startIDs.put("GUID", guid);
                    startIDs.put("testID", testID);
                } catch (JSONException e) {
                    // todo: 错误处理
                    throw new RuntimeException(e);
                }
                // 超时器
                ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
                ScheduledFuture<?> sendTimeoutTask = executorService.schedule(() -> {
                    Log.d("WebSocket", "Send timeout");
                    activity.setNetworkIssueUI(1);
                    stopService();
                }, 1, TimeUnit.SECONDS);
                boolean isSent = webSocket.send(startIDs.toString());
                if (isSent) {
                    sendTimeoutTask.cancel(false);
                    Log.d("WebSocket", "Send:" + startIDs.toString());
                    receiveTimeoutTask = executorService.schedule(() -> {
                        Log.d("WebSocket", "Receive timeout");
                        activity.setNetworkIssueUI(2);
                        stopService();
                    }, 1, TimeUnit.SECONDS);
                }
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                receiveTimeoutTask.cancel(false);
                Log.d("WebSocket", "Receive:" + text);
                try {
                    JSONObject responseJson = new JSONObject(text);
                    if (responseJson.has("udp_port")) {
                        Log.d("WebSocket", "trigger");
                        // 建立udp连接&发送trigger
                        startTime = 0;
                        udpPort = responseJson.getInt("udp_port");
                        udpService = new UdpService(wsService, udpUrl, udpPort);
                        udpService.start();
                    } else if (responseJson.has("msg")) {
                        if (responseJson.get("msg").equals("start")) {

                            // 停止发送trigger
                            udpService.setTrigger(false);

                            // 发送开始ack
                            JSONObject startAck = new JSONObject();
                            startAck.put("msg", "start");
                            webSocket.send(startAck.toString());

                            // 开始接收udp包
                            udpService.setSendSpeed(responseJson.getInt("speed"));
                            udpService.setReceive(true);
                            if (startTime == 0) {
                                startTime = System.currentTimeMillis();
                            }
                            while (!udpService.getSingle){
                                sleep(100);
                            }

                            activity.chartValues.add(new Entry((System.currentTimeMillis()-startTime)/1000.0f, udpService.getRcvSpeed()));
                            activity.updateBandwidthText(udpService.getRcvSpeed());

                            // 通知发送速度
                            if (udpService.isSaturated()) {
                                if (udpService.isTestEnd()) {
                                    // 测试结束
                                    endTime = System.currentTimeMillis();
                                    sleep(400);
                                    JSONObject endMsg = new JSONObject();
                                    endMsg.put("msg", "finish");
                                    endMsg.put("download", udpService.downloadSpeed);
                                    // 超时器
                                    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
                                    ScheduledFuture<?> sendTimeoutTask = executorService.schedule(() -> {
                                        Log.d("WebSocket", "Send timeout");
                                        activity.setNetworkIssueUI(1);
                                        stopService();
                                    }, 1, TimeUnit.SECONDS);
                                    boolean isSent = webSocket.send(endMsg.toString());
                                    if (isSent) {
                                        sendTimeoutTask.cancel(false);
                                        Log.d("WebSocket", "Send:" + endMsg.toString());
                                        activity.chartValues.add(new Entry((endTime-startTime)/1000.0f, udpService.downloadSpeed));
                                        activity.updateBandwidthText(udpService.downloadSpeed);
                                        receiveTimeoutTask = executorService.schedule(() -> {
                                            Log.d("WebSocket", "Receive timeout");
                                            activity.setNetworkIssueUI(1);
                                            stopService();
                                        }, 3, TimeUnit.SECONDS);
                                    }
                                } else {
                                    // 饱和，同速度再发
                                    JSONObject repeatMsg = new JSONObject();
                                    repeatMsg.put("msg", "repeat");
                                    // 超时器
                                    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
                                    ScheduledFuture<?> sendTimeoutTask = executorService.schedule(() -> {
                                        Log.d("WebSocket", "Send timeout");
                                        activity.setNetworkIssueUI(1);
                                        stopService();
                                    }, 1, TimeUnit.SECONDS);
                                    boolean isSent = webSocket.send(repeatMsg.toString());
                                    if (isSent) {
                                        sendTimeoutTask.cancel(false);
                                        Log.d("WebSocket", "Send:" + repeatMsg.toString());
                                        receiveTimeoutTask = executorService.schedule(() -> {
                                            Log.d("WebSocket", "Receive timeout");
                                            activity.setNetworkIssueUI(1);
                                            stopService();
                                        }, 3, TimeUnit.SECONDS);
                                    }
                                }
                            } else {
                                // 不饱和，加速
                                JSONObject raiseMsg = new JSONObject();
                                raiseMsg.put("msg", "continue");
                                // 超时器
                                ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
                                ScheduledFuture<?> sendTimeoutTask = executorService.schedule(() -> {
                                    Log.d("WebSocket", "Send timeout");
                                    activity.setNetworkIssueUI(1);
                                    stopService();
                                }, 1, TimeUnit.SECONDS);
                                boolean isSent = webSocket.send(raiseMsg.toString());
                                if (isSent) {
                                    sendTimeoutTask.cancel(false);
                                    Log.d("WebSocket", "Send:" + raiseMsg.toString());
                                    receiveTimeoutTask = executorService.schedule(() -> {
                                        Log.d("WebSocket", "Receive timeout");
                                        activity.setNetworkIssueUI(1);
                                        stopService();
                                    }, 3, TimeUnit.SECONDS);
                                }
                            }
                            udpService.getSingle = false;
                        } else if (responseJson.get("msg").equals("exceed")) {
                            // todo: 达到上限，测速结束
                            JSONObject endMsg = new JSONObject();
                            endMsg.put("msg", "finish");
                            endMsg.put("download", 500);
                            // 超时器
                            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
                            ScheduledFuture<?> sendTimeoutTask = executorService.schedule(() -> {
                                Log.d("WebSocket", "Send timeout");
                                activity.setNetworkIssueUI(1);
                                stopService();
                            }, 1, TimeUnit.SECONDS);
                            boolean isSent = webSocket.send(endMsg.toString());
                            if (isSent) {
                                sendTimeoutTask.cancel(false);
                                Log.d("WebSocket", "Send:" + endMsg.toString());
                                activity.updateBandwidthText(500);
                                receiveTimeoutTask = executorService.schedule(() -> {
                                    Log.d("WebSocket", "Receive timeout");
                                    activity.setNetworkIssueUI(2);
                                    stopService();
                                }, 3, TimeUnit.SECONDS);
                            }
                        }
                    } else if (responseJson.has("traffic")) {
                        activity.bandwidth = udpService.downloadSpeed;
                        activity.testTraffic = responseJson.getDouble("traffic") / 1024 / 1024;
                        activity.setTestSuccessUI();
                        stopService();
                    }
                } catch (JSONException e) {
                    // todo: 错误处理
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                activity.setNetworkIssueUI(2);
                Log.e("WebSocket", "Connection failed");
            }
        });
    }

    public void stopService() {
        isRunning = false;
        if (udpService!=null && udpService.isAlive()) {
            udpService.stopService();
        }
        try {
            udpService.join();
        } catch (InterruptedException e) {
            //todo: 错误处理
            e.printStackTrace();
        }
        if (webSocket!=null) {
            webSocket.close(1000, "WebSocket closed");
        }
    }
}
