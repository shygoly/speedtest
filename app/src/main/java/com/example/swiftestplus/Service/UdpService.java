package com.example.swiftestplus.Service;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

public class UdpService extends Thread{
    private static final int MAX_LOOP = 3;
    private static final float SATURATED_THRESHOLD = 1.2f;
    private static final int MAX_TRIGGER_COUNT = 10;
    private WsService wsService;
    public boolean isRunning;
    private String udpUrl;
    private int udpPort;
    private DatagramSocket datagramSocket;
    private boolean trigger;
    private int triggerCount;
    private int sendSpeed;
    private int repeatCounter;
    private boolean receive;
    private long rcvBytesCount;
    private ArrayList<Long> rcvBytesRecord;
    private ArrayList<Float> rcvSpeedSamples;
    private Timer sampleTimer;
    private float rcvSpeed;
    public boolean getSingle;
    private boolean saturated;
    public ArrayList<Float> rcvSpeeds;
    public float downloadSpeed;

    public UdpService(WsService wsService, String udpUrl, int udpPort) {
        this.wsService = wsService;
        this.udpUrl = udpUrl;
        this.udpPort = udpPort;
        this.trigger = true;
        this.triggerCount = 0;
        this.repeatCounter = 0;
        this.receive = false;
        this.rcvBytesCount = 0;
        this.rcvBytesRecord = new ArrayList<>();
        this.rcvSpeedSamples = new ArrayList<>();
        this.rcvSpeeds = new ArrayList<>();
        this.getSingle = false;
    }

    public void run() {
        isRunning = true;
        try {
            datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(100);
            InetAddress bpsAddress = InetAddress.getByName(udpUrl);

            // 发trigger
            while (trigger) {
                triggerCount ++;
                // timeout
                if (triggerCount > MAX_TRIGGER_COUNT) {
                    wsService.activity.setNetworkIssueUI(2);
                    wsService.stopService();
                }
                byte[] triggerBuf = "trigger".getBytes();
                DatagramPacket triggerPacket = new DatagramPacket(triggerBuf, triggerBuf.length, bpsAddress, udpPort);
                datagramSocket.send(triggerPacket);
                Log.d("UDP", "Send: trigger");
                sleep(50);
            }

            while (repeatCounter < MAX_LOOP) {
                //等待开始接收
                while (!receive){
                    sleep(10);
                }

                Log.d("Test", "Test start, Loop: " + repeatCounter + ", Speed: " + sendSpeed);

                // 接收缓冲区
                byte[] rcvBuf = new byte[1024];
                DatagramPacket rcvPacket = new DatagramPacket(rcvBuf, rcvBuf.length);

                // 收包&计数
                repeatCounter ++;
                try {
                    datagramSocket.receive(rcvPacket);
                    Log.d("UDP", "Receive Packet: " + rcvPacket);
                    rcvBytesCount += rcvPacket.getLength();
                    //采样Timer
                    sampleTimer = new Timer();
                    sampleTimer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            rcvBytesRecord.add(rcvBytesCount);
                        }
                    }, 0, 10);
                    while (true) {
                        datagramSocket.receive(rcvPacket);
                        rcvBytesCount += rcvPacket.getLength();
                    }
                } catch (SocketTimeoutException e) {
                    // 本次收包正常结束
                    if (rcvBytesCount != 0) {
                        sampleTimer.cancel();
                        //计算本次测速结果
                        Log.d("Test", "Receive Bytes Record: " + rcvBytesRecord.toString());
                        for (int i=0; i<9; i++) {
                            rcvBytesRecord.remove(rcvBytesRecord.size()-1);
                        }
                        Log.d("Test", "Receive Bytes Record: " + rcvBytesRecord.toString());
                        float rcvSpeedSample;
                        for (int i=0; i<rcvBytesRecord.size(); i++) {
                            if (i<5) {
                                rcvSpeedSample = rcvBytesRecord.get(i) * 8.0f / 1024 / 1024 / ((i + 1) * 0.01f);
                            } else {
                                rcvSpeedSample = (rcvBytesRecord.get(i) - rcvBytesRecord.get(i - 5)) * 8.0f / 1024 / 1024 / (0.05f);
                            }
                            rcvSpeedSamples.add(rcvSpeedSample);
                        }
                        Log.d("Test", "Receive Speed Record: " + rcvSpeedSamples.toString());
                        Collections.sort(rcvSpeedSamples);
                        int percentileIndex = 95 * rcvSpeedSamples.size() / 100;
                        rcvSpeed = rcvSpeedSamples.get(percentileIndex);
                        Log.d("Test", "Download speed: " + rcvSpeed);

                        // 判断饱和
                        if (rcvSpeed * SATURATED_THRESHOLD >= sendSpeed) {
                            // 不饱和，重置
                            saturated = false;
                            repeatCounter = 0;
                            rcvSpeeds.clear();
                        } else {
                            // 饱和
                            saturated = true;
                            rcvSpeeds.add(rcvSpeed);
                        }
                        this.getSingle = true;
                    }

                    // todo:啥也没收到
                    else {
                        wsService.activity.setNetworkIssueUI(1);
                        wsService.stopService();
                    }
                }
                //重置单次测试数据
                rcvBytesCount = 0;
                rcvBytesRecord.clear();
                rcvSpeedSamples.clear();
                receive = false;
            }
            // 计算最终测试结果
            float sum = 0;
            for (float speed: rcvSpeeds){
                sum += speed;
            }
            downloadSpeed = sum / rcvSpeeds.size();
            Log.d("Test", "Final download bandwidth: " + downloadSpeed);

        } catch (SocketException e) {
            // todo: 一大堆错误处理
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isTestEnd() {
        return getSingle && (repeatCounter == MAX_LOOP);
    }

    public void stopService() {
        isRunning = false;
        if (datagramSocket!=null) {
            datagramSocket.close();
        }
        if (sampleTimer!=null) {
            sampleTimer.cancel();
        }
    }

    public void setTrigger(boolean trigger) {
        this.trigger = trigger;
    }

    public void setSendSpeed(int serverSpeed) {
        this.sendSpeed = serverSpeed;
    }

    public void setReceive(boolean receive) {
        this.receive = receive;
    }

    public int getRepeatCounter() {
        return repeatCounter;
    }

    public float getRcvSpeed() {
        return this.rcvSpeed;
    }

    public boolean isSaturated() {
        return saturated;
    }
}
