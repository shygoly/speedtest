package com.swiftest.app.utils;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class ChartUtils {
    public static void setupChart(LineChart chart) {
        Description d = new Description();
        d.setText("");
        chart.setDescription(d);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
    }

    public static void updateChart(LineChart chart, ArrayList<Entry> values) {
        LineDataSet set = new LineDataSet(values, "Speed");
        set.setDrawCircles(false);
        set.setDrawValues(false);
        LineData data = new LineData(set);
        chart.setData(data);
        chart.invalidate();
    }
}
