package com.example.blesample;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.slider.LabelFormatter;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Bundle;

public class ChildActivity extends AppCompatActivity {

    private LineChart lineChart;
    private List<String> xValues,yValues2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child);

        TextView nhiptim = findViewById(R.id.nhiptim);
        TextView spo2 = findViewById(R.id.spo2);
        TextView ketqua = findViewById(R.id.ketqua);
        lineChart = findViewById(R.id.chart);

        Description description = new Description();
        description.setText("Nhịp/s                                                              " +
                "                                                                                %");
        description.setPosition(850f, 15f);
        lineChart.setDescription(description);

        xValues = Arrays.asList("0h0m0s","","0h0m10s","","0h0m20s","","0h0m30s","","0h0m40s","","0h0m50s","");

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xValues));
        xAxis.setAxisMinimum(0);
        xAxis.setAxisMaximum(10);
        xAxis.setLabelCount(5);
        xAxis.setGranularity(1f);

        yValues2 = Arrays.asList("00","10","20","30","40","50","60","70","80","90","100");

        YAxis yAxis1 = lineChart.getAxisLeft();
        yAxis1.setAxisMinimum(50f);
        yAxis1.setAxisMaximum(150f);
        yAxis1.setAxisLineWidth(2f);
        yAxis1.setAxisLineColor(Color.RED);
        yAxis1.setLabelCount(10);

        YAxis yAxis2 = lineChart.getAxisRight();
        yAxis2.setAxisMinimum(50f);
        yAxis2.setAxisMaximum(100f);
        yAxis2.setAxisLineWidth(2f);
        yAxis2.setAxisLineColor(Color.BLUE);

        int[] data_heartrate = {93, 90, 83, 80, 85, 89, 78, 75, 80, 99, 100};
        List<Entry> entries1 = new ArrayList<>();
        for (int i = 0; i < data_heartrate.length; i++) {
            entries1.add(new Entry(i, data_heartrate[i]));
        }
        int[] arr_heart={calculateAverage(data_heartrate),findMaxValue(data_heartrate),findMinValue(data_heartrate)};
        String text_nhiptim = " Nhịp tim/giây"+
                "\n -Trung bình: " + arr_heart[0] +
                "\n -Cao nhất: " + arr_heart[1] +
                "\n -Thấp nhất: " + arr_heart[2];
        nhiptim.setText(text_nhiptim);

        int[] data_spo2 = {140, 145, 135, 148, 144, 144, 140, 130, 135, 145, 135};
        List<Entry> entries2 = new ArrayList<>();
        for (int i = 0; i < data_spo2.length; i++) {
            entries2.add(new Entry(i, data_spo2[i]));
        }
        int[] arr_spo2={calculateAverage(data_spo2)*100/150,findMaxValue(data_spo2)*100/150,findMinValue(data_spo2)*100/150};
        String text_spo2 = " Nồng độ Oxi (%)"+
                "\n -Trung bình: " + arr_spo2[0] +
                "\n -Cao nhất: " + arr_spo2[1] +
                "\n -Thấp nhất: " + arr_spo2[2];
        spo2.setText(text_spo2);

        String text_ketqua = " Tình trạng sức khỏe: ";
        if(arr_spo2[0]>=60 && arr_spo2[0]<=100)
        {
            text_ketqua=text_ketqua + "\n -Nhịp tim:Bình thường";
        }
        else if(arr_spo2[0]<=59)
        {
            text_ketqua=text_ketqua + "\n -Nhịp tim: Thấp";
        }
        else if(101>=arr_heart[0])
        {
            text_ketqua=text_ketqua + "\n -Nhịp tim: Cao";
        }

        if(arr_spo2[0]>=97 && arr_spo2[0]<=100)
        {
            text_ketqua=text_ketqua + "\n -SPO2:Oxy trong máu tốt";
        }
        else if(arr_spo2[0]>=94 && arr_spo2[0]<=96)
        {
            text_ketqua=text_ketqua + "\n -SPO2:Oxy trong máu trung bình";
        }
        else if(arr_spo2[0]>=90 && arr_spo2[0]<=93)
        {
            text_ketqua=text_ketqua + "\n -SPO2:Oxy trong máu thấp";
        }
        else
        {
            text_ketqua=text_ketqua + "\n -SPO2:Oxy trong máu rất thấp";
        }
        ketqua.setText(text_ketqua);

        LineDataSet dataSet1 = new LineDataSet(entries1, "Nhịp tim");
        dataSet1.setColor(Color.RED);
        dataSet1.setLineWidth(2f);

        LineDataSet dataSet2 = new LineDataSet(entries2, "SpO2");
        dataSet2.setColor(Color.BLUE);
        dataSet2.setLineWidth(2f);

        LineData lineData = new LineData(dataSet1, dataSet2);


        lineData.setDrawValues(false);

        lineChart.setData(lineData);

        lineChart.invalidate();
    }

    public static int calculateAverage(int[] data) {
        int sum = 0;
        for (int value : data) {
            sum += value;
        }
        return (int) Math.round((double) sum / data.length);
    }

    public static int findMaxValue(int[] data) {
        int max = data[0];
        for (int value : data) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }
    public static int findMinValue(int[] data) {
        int min = data[0];
        for (int value : data) {
            if (value < min) {
                min = value;
            }
        }
        return min;
    }
}