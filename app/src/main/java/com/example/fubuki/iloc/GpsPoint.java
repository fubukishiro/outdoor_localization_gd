/*手机采样点的位置，包含坐标和采集到的距离*/
package com.example.fubuki.iloc;

public class GpsPoint {
    private double angle;       //采样点与北方向夹角，弧度值
    private double longitude;   //采样点经度
    private double latitude;    //采样点纬度
    private double distance;    //采样点采集的距离

    private double count=0;     //坏点标记

    private int number;

    public static final double k1 = 96029;
    public static final double k2 = 112000;

    GpsPoint(double pLongitude,double pLatitude,double pDistance, int pNumber)
    {
        latitude = pLatitude;
        longitude = pLongitude;
        distance = pDistance;
        number = pNumber;
    }

    public double getLongitude()
    {
        return longitude;
    }
    public double getLatitude()
    {
        return latitude;
    }
    public double getDistance(){return distance;}
    public void setDistance(double setDis){ this.distance = setDis; }

    public void addCount(){this.count++;}
    public double getCount(){return count;}

    public int getIndex(){ return this.number;}

}
