/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.senssun.bluetooth.tools.blutooth_pressure;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.relative.Information;
import com.senssun.bluetooth.tools.relative.WheelViewSelect05;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import widget.TosAdapterView;
import widget.WheelView;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class BluetoothPressureControlActivity extends Activity {
    private final static String TAG = BluetoothPressureControlActivity.class.getSimpleName();
    public static final byte RESERVED = 0x00;
    public static final byte GUIDE_CODE = (byte) 0xFF;
    public static final byte LENTH = 0x20;


    public static final byte CMD_ID_GET = 0x02;
    public static final byte KEY_GET_LIVE_DATA = 0x07;
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private SharedPreferences mySharedPreferences;
    private SharedPreferences.Editor myEditor;

    private TextView mConnectionState;
    private TextView mRssiField;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothPressureLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private boolean mChceked = false;

    private boolean isEdit = false;
    private Button mSend_interval, mStop_btn;
    private EditText mTem_edit;
    private int time_interval = 0;
    private ListView mShow_data_lv;
    private DataShowAdapter mShowAdapter;
    private ArrayList<Map<String, String>> datalist;
    private Map<String, String> map;
    private Date date;
    private SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss");
    private Timer TimerOne;
    private int count;
    private long current_time;

    private boolean isSending=false;

    public final static byte[] sendBuffer_zero = new byte[]{(byte) Integer.parseInt("A5", 16), (byte) Integer.parseInt("00", 16),
            (byte) Integer.parseInt("00", 16), (byte) Integer.parseInt("A1", 16), (byte) Integer.parseInt("A1", 16)
    };
    public final static byte[] sendBuffer_standard = new byte[]{(byte) Integer.parseInt("A5", 16), (byte) Integer.parseInt("00", 16),
            (byte) Integer.parseInt("00", 16), (byte) Integer.parseInt("B0", 16), (byte) Integer.parseInt("B0", 16)
    };

    public final static byte[] sendBuffer_tem = new byte[]{(byte) Integer.parseInt("A5", 16), (byte) Integer.parseInt("00", 16),
            (byte) Integer.parseInt("00", 16), (byte) Integer.parseInt("C0", 16), (byte) Integer.parseInt("C0", 16)
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothPressureLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothPressureLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothPressureLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                if (TimerOne != null) {
                    TimerOne.cancel();
                    TimerOne = null;
                }
                mBluetoothLeService.close();
                mConnected = false;
                mChceked = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
                SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
                if (mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
                    onBackPressed();
                }

            } else if (BluetoothPressureLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothPressureLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothPressureLeService.EXTRA_DATA));
            } else if (BluetoothPressureLeService.ACTION_DATA_RSSI.equals(action)) {
                displayRssi(intent.getStringExtra("Rssi"));
            }
        }
    };

    private void clearUI() {
//		mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_pressure_control);
        mySharedPreferences = getSharedPreferences("sp", Activity.MODE_PRIVATE);
        myEditor = mySharedPreferences.edit();
        current_time = System.currentTimeMillis();
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mRssiField = (TextView) findViewById(R.id.rssi_value);
        TimerOne = new Timer();
        mSend_interval = (Button) findViewById(R.id.send_interval);
        mStop_btn = (Button) findViewById(R.id.stop_btn);
        mTem_edit = (EditText) findViewById(R.id.tem_edit);
        datalist = new ArrayList<>();
        mShow_data_lv = (ListView) findViewById(R.id.data_from_device);
        mShowAdapter = new DataShowAdapter(datalist, this);
        mShow_data_lv.setAdapter(mShowAdapter);

        mShow_data_lv.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                Log.i("tttttttt", "onScrollStateChanged: 001自己动手？" + scrollState);
                //停止滑动
                if (scrollState == 0) {
                    current_time = System.currentTimeMillis();
                    //滑动到了底部
                    if (view.getLastVisiblePosition() == (view.getCount() - 1)) {
                        mShow_data_lv.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                    }
                } else {
                    if (view.getLastVisiblePosition() == (view.getCount() - 1)) {
                        mShow_data_lv.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                    }else {
                        mShow_data_lv.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_DISABLED);
                    }

                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (System.currentTimeMillis() - current_time >= 14000) {
                    mShow_data_lv.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                    Log.i("tttttttt", visibleItemCount + "onScrollStateChanged: 002自己动手？" + totalItemCount);
                }

            }
        });

        getActionBar().setTitle(R.string.menu_back);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothPressureLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        WheelView projectWheel = (WheelView) findViewById(R.id.time_interval);
        WheelViewSelect05.viewProjectNum(projectWheel, this);
        projectWheel.setOnItemSelectedListener(new TosAdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(TosAdapterView<?> parent, View view, int position, long id) {
                SharedPreferences.Editor editor = mySharedPreferences.edit();
                editor.putInt(Information.DB.TIME_INTERVAL, position);
                editor.commit();
                time_interval = position*100+100;
            }

            public void onNothingSelected(TosAdapterView<?> parent) {

            }
        });
        projectWheel.setSelection(time_interval);

        mSend_interval.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TimerOne==null){
                    TimerOne = new Timer();
                    isSending=false;
                }else {
                    if (!isSending){
                        count = 0;
                        TimerOne.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                if (mWriteCharacteristic != null) {
                                    isSending=true;
                                    if (mConnected) {
                                        //这是CZJK手环的写入
                                        mWriteCharacteristic.setValue(sendCmd(CMD_ID_GET, KEY_GET_LIVE_DATA, count));

                                        //声荣模块的写入
                                       // mWriteCharacteristic.setValue(sendCmd2(GUIDE_CODE, LENTH, count));
                                        count++;
                                        Log.i("hhhhhhhhhhh", "onClick: 我们当前的时间间隔是？" + time_interval);
                                        //这是鸡尾酒秤的写入
                                        // mWriteCharacteristic.setValue(sendBuffer_zero);
                                        mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                                    } else {
                                        isSending=false;
                                        if (TimerOne != null) {
                                            TimerOne.cancel();
                                            TimerOne=null;
                                        }
                                    }
                                }
                            }
                        }, 0, time_interval);
                    }

                }

            }
        });
        mStop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if (TimerOne!=null){
                   TimerOne.cancel();
                   TimerOne=null;
               }
               isSending=false;
                datalist.clear();
                mShowAdapter.notifyDataSetChanged();
            }
        });
    }
    private void displayData(String data) {
        if (data != null) {
            String[] strdata = data.split("-");
            Log.e("Notification", data);
            map = new HashMap<>();
            // calendar=Calendar.getInstance();
            date = new Date();
            String bb = sdf.format(date);
            map.put("date", bb + "  毫秒:" + date.getTime());
            map.put("data", data);
            datalist.add(map);
            mShowAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);

            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                if (TimerOne != null) {
                    TimerOne.cancel();
                    TimerOne = null;
                }
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayRssi(String Rssi) {
        mRssiField.setText(Rssi);
    }


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        for (BluetoothGattService gattService : gattServices) {

//            if (gattService.getUuid().toString().equals("0000ffb0-0000-1000-8000-00805f9b34fb")) {
//                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
//                for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
//                    if (characteristic.getUuid().toString().trim().equals("0000ffb2-0000-1000-8000-00805f9b34fb")) {
//                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
//                        mNotifyCharacteristic = characteristic;
//                    }
//                    if (characteristic.getUuid().toString().trim().equals("0000ffb2-0000-1000-8000-00805f9b34fb")) {
//                        mWriteCharacteristic = characteristic;
//                    }
//                }
//            }
//            //这是鸡尾酒秤的主服务和写入特征值
//            if (gattService.getUuid().toString().equals("0000fff0-0000-1000-8000-00805f9b34fb")) {
//                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
//                for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
//                    if (characteristic.getUuid().toString().trim().equals("0000fff1-0000-1000-8000-00805f9b34fb")) {
//                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
//                        mNotifyCharacteristic = characteristic;
//                    }
//                    if (characteristic.getUuid().toString().trim().equals("0000fff2-0000-1000-8000-00805f9b34fb")) {
//                        mWriteCharacteristic = characteristic;
//                    }
//                }
//            }
            //这是CZJK手环的写特征值
            if (gattService.getUuid().toString().equals("00000af0-0000-1000-8000-00805f9b34fb")) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
                    if (characteristic.getUuid().toString().trim().equals("00000af6-0000-1000-8000-00805f9b34fb")) {
                        mWriteCharacteristic = characteristic;
                    }
                    if (characteristic.getUuid().toString().trim().equals("00000af7-0000-1000-8000-00805f9b34fb")) {
                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                        mNotifyCharacteristic = characteristic;
                    }
                }
            }

//            /*这是声荣的模块的写特征值*/
//            if (gattService.getUuid().toString().equals("00001910-0000-1000-8000-00805f9b34fb")) {
//                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
//                for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
//                    if (characteristic.getUuid().toString().trim().equals("00002c11-0000-1000-8000-00805f9b34fb")) {
//                        mWriteCharacteristic = characteristic;
//                    }
//                    if (characteristic.getUuid().toString().trim().equals("00002c12-0000-1000-8000-00805f9b34fb")) {
//                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
//                        mNotifyCharacteristic = characteristic;
//                    }
//                }
//            }



        }
//		new Thread(new TimeThread()).start();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothPressureLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothPressureLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothPressureLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothPressureLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothPressureLeService.ACTION_DATA_RSSI);
        return intentFilter;
    }

    private byte[] sendCmd(byte cmdId, byte key, int count) {
        byte[] cmd = new byte[20];
        cmd[0] = cmdId;
        cmd[1] = key;
        for (int i = 2; i < 20; i++) {
            cmd[i] = RESERVED;
        }
        cmd[19] = (byte) (count & 0xff);
        return cmd;
    }
    private byte[] sendCmd2(byte cmdId, byte key, int count) {
        byte[] cmd = new byte[20];
        for (int i = 0; i <20 ; i++) {
            if (i<4){
                cmd[i]= (byte) 0xff;
            }else if (i==4){
                cmd[i]=key;
            }else if (i==5){
                cmd[i]=0x03;
            }else if (i==6){
                cmd[i]=0x01;
            }else if (i>6&&i<18){
                cmd[i]=RESERVED;
            }else if (i==18){
                cmd[i]= (byte) (count&0xff);
            }else if (i==19){
                for (int j = 4; j <20 ; j++) {
                    cmd[i]+=cmd[j];
                }
                Log.i("hhhhhhh", "sendCmd2: 校验和算出来是多少？"+cmd[19]);
            }
        }

        return cmd;
    }

}
