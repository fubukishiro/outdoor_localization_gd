package com.example.fubuki.iloc;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.example.fubuki.iloc.Constant.DISCONN_BLE;
import static com.example.fubuki.iloc.Constant.MOVE_FORWARD;
import static com.example.fubuki.iloc.Constant.NEW_DISTANCE;
import static com.example.fubuki.iloc.Constant.NEW_LOCATION;
import static com.example.fubuki.iloc.Constant.NEW_RSSI;
import static com.example.fubuki.iloc.Constant.NEW_SAMPLE;
import static com.example.fubuki.iloc.Constant.NO_POINT;
import static com.example.fubuki.iloc.Constant.SHOW_POINT;
import static com.example.fubuki.iloc.Constant.TIMER_LOCATION;
import static com.example.fubuki.iloc.Constant.TOO_FAR;
import static com.example.fubuki.iloc.Constant.TURN_AROUND;
import static com.example.fubuki.iloc.Constant.TURN_REVERSE;
import static com.example.fubuki.iloc.Constant.UPDATE_LIST;
import static com.example.fubuki.iloc.Constant.UPDATE_STATUS;
import static com.example.fubuki.iloc.MyUtils.convertToDouble;
import static com.example.fubuki.iloc.MyUtils.searchMinPoint;
import static com.example.fubuki.iloc.Paint.PaintNode;
import static java.lang.Double.isNaN;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,AMap.OnMyLocationChangeListener {

    private MapView mMapView = null;
    private AMap aMap = null;

    private static final String TAG = "Main Activity";

    private double rcvDis; //从终端接收回来的距离
    private double rssi; //从终端接收回来的rssi

    private ArrayAdapter<String> arrayAdapter; //ListView的适配器

    private BLE mBLE;

    private AlertDialog.Builder builder;
    private AlertDialog alertDialog;

    private static double validDistance = 40.0;

    private double currentLatitude,currentLongitude; //存储当前的经纬度
    private double lastNodeLatitude = 0.0,lastNodeLongitude = 0.0; //存储上一次计算出的节点的位置
    private double prevSampleLatitude = 0.0, prevSampleLongitude = 0.0; //存储上一次采样的节点的位置

    private List<Double> distanceArray = new ArrayList<Double>();//存放接收的距离序列

    private GpsNode gpsPointSet;
    private GpsNode blindSearchGpsPointSet; //盲走阶段的GPS序列

    private boolean isFinal = false;
    private double minArrayDis;
    private static boolean isReverse = false;
    private static int delayCount = 0;

    private boolean isFirstDistance = true;
    private boolean isFirstOpen = true;

    private final Timer locationTimer = new Timer();
    private TimerTask locationTask;

    //线程相关
    private class Token {
        private boolean flag;
        public Token() {
            setFlag(false);
        }
        public void setFlag(boolean flag) {
            this.flag = flag;
        }
        public boolean getFlag() {
            return flag;
        }
    }
    private Token token = null;
    private boolean toggleFlag = false;

    //声明AMapLocationClient类对象
    private AMapLocationClient mLocationClient = null;
    //声明定位回调监听器
    private AMapLocationListener mLocationListener;

    private AMapLocationClientOption mLocationOption =  new AMapLocationClientOption();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.gd_map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);

        initMap();
        initView();

        mBLE = new BLE();

        //蓝牙相关
        // 检查手机是否支持BLE，不支持则退出
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "您的设备不支持蓝牙BLE，将关闭", Toast.LENGTH_SHORT).show();
            finish();
        }

        mBLE.mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBLE.mBluetoothAdapter = mBLE.mBluetoothManager.getAdapter();
        if (!mBLE.mBluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);  // 弹对话框的形式提示用户开启蓝牙
        }

        gpsPointSet = new GpsNode();   //计算用的gps点序列
        blindSearchGpsPointSet = new GpsNode();  //盲走阶段的gps点序列

        //定时检测距离
        locationTask = new TimerTask() {
            @Override
            public void run() {

                if(!isFirstDistance) {
                    checkDistance();
                }
            }
        };

        locationTimer.schedule(locationTask, 1000, 2000);

        minArrayDis = Double.MAX_VALUE;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }

    private void initMap(){
        if (aMap == null) {
            aMap = mMapView.getMap();
        }
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        MyLocationStyle myLocationStyle;
        myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.interval(1000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        //myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
        myLocationStyle.radiusFillColor(Color.TRANSPARENT);
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        //aMap.getUiSettings().setMyLocationButtonEnabled(true);设置默认定位按钮是否显示，非必需设置。
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。

        aMap.moveCamera(CameraUpdateFactory.zoomTo(18));

        aMap.setOnMyLocationChangeListener(this);

        //aMap.setLocationSource(this);
        //初始化定位
       /* mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);

        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(1000);
        //设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为1000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
        //启动定位
        mLocationClient.startLocation();

        mLocationListener = new AMapLocationListener(){
            @Override
            public void onLocationChanged(AMapLocation amapLocation) {
                if (amapLocation != null) {
                    if (amapLocation.getErrorCode() == 0) {
                        //定位成功回调信息，设置相关消息
                        amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                        amapLocation.getLatitude();//获取纬度
                        amapLocation.getLongitude();//获取经度
                        amapLocation.getAccuracy();//获取精度信息
                        Log.e("amap", "onMyLocationChange 定位成功， lat: " + amapLocation.getLatitude() + " lon: " + amapLocation.getLongitude());
                    } else {
                        //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                        Log.e("AmapError","location Error, ErrCode:"
                                + amapLocation.getErrorCode() + ", errInfo:"
                                + amapLocation.getErrorInfo());
                    }
                }
            }
        };*/
    }

    private void initView(){

        View.OnClickListener listener = new View.OnClickListener() {
            public void onClick(View v) {
                int id = v.getId();
                switch (id) {
                    case R.id.searchBtn: //蓝牙
                        if(mBLE.bluetoothGatt == null){
                            actionAlertDialog();
                        }else{
                            mBLE.disconnect_BLE(handler);
                            mBLE.bluetoothGatt = null;
                        }
                        break;
                    case R.id.setValidDistance:
                        EditText msg = findViewById(R.id.validDis);
                        String tmpStr = msg.getText().toString();
                        validDistance = convertToDouble(tmpStr,0);
                        break;
                    default:
                        break;
                }
            }
        };

        //设定安全距离的按钮
        Button setDistanceBtn = findViewById(R.id.setValidDistance);
        setDistanceBtn.setOnClickListener(listener);

        //搜索蓝牙的按钮
        Button searchBLEBtn = findViewById(R.id.searchBtn);
        searchBLEBtn.setOnClickListener(listener);
    }

    public void onMyLocationChange(Location location) {

        // 定位回调监听
        if(location != null) {
            Log.e("amap", "onMyLocationChange 定位成功， lat: " + location.getLatitude() + " lon: " + location.getLongitude());
            Bundle bundle = location.getExtras();
            if(bundle != null) {
                int errorCode = bundle.getInt(MyLocationStyle.ERROR_CODE);
                String errorInfo = bundle.getString(MyLocationStyle.ERROR_INFO);
                // 定位类型，可能为GPS WIFI等，具体可以参考官网的定位SDK介绍
                int locationType = bundle.getInt(MyLocationStyle.LOCATION_TYPE);

                /*
                errorCode
                errorInfo
                locationType
                */
                Log.e("amap", "定位信息， code: " + errorCode + " errorInfo: " + errorInfo + " locationType: " + locationType );
            } else {
                Log.e("amap", "定位信息， bundle is null ");

            }

        } else {
            Log.e("amap", "定位失败");
        }
        // map view 销毁后不在处理新接收的位置
        if (location == null || mMapView == null) {
            return;
        }

        //获取并显示
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        //Log.e(TAG,"接收到位置："+currentLatitude+"#"+currentLongitude);
        Message tempMsg = new Message();
        tempMsg.what = NEW_LOCATION;
        handler.sendMessage(tempMsg);
        if(lastNodeLatitude > 0.0 && lastNodeLongitude > 0.0)
            //画出节点
            PaintNode(lastNodeLatitude,lastNodeLongitude, rcvDis, aMap);

        if(isFirstOpen){
            isFirstOpen = false;
            CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(new LatLng(currentLatitude, currentLongitude), 18);
            aMap.moveCamera(cu);
        }

    }

    public Handler handler = new Handler(){

        public void handleMessage(Message msg){
            Button searchBtn = findViewById(R.id.searchBtn);
            switch (msg.what){
                case UPDATE_STATUS:
                    searchBtn.setBackgroundResource(R.drawable.bluetooth);
                    break;
                case DISCONN_BLE:
                    searchBtn.setBackgroundResource(R.drawable.bluetooth_red);
                    break;
                case UPDATE_LIST:
                    arrayAdapter.notifyDataSetChanged();
                    break;
                case TIMER_LOCATION:
                    //getSecondLocation();
                    break;
                case NEW_DISTANCE:
                    TextView distanceText = findViewById(R.id.distance);
                    distanceText.setText("接收距离:"+rcvDis);
                    if(rcvDis < 10.0){
                        Toast.makeText(MainActivity.this,"到达节点附近，请四处张望，寻找节点",Toast.LENGTH_LONG).show();
                    }
                    break;
                case MOVE_FORWARD:
                    Toast.makeText(MainActivity.this,"请四处走动，寻找信号强度强的地方",Toast.LENGTH_LONG).show();
                    break;
                case NEW_SAMPLE:
                    Toast.makeText(MainActivity.this,"请走到下一个采样点",Toast.LENGTH_LONG).show();
                    break;
                case TURN_AROUND:
                    Toast.makeText(MainActivity.this,"请向左拐或右拐",Toast.LENGTH_LONG).show();
                    break;
                case TURN_REVERSE:
                    Toast.makeText(MainActivity.this,"正在逐渐偏离目标，请往回走",Toast.LENGTH_SHORT).show();
                    break;
                case NO_POINT:
                    Toast.makeText(MainActivity.this,"暂时算不出节点，请左拐或右拐",Toast.LENGTH_SHORT).show();
                    break;
                case SHOW_POINT:
                    //TextView pointNumberText = findViewById(R.id.samplePoint);
                    //pointNumberText.setText("采样点的个数:"+gpsPointSet.getNodeNumber());
                    break;
                case TOO_FAR:
                    GpsPoint minBlindPoint = searchMinPoint(blindSearchGpsPointSet);
                    LatLng minPoint = new LatLng(minBlindPoint.getLatitude(),minBlindPoint.getLongitude());
                    //小于有效距离认定可靠，添加
                    if(minBlindPoint.getDistance() < validDistance){
                        gpsPointSet.addGpsPoint(minBlindPoint);
                    }

                    if(minBlindPoint.getDistance() > 70.0){
                        Toast.makeText(MainActivity.this,"请在地图上寻找有信号的位置再重新寻找",Toast.LENGTH_SHORT).show();
                    }else{
                        aMap.clear();
                        BitmapDescriptor bitmap = BitmapDescriptorFactory
                                .fromResource(R.drawable.hint_point);

                        MarkerOptions markerOption = new MarkerOptions();
                        markerOption.position(minPoint);

                        markerOption.icon(bitmap);
                        aMap.addMarker(markerOption);

                        Toast.makeText(MainActivity.this,"请回到提示点，换一个方向走",Toast.LENGTH_SHORT).show();
                    }
                    break;
                case NEW_RSSI:
                    TextView rssiText = findViewById(R.id.rssi);
                    rssiText.setText("接收到的rssi:"+rssi);
                    break;
                case NEW_LOCATION:
                    TextView locationText = findViewById(R.id.location);
                    locationText.setText("当前位置:"+currentLatitude+"#"+currentLongitude);
                    break;
                default:
                    break;
            }
        }
    };

    //蓝牙选择的窗口弹出
    private void actionAlertDialog(){
        View bottomView = View.inflate(MainActivity.this,R.layout.ble_devices,null);//填充ListView布局
        ListView lvDevices = (ListView) bottomView.findViewById(R.id.device_list);//初始化ListView控件
        arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1,
                mBLE.bluetoothDevices);
        lvDevices.setAdapter(arrayAdapter);
        lvDevices.setOnItemClickListener(this);

        builder= new AlertDialog.Builder(MainActivity.this)
                .setTitle("蓝牙列表").setView(bottomView);//在这里把写好的这个listview的布局加载dialog中
        alertDialog = builder.create();
        alertDialog.show();

        mBLE.bluetoothLeScanner = mBLE.mBluetoothAdapter.getBluetoothLeScanner();
        mBLE.bluetoothLeScanner.startScan(scanCallback);//android5.0把扫描方法单独弄成一个对象了（alt+enter添加），扫描结果储存在devices数组中。最好在startScan()前调用stopScan()。

        handler.postDelayed(runnable, 10000);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult results) {
            super.onScanResult(callbackType, results);
            BluetoothDevice device = results.getDevice();
            if (!mBLE.devices.contains(device)) {  //判断是否已经添加
                mBLE.devices.add(device);

                mBLE.bluetoothDevices.add(device.getName() + ":"
                        + device.getAddress() + "\n");
                //更新字符串数组适配器，显示到listview中
                Message tempMsg = new Message();
                tempMsg.what = UPDATE_LIST;
                handler.sendMessage(tempMsg);
            }
        }
    };

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mBLE.bluetoothDevice = mBLE.devices.get(position);
        mBLE.bluetoothGatt = mBLE.bluetoothDevice.connectGatt(MainActivity.this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    alertDialog.dismiss();

                    Message tempMsg = new Message();
                    tempMsg.what = UPDATE_STATUS;
                    handler.sendMessage(tempMsg);

                    try {
                        Thread.sleep(600);
                        Log.i(TAG, "Attempting to start service discovery:"
                                + gatt.discoverServices());
                    } catch (InterruptedException e) {
                        Log.i(TAG, "Fail to start service discovery:");
                        e.printStackTrace();
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                }
                return;
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, final int status) {
                //此函数用于接收蓝牙传来的数据
                super.onServicesDiscovered(gatt, status);

                mBLE.bluetoothGattService = mBLE.bluetoothGatt.getService(UUID.fromString(mBLE.service_UUID));
                mBLE.bluetoothGattCharacteristic = mBLE.bluetoothGattService.getCharacteristic(UUID.fromString(mBLE.characteristic_UUID));

                if (mBLE.bluetoothGattCharacteristic != null) {
                    gatt.setCharacteristicNotification(mBLE.bluetoothGattCharacteristic, true); //用于接收数据
                    for (BluetoothGattDescriptor dp : mBLE.bluetoothGattCharacteristic.getDescriptors()) {
                        if (dp != null) {
                            if ((mBLE.bluetoothGattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            } else if ((mBLE.bluetoothGattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                                dp.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            }
                            gatt.writeDescriptor(dp);
                        }
                    }
                    Log.d(TAG, "服务连接成功");
                } else {
                    Log.d(TAG, "服务失败");
                    return;
                }
                return;
            }

            //接收到的蓝牙数据在这里做处理
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
                super.onCharacteristicChanged(gatt,characteristic);
                //发现服务后的响应函数
                byte[] bytesReceive = characteristic.getValue();
                String msgStr = new String(bytesReceive);
                Log.e(TAG,"接收到包："+msgStr);
                //拆分数据格式
                String[] strs = msgStr.split("#");
                //Log.e(TAG,"接收到包："+strs[0]);
                boolean isDis = false;
                switch(strs[0]){
                    case "dis":
                        rcvDis = convertToDouble(strs[1],0);
                        //Log.e(TAG,"接收到距离："+rcvDis);
                        isDis = true;
                        break;
                    case "rssi":
                        rssi = (int)convertToDouble(strs[1],0);
                        //Log.e(TAG,"接收到rssi："+ rssi);
                        break;
                    default:
                        System.out.println("unknown");
                        break;
                }
                //rcvDis = convertToDouble(strs[1],0);
                //Log.e(TAG,"蓝牙接收到的距离："+rcvDis);

                //距离序列大于100时清空
                if(isDis){
                    if(distanceArray.size() > 100){
                        distanceArray.clear();
                    }

                    if(rcvDis > 0)
                        distanceArray.add(rcvDis);

                    //盲走序列采样
                    blindSearchGpsPointSet.addGpsPoint(new GpsPoint(currentLongitude,currentLatitude,rcvDis, gpsPointSet.getNodeNumber()));

                    //当接收到的蓝牙距离过大时，提示用户回到一个起始点
                    if(rcvDis > 70){
                        Message tempMsg = new Message();
                        tempMsg.what = TOO_FAR;
                        handler.sendMessage(tempMsg);
                    }

                    if(rcvDis < minArrayDis){
                        minArrayDis = rcvDis;
                        //validDistance = minArrayDis;
                    }
                    //判断蓝牙距离的趋势，若逐渐远离则提示用户往回走
                    if(isReverse){
                        delayCount ++;
                        if(delayCount > 6) {
                            isReverse = false;
                            delayCount = 0;
                        }
                    }else if(distanceArray.size()>5){
                        if(MyUtils.judgeTrend(distanceArray)){
                            Message tempMsg = new Message();
                            tempMsg.what = TURN_REVERSE;
                            handler.sendMessage(tempMsg);
                            isReverse = true;
                        }
                    }

                    Message tempMsg = new Message();
                    tempMsg.what = NEW_DISTANCE;
                    handler.sendMessage(tempMsg);
                    toggleFlag = !toggleFlag;

                    if(token.getFlag()) {
                        synchronized (token) {
                            token.setFlag(false);
                            token.notifyAll();
                            Log.e(TAG,"线程重新启动");
                        }
                    }

                    if(isFirstDistance){
                        isFirstDistance = false;

                        prevSampleLatitude = currentLatitude;
                        prevSampleLongitude = currentLongitude;

                        Message tempMsg2 = new Message();
                        tempMsg2.what = MOVE_FORWARD;
                        handler.sendMessage(tempMsg2);
                    }
                }else{
                    Message tempMsg = new Message();
                    tempMsg.what = NEW_RSSI;
                    handler.sendMessage(tempMsg);
                }
                isDis = false;
                return;
            }
        });
    }
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mBLE.bluetoothLeScanner.stopScan(scanCallback);
        }
    };

    //检查距离，判断是否采样
    private void checkDistance(){
        LatLng p1 = new LatLng(currentLatitude,currentLongitude);
        LatLng p2 = new LatLng(lastNodeLatitude,lastNodeLongitude);
        LatLng p3 = new LatLng(prevSampleLatitude,prevSampleLongitude);

        //当 当前位置距离计算出的可能节点位置小于10米 或者 距离上次采样点大于5米（已走五米）时，进行新一次的采样
        if((AMapUtils.calculateLineDistance(p1,p2) < 10 || AMapUtils.calculateLineDistance(p1,p3) > 5) && prevSampleLatitude>0 && prevSampleLongitude>0){
            //等待最新的蓝牙发送过来的距离
            if(mBLE.bluetoothGattCharacteristic != null) {
                synchronized (token) {
                    try {
                        token.setFlag(true);
                        Log.e(TAG, "线程挂起");
                        token.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            //当接收距离小于30米，才把当前GPS添加进去
            if(rcvDis < validDistance && rcvDis > 0) {
                GpsPoint currentGpsPoint = new GpsPoint(currentLongitude, currentLatitude, rcvDis, gpsPointSet.getNodeNumber());

                gpsPointSet.addGpsPoint(currentGpsPoint);

                //当计算用的采样gps序列中的点数大于2时，计算位置
                if (gpsPointSet.getNodeNumber() > 2) {

                    //TODO:计算节点位置的函数接口
                    Point nodePosition = gpsPointSet.getNodePosition();
                    Log.e(TAG,"计算出节点："+ nodePosition.getY()+"#"+nodePosition.getX());
                    if (isNaN(nodePosition.getY()) || isNaN(nodePosition.getX()) || nodePosition.getY() == 0.0 || nodePosition.getX() == 0.0) {
                        //计算出的点是NaN或者0的时候，不更新位置
                        return;
                    } else {
                        lastNodeLatitude = nodePosition.getY();
                        lastNodeLongitude = nodePosition.getX();
                    }
                    prevSampleLatitude = currentLatitude;
                    prevSampleLongitude = currentLongitude;

                    aMap.clear();

                    PaintNode(nodePosition.getY(),nodePosition.getX(), rcvDis, aMap);

                    if(rcvDis > 5)
                        isFinal = true;

                    if(!isFinal){
                        Message tempMsg = new Message();
                        tempMsg.what = SHOW_POINT;
                        handler.sendMessage(tempMsg);
                    }
                } else {}
            }
        }
    }
}
