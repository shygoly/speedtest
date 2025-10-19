package com.swiftest.app.utils;

import android.content.Context;
import android.util.DisplayMetrics;

public class UIUtils {
    public static float dpToPx(Context ctx, float dp) {
        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        return dp * metrics.density;
    }
}
