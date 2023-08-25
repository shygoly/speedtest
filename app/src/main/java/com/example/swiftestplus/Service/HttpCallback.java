package com.example.swiftestplus.Service;

import org.json.JSONException;
import org.json.JSONObject;

public interface HttpCallback {
    void onSuccess(JSONObject json) throws JSONException, InterruptedException;
    void onFailure(Exception e);
}
