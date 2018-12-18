package com.example.fubuki.iloc;

import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;

import java.util.ArrayList;

public class IterationMethod {
    private ArrayList<GpsPoint> tmpGPSPointArr = new ArrayList<>();

    public void markGPSPoint(ArrayList<GpsPoint> gpsPointArray,double thre){
        //将新采的这个点 与 前面序列点中的任意两点组合
        this.tmpGPSPointArr.add(gpsPointArray.get(gpsPointArray.size()-1));
        for(int i=0;i<gpsPointArray.size()-2;i++){
            this.tmpGPSPointArr.add(gpsPointArray.get(i));
            for(int j=i+1;j<gpsPointArray.size()-1;j++){
                this.tmpGPSPointArr.add(gpsPointArray.get(j));
                Point tempBest = GpsNode.newtonIteration(this.tmpGPSPointArr);//牛顿迭代法
                LatLng p1 = new LatLng(tempBest.getY(),tempBest.getX());

                double totalDiff = 0;
                for(int t=0;t<3;t++){
                    LatLng p2 = new LatLng(this.tmpGPSPointArr.get(t).getLatitude(),this.tmpGPSPointArr.get(t).getLongitude());
                    totalDiff = totalDiff+Math.abs(AMapUtils.calculateLineDistance(p1,p2)-this.tmpGPSPointArr.get(t).getDistance());
                }
                if(totalDiff>thre){
                    //超过阈值时打坏点标记
                    for(int t=0;t<3;t++){
                        this.tmpGPSPointArr.get(t).addCount();
                        }
                }
                this.tmpGPSPointArr.remove((Object)gpsPointArray.get(j));
            }
            this.tmpGPSPointArr.remove((Object)gpsPointArray.get(i));
        }
        this.tmpGPSPointArr.remove((Object)gpsPointArray.get(gpsPointArray.size()-1));
    }

    //gps序列点增多后，重新找到最可靠的三个点
    public ArrayList<GpsPoint> newReliablePoint(ArrayList<GpsPoint> reliablePointArray,ArrayList<GpsPoint> gpsPointArray){
        double count = gpsPointArray.get(gpsPointArray.size()-1).getCount();
        GpsPoint reliableGPSPoint1,reliableGPSPoint2,reliableGPSPoint3;
        //经过新一轮的标记后，重新排序，保证升序
        if(reliablePointArray.get(1).getCount()<reliablePointArray.get(0).getCount()){
            reliableGPSPoint1 = reliablePointArray.get(1);
            reliableGPSPoint2 = reliablePointArray.get(0);
        }else{
            reliableGPSPoint1 = reliablePointArray.get(0);
            reliableGPSPoint2 = reliablePointArray.get(1);
        }
        if(reliablePointArray.get(2).getCount()<reliableGPSPoint1.getCount()){
            reliableGPSPoint3 = reliableGPSPoint2;
            reliableGPSPoint2 = reliableGPSPoint1;
            reliableGPSPoint1 = reliablePointArray.get(2);
        }else if(reliablePointArray.get(2).getCount()<reliableGPSPoint2.getCount()){
            reliableGPSPoint3 = reliableGPSPoint2;
            reliableGPSPoint2 = reliablePointArray.get(2);
        }else{
            reliableGPSPoint3 = reliablePointArray.get(2);
        }
        //加入最新点后排序
        if(count<reliableGPSPoint1.getCount()){
            reliableGPSPoint3 = reliableGPSPoint2;
            reliableGPSPoint2 = reliableGPSPoint1;
            reliableGPSPoint1 = gpsPointArray.get(gpsPointArray.size()-1);
        }else if(count<reliableGPSPoint2.getCount()){
            reliableGPSPoint3 = reliableGPSPoint2;
            reliableGPSPoint2 = gpsPointArray.get(gpsPointArray.size()-1);
        }else if(count<reliableGPSPoint3.getCount()){
            reliableGPSPoint3 = gpsPointArray.get(gpsPointArray.size()-1);
        }else if(count == reliableGPSPoint3.getCount()&&gpsPointArray.get(gpsPointArray.size()-1).getDistance()<reliableGPSPoint3.getDistance()){
            reliableGPSPoint3 = gpsPointArray.get(gpsPointArray.size()-1);
        }
        ArrayList<GpsPoint> reliableGPSPoint = new ArrayList<>();
        reliableGPSPoint.add(reliableGPSPoint1);
        reliableGPSPoint.add(reliableGPSPoint2);
        reliableGPSPoint.add(reliableGPSPoint3);
        return reliableGPSPoint;
    }
}
