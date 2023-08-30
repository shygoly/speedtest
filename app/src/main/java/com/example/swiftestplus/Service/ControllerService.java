package com.example.swiftestplus.Service;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.swiftestplus.MainActivity;
import com.example.swiftestplus.Service.HttpCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.concurrent.TimeUnit;

import okhttp3.*;

public class ControllerService {
    private final MainActivity activity;
    private OkHttpClient httpClient;
    private String controllerIp = new String("1.15.30.244");
    private String controllerPort = new String("12345");

    public ControllerService(MainActivity activity) {
        this.activity = activity;
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .writeTimeout(3, TimeUnit.SECONDS)
                .build();
    }

    public void post(String url, String json, HttpCallback callback) {
        RequestBody body = RequestBody.create(
          json,
          MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // todo:网络请求失败
                e.printStackTrace();
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    String responseData = response.body().string();
                    Log.d("Controller", responseData);
                    JSONObject responseJson = new JSONObject(responseData);
                    callback.onSuccess(responseJson);
                } catch (JSONException e) {
                    // todo: JSON转换错误处理
                    e.printStackTrace();
                    activity.setNetworkIssueUI(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    activity.setNetworkIssueUI(2);
                } catch (ProtocolException e) {
                    // 协议错误
                    e.printStackTrace();
                    activity.setNetworkIssueUI(2);
                }
                return ;
            }
        });
    }

    public String getControllerIp() {
        return controllerIp;
    }

    public String getControllerPort() {
        return controllerPort;
    }
}

