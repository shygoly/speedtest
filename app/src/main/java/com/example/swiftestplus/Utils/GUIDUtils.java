package com.example.swiftestplus.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.UUID;

public class GUIDUtils {

    private Context context;

    public GUIDUtils(Context context) {
        this.context = context;
    }

    private String generateGuid() {
        UUID uuid = UUID.randomUUID();
        String guid = uuid.toString();
        Log.d("GUID","GUID generated: " + guid);
        return guid;
    }

    private void saveGuid(String guid) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("TestPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("GUID", guid);
        editor.apply();
        Log.d("GUID","GUID saved: " + guid);
    }

    public String getGuid() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("TestPrefs", Context.MODE_PRIVATE);
        String guid = sharedPreferences.getString("GUID", null);
        if (guid == null) {
            Log.d("GUID", "Can't find GUID");
            guid = generateGuid();
            saveGuid(guid);
        } else {
            Log.d("GUID", "Find GUID:" + guid);
        }
        return guid;
    }
}
