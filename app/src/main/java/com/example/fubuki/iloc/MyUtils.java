package com.example.fubuki.iloc;

import android.text.TextUtils;
import android.util.Log;

import java.util.List;

public class MyUtils {
    //判断蓝牙接收距离趋势
    public static boolean judgeTrend(List<Double> distanceArray){
        int currentNum = distanceArray.size();
        double d1 = distanceArray.get(currentNum - 1);
        double d2 = distanceArray.get(currentNum - 2);
        double d3 = distanceArray.get(currentNum - 3);
        double d4 = distanceArray.get(currentNum - 4);
        double d5 = distanceArray.get(currentNum - 5);

        //Log.e("distance judge","distance is:"+d1+"#"+d2+"#"+d3+"#"+d4);
        int ascendCount = 0;
        if(d1 - d2 > 0)
            ascendCount++;
        if(d2 - d3 > 0)
            ascendCount++;
        if(d3 - d4 > 0)
            ascendCount++;
        if(d4 - d5 > 0)
            ascendCount++;

        if(ascendCount > 2)
            return true;
        else
            return false;
    }

    //寻找盲走序列中距离最小的点
    public static GpsPoint searchMinPoint(GpsNode blindPointSet){
        int minIndex = 0;
        double minDis = Double.MAX_VALUE;
        for(int i = 0; i < blindPointSet.getNodeNumber(); i++){
            if(blindPointSet.getGpsPoint(i).getDistance() < minDis){
                minIndex = i;
                minDis = blindPointSet.getGpsPoint(i).getDistance();
            }
        }

        return blindPointSet.getGpsPoint(minIndex);
    }

    //string转float
    public static double convertToDouble(String number, float defaultValue) {
        if (TextUtils.isEmpty(number)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(number);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
