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

package com.senssun.bluetooth.tools.jdweight;

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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckedTextView;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.relative.Information;

import java.util.ArrayList;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class JdDeviceControlActivity extends Activity {
    private final static String TAG = JdDeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private CheckedTextView mCheckField;
    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mRssiField;
    private String mDeviceName;
    private String mDeviceAddress;
    private JdBluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private boolean mDess = true;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic, mWriteCharacteristic02, mWriteCharacteristic03;

    public final static byte[] sendBuffer = new byte[]{(byte) Integer.parseInt("A5", 16),
            (byte) Integer.parseInt("5A", 16), 0, 0, 0, 0, 0, 0, 0};//
    private boolean goinfo = false;
    private Handler mHander;

    //同步个人信息
    public final static byte[] sendBuffer02 = new byte[]{(byte) 0x00, (byte) 0xF1, (byte) 0x05, (byte) 0x01, (byte) 0x01,
            (byte) 0x28, (byte) 0x00, (byte) 0xAF};

    //同步时间
    public final static byte[] sendBuffer03 = new byte[]{(byte) 0x00, (byte) 0xF2, (byte) 0x06, (byte) 0x07, (byte) 0xE0,
            (byte) 0x0B, (byte) 0x15, (byte) 0x12, (byte) 0x33, (byte) 0x30};
    //同步设备信息
    public final static byte[] sendBuffer04 = new byte[]{(byte) 0x00, (byte) 0xF0, (byte) 0x07, (byte) 0x4B, (byte) 0x52,
            (byte) 0x4F, (byte) 0x51, (byte) 0x51, (byte) 0x46};

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            mBluetoothLeService = ((JdBluetoothLeService.LocalBinder) service).getService();
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

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            if (JdBluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);

                invalidateOptionsMenu();
            } else if (JdBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                //自己添加，连接失败后，就重新连接。
                mBluetoothLeService.connect(mDeviceAddress);
                //  clearUI();
                SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
                if (mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
                    onBackPressed();
                }
            } else if (JdBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (JdBluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(JdBluetoothLeService.EXTRA_DATA));
            } else if (JdBluetoothLeService.ACTION_DATA_RSSI.equals(action)) {
                displayRssi(intent.getStringExtra("Rssi"));
            }
        }
    };


    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jd_device_control);

        mHander = new Handler();
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mRssiField = (TextView) findViewById(R.id.rssi_value);
        mCheckField = (CheckedTextView) findViewById(R.id.check_value);

        getActionBar().setTitle(R.string.menu_back);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.button1).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                onBackPressed();
            }
        });

        Intent gattServiceIntent = new Intent(this, JdBluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        DessRssi();
        Dess();

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

        //  unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mGattUpdateReceiver);
        mDess = false;
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

    private void displayData(String data) {

        Log.i("zhangjing", "数据来了么？" + data);
        if (data != null) {
            String[] strdata = data.split("-");
            Log.e("Notification", data);
            if (strdata.length >= 8) {
                if (strdata[1].equals("04")) {
                    String tmpNum = strdata[9] + strdata[10];
                    int weight = Integer.valueOf(tmpNum, 16);
                    float weight_f = weight / 10f;

                    mDataField.setText(String.valueOf(weight_f) + "kg");
                    mCheckField.setChecked(true);

                    //以下是测完就断开链接，然后去重连，就可以获取第二次的数据了，但是连接速度经常很慢，体验很差。
                    //mBluetoothLeService.disconnect();
                    //mBluetoothLeService.connect(mDeviceAddress);


                    SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
                    if (mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
                        Intent intent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putString("BleSuc", mDeviceAddress);
                        intent.putExtras(bundle);
                        setResult(RESULT_OK, intent);
                        mHander.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, 2000);


                    }
                }
            }
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {

        if (gattServices == null) return;

        for (BluetoothGattService gattService : gattServices) {

            if (gattService.getUuid().toString().equals("d618d000-6000-1000-8000-000000000000")) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {

                    if (characteristic.getUuid().toString().trim().equals("d618d002-6000-1000-8000-000000000000")) {
                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                        mNotifyCharacteristic = characteristic;
                    }
                    if (characteristic.getUuid().toString().trim().equals("d618d001-6000-1000-8000-000000000000")) {
                        mWriteCharacteristic = characteristic;
                    }
                }
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(JdBluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(JdBluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(JdBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(JdBluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(JdBluetoothLeService.ACTION_DATA_RSSI);
        return intentFilter;
    }

    private void Dess() {
        new Thread() {
            public void run() {
                while (mDess) {
                    if (mConnected && mWriteCharacteristic != null) {
                        mWriteCharacteristic.setValue(sendBuffer);
                        mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void DessRssi() {
        new Thread() {

            public void run() {
                while (mDess) {
                    if (mConnected) {
                        mBluetoothLeService.getmBluetoothGatt().readRemoteRssi();
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }


}
