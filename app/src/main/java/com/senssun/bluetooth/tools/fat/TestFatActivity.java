package com.senssun.bluetooth.tools.fat;


import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.adapter.BluetoothDeviceAdapter;
import com.senssun.bluetooth.tools.application.MyApp;
import com.senssun.bluetooth.tools.adapter.DeviceAdapter;
import com.senssun.bluetooth.tools.entity.BleDevice;
import com.senssun.bluetooth.tools.entity.MessObject;
import com.senssun.bluetooth.tools.relative.Information;
import com.senssun.bluetooth.tools.relative.WheelViewSelect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import widget.TosAdapterView;
import widget.WheelView;

/**
 * 项目的主Activity，所有的Fragment都嵌入在这里。
 *
 * @author guolin
 */

public class TestFatActivity extends Activity implements OnClickListener {
    private int limitRssi;
    private ListView deviceList;
    private Button disconnectBtn;
    private BluetoothDeviceAdapter deviceAdapter;
    private List<MessObject> messList=new ArrayList<MessObject>();
    //蓝牙部分
    private MyHandler handler;
    private ConnStateHandler connStateHandler;
    private Handler mHandler;
    private boolean mScanning=false;//判断搜索状态
    private static final long SCAN_PERIOD =30000;// 10000;  扫描间隔
    private BluetoothManager mBluetoothManager=null;
    private BluetoothAdapter mBtAdapter = null; //蓝牙适配器
    private BluetoothGatt mBluetoothGatt = null;
    private String TAG = "BluetoothLeService";
    private String mBluetoothDeviceAddress;
    private int mConnectionState = STATE_DISCONNECTED;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    public final static byte[] sendBodyBuffer = new byte[]{(byte) Integer.parseInt("A5", 16),
            (byte) Integer.parseInt("10", 16), (byte) Integer.parseInt("01", 16),
            (byte) Integer.parseInt("12", 16), (byte) Integer.parseInt("A7", 16),
            (byte) Integer.parseInt("2", 16), (byte) Integer.parseInt("8F", 16),
            (byte) Integer.parseInt("5B", 16)};//

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.layout_jd_weight_noti);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);//
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        init();
    }
    private void init() {
        mHandler=new Handler();
        handler=new MyHandler();
        connStateHandler=new ConnStateHandler();
        deviceAdapter = new BluetoothDeviceAdapter(this);
        deviceList = (ListView) findViewById(R.id.deviceList);
        disconnectBtn = (Button) findViewById(R.id.disconnectBtn);
        deviceList.setAdapter(deviceAdapter);

        SharedPreferences mySharedPreferences= getSharedPreferences("sp", Context.MODE_PRIVATE);
        limitRssi=mySharedPreferences.getInt(Information.DB.LimitRssi,0);

        WheelView projectWheel =(WheelView) findViewById(R.id.projectWheel);
        WheelViewSelect.viewProjectNum(projectWheel, this);
        projectWheel.setOnItemSelectedListener(new TosAdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(TosAdapterView<?> parent, View view, int position, long id) {
                SharedPreferences mySharedPreferences= getSharedPreferences("sp", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = mySharedPreferences.edit();
                editor.putInt(Information.DB.LimitRssi, position+40);
                editor.commit();
                limitRssi=position+40;
            }
            public void onNothingSelected(TosAdapterView<?> parent) {

            }
        });
        projectWheel.setSelection(limitRssi-40);


        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBluetoothManager.getAdapter();

        scanLeDevice(true);
    }

/////////////////////////////////////////////////////////////////////////////////////////////////蓝牙控制部分///////////////////////////////////////////////////

    public void scanLeDevice(final boolean enable) {//搜索BLE设备
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(mScanning){
                        mBtAdapter.stopLeScan(mLeScanCallback);
                        scanLeDevice(mScanning);
                    }else{
                        mScanning = false;
                        mBtAdapter.stopLeScan(mLeScanCallback);
                    }

                }
            }, SCAN_PERIOD);
            if(!mScanning){
                mScanning = true;
                mBtAdapter.startLeScan(mLeScanCallback);
            }
        } else {
            mScanning = false;
            mBtAdapter.stopLeScan(mLeScanCallback);
        }
    }


    // Device scan callback. 设备扫描回调。
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (device.getName()==null)return;
                            if(limitRssi<Math.abs(rssi))return;
                            if ("BLE to UART_2-SENSSUN FAT".contains(device.getName())&&device.getAddress().equals("18:7A:93:02:03:5A")){
                                scanLeDevice(false);
                                connect(device.getAddress());
                            }
                        }
                    }).start();
                }
            };


    //根据Mac 地址链接蓝牙
    public boolean connect(String address){
        if (mBtAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                {
                    Message m = new Message();
                    Bundle b = new Bundle();
                    b.putInt("ConnState", mConnectionState);
                    m.setData(b);
                    connStateHandler.sendMessage(m);
                }
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        {
            Message m = new Message();
            Bundle b = new Bundle();
            b.putInt("ConnState", mConnectionState);
            m.setData(b);
            connStateHandler.sendMessage(m);
        }

        return true;
    }


    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

                {
                    Message m = new Message();
                    Bundle b=new Bundle();
                    b.putInt("ConnState", mConnectionState);
                    m.setData(b);
                    connStateHandler.sendMessage(m);}
                {
                    BleDevice ble=new BleDevice();
                    ble.setmDevice(gatt.getDevice());

                    Message m = new Message();
                    Bundle b=new Bundle();
                    b.putSerializable("BleDevice", ble);
                    m.setData(b);
                    handler.sendMessage(m);}

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                {
                    Message m = new Message();
                    Bundle b=new Bundle();
                    b.putInt("ConnState", mConnectionState);
                    m.setData(b);
                    connStateHandler.sendMessage(m);}

                scanLeDevice(true);
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mBluetoothGatt == null) return ;
                displayGattServices(mBluetoothGatt.getServices());
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final byte[] data = characteristic.getValue();
            String[] strdata = new String[data.length];
            if (data != null ) {
                for(int i=0;i<data.length;i++){
                    strdata[i]=String.format("%02X ", data[i]).trim();
                }
            }
            BleDevice ble=new BleDevice();

            if ("AA-A0-10-1A-20-2A-30-3A".contains(strdata[6])&&!"31-30-FF".contains(strdata[2])&&strdata[1].equals("A5")) {
                String tmpNum = strdata[2] + strdata[3];
                int weight = Integer.valueOf(tmpNum, 16);
                ble.setmNotiStr(String.valueOf(weight));
                mWriteCharacteristic.setValue(sendBodyBuffer);
                WriteCharacteristic(mWriteCharacteristic,mBluetoothGatt);
            }
            if ("10".equals(strdata[2])&&strdata[1].equals("A5")) {
                ble.setmSendSuc(true);

                close();
            }

            ble.setmDevice(gatt.getDevice());
            Message m = new Message();
            Bundle b=new Bundle();
            b.putSerializable("BleDevice", ble);
            m.setData(b);
            handler.sendMessage(m);

            mBluetoothGatt.readRemoteRssi();
        }

        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            BleDevice ble=new BleDevice();
            ble.setmRssi(rssi);
            ble.setmDevice(gatt.getDevice());
            Message m = new Message();
            Bundle b=new Bundle();
            b.putSerializable("BleDevice", ble);
            m.setData(b);
            handler.sendMessage(m);
        }
    };


    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        for (BluetoothGattService gattService : gattServices) {
            if (gattService.getUuid().toString().equals("0000ffb0-0000-1000-8000-00805f9b34fb")) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    if (gattCharacteristic.getUuid().toString().trim().equals("0000ffb2-0000-1000-8000-00805f9b34fb")){
                        setCharacteristicNotification(gattCharacteristic,true);
                    }
                    if (gattCharacteristic.getUuid().toString().trim().equals("0000ffb2-0000-1000-8000-00805f9b34fb")){
                        mWriteCharacteristic=gattCharacteristic;
                    }
                }
            }
            if (gattService.getUuid().toString().equals("0000fff0-0000-1000-8000-00805f9b34fb")) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    if (gattCharacteristic.getUuid().toString().trim().equals("0000fff1-0000-1000-8000-00805f9b34fb")){
                        setCharacteristicNotification(gattCharacteristic,true);
                    }
                    if (gattCharacteristic.getUuid().toString().trim().equals("0000fff2-0000-1000-8000-00805f9b34fb")){
                        mWriteCharacteristic=gattCharacteristic;
                    }
                }
            }
        }
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBtAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }
    public boolean WriteCharacteristic(BluetoothGattCharacteristic characteristic, BluetoothGatt mBluetoothGatt) {
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    class MyHandler extends  Handler{
        @Override
        public void handleMessage(Message msg) {
            Bundle b=msg.getData();
            BleDevice ble=(BleDevice)b.getSerializable("BleDevice");

            deviceAdapter.addDevice(ble);
            deviceAdapter.notifyDataSetChanged();
        }
    }

    class ConnStateHandler extends  Handler{
        @Override
        public void handleMessage(Message msg) {
            Bundle b=msg.getData();
            int ConnState=b.getInt("ConnState");

            if(ConnState==STATE_DISCONNECTED){
                disconnectBtn.setEnabled(false);
            }else{
                disconnectBtn.setEnabled(true);
            }
        }
    }
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        close();
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                onBackPressed();
                break;
            case R.id.disconnectBtn:
                disconnectBtn.setEnabled(false);
                close();
//                scanLeDevice(true);
                break;
        }
    }

}
