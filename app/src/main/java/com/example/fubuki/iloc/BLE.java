package com.example.fubuki.iloc;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.example.fubuki.iloc.Constant.DISCONN_BLE;
import static com.example.fubuki.iloc.Constant.NEW_DISTANCE;
import static com.example.fubuki.iloc.Constant.TOO_FAR;
import static com.example.fubuki.iloc.Constant.TURN_REVERSE;
import static com.example.fubuki.iloc.Constant.UPDATE_STATUS;

public class BLE {
    //蓝牙
    public List<String> bluetoothDevices = new ArrayList<String>(); //保存搜索到的列表
    public ArrayAdapter<String> arrayAdapter; //ListView的适配器

    public BluetoothManager mBluetoothManager;
    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothLeScanner bluetoothLeScanner;

    public BluetoothGatt bluetoothGatt;
    //bluetoothDevice是dervices中选中的一项 bluetoothDevice=dervices.get(i);
    public BluetoothGattService bluetoothGattService;
    public BluetoothGattCharacteristic bluetoothGattCharacteristic;
    public BluetoothDevice bluetoothDevice;

    public List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();//存放扫描结果

    //使用的蓝牙的UUID
    public final String service_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public final String characteristic_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";

    //断开BLE连接
    public void disconnect_BLE(Handler handler){
        bluetoothGatt.disconnect();
        Message tempMsg = new Message();
        tempMsg.what = DISCONN_BLE;
        handler.sendMessage(tempMsg);
    }

}
