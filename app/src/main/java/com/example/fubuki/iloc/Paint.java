package com.example.fubuki.iloc;

import android.graphics.BitmapFactory;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;

public class Paint {
    public static void PaintNode(double nodeLatitude, double nodeLongitude, double rcvDis, AMap aMap){
        MarkerOptions markerOption = new MarkerOptions();
        markerOption.position(new LatLng(nodeLatitude,nodeLongitude));

        BitmapDescriptor bitmap;
        if(rcvDis > 5){
            bitmap = BitmapDescriptorFactory
                    .fromResource(R.drawable.map);

        }else{
            bitmap = BitmapDescriptorFactory
                    .fromResource(R.drawable.zhongdian);
        }
        markerOption.icon(bitmap);
        aMap.addMarker(markerOption);

        return;
    }
}
