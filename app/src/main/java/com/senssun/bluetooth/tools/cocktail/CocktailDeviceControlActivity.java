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

package com.senssun.bluetooth.tools.cocktail;

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
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.bleweight.BleWeightBluetoothLeService;
import com.senssun.bluetooth.tools.configfatweight.ConfigBluetoothLeService;
import com.senssun.bluetooth.tools.relative.Information;

import java.util.ArrayList;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class CocktailDeviceControlActivity extends Activity {
    private final static String TAG = CocktailDeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private SharedPreferences mySharedPreferences;
    private SharedPreferences.Editor myEditor;

    private CheckedTextView mCheckField;
    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mRssiField;
    private String mDeviceName;
    private String mDeviceAddress;
    private CocktailBluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private boolean mChceked = false;

    private boolean isEdit = false;
    private Button mZero_btn, mStandard_btn, mOffBack_btn, mTem_btn;
    private EditText mTem_edit;


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
            mBluetoothLeService = ((CocktailBluetoothLeService.LocalBinder) service).getService();
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
            if (ConfigBluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (ConfigBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mBluetoothLeService.close();
                mConnected = false;
                mCheckField.setChecked(false);
                mChceked = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
                SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
                if (mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
                    onBackPressed();
                }

            } else if (ConfigBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (ConfigBluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BleWeightBluetoothLeService.EXTRA_DATA));
            } else if (ConfigBluetoothLeService.ACTION_DATA_RSSI.equals(action)) {
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
        setContentView(R.layout.activity_cocktail_device_control);
        mySharedPreferences = getSharedPreferences("sp", Activity.MODE_PRIVATE);
        myEditor = mySharedPreferences.edit();

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);


        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mRssiField = (TextView) findViewById(R.id.rssi_value);
        mCheckField = (CheckedTextView) findViewById(R.id.check_value);

        mZero_btn = (Button) findViewById(R.id.button_zero);
        mStandard_btn = (Button) findViewById(R.id.button_calibration);
        mOffBack_btn = (Button) findViewById(R.id.button_off_return);
        mTem_btn = (Button) findViewById(R.id.tem_btn);
        mTem_edit = (EditText) findViewById(R.id.tem_edit);


        getActionBar().setTitle(R.string.menu_back);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, CocktailBluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


        mZero_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWriteCharacteristic != null) {
                    mWriteCharacteristic.setValue(sendBuffer_zero);
                    mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                }

            }
        });
        mStandard_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWriteCharacteristic != null) {
                    mWriteCharacteristic.setValue(sendBuffer_standard);
                    mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                }

            }
        });
        mOffBack_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothLeService.disconnect();
                onBackPressed();
            }
        });

        mTem_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int w=Integer.parseInt(String.valueOf(mTem_edit.getText()));
                String dest1=Integer.toHexString(w);
                sendBuffer_tem[2] = (byte) Integer.parseInt(dest1, 16);
                int x=Integer.parseInt(String.valueOf(mTem_edit.getText()));
                int y=x+192;
                String dest = Integer.toHexString(y);
                sendBuffer_tem[4]=(byte) Integer.parseInt(dest, 16);
                if (mWriteCharacteristic != null) {
                    mWriteCharacteristic.setValue(sendBuffer_tem);
                    mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);

                }
            }
        });


    }

    private void displayData(String data) {
        if (data != null) {
            String[] strdata = data.split("-");
            Log.e("Notification", data);
            if (strdata.length > 3) {
                if (strdata[3].equals("AA")) {
                    //正常数据
                    mChceked = true;
                    mCheckField.setChecked(true);
                    String tmpNum = strdata[1] + strdata[2];
                    int weight = Integer.valueOf(tmpNum, 16);
                    if (mySharedPreferences.getBoolean(Information.DB.AutoCheck, false)) {
                        if (weight > 0) {
                            Handler mhadler = new Handler();
                            mhadler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mDataField.setTextColor(Color.BLUE);
                                    Intent intent = new Intent();
                                    Bundle bundle = new Bundle();
                                    bundle.putString("CocktailSuc", mDeviceAddress);
                                    intent.putExtras(bundle);
                                    setResult(RESULT_OK, intent);
                                    finish();
                                }
                            }, 3000);
                        }
                    }
                    mDataField.setText(weight + "g");
                } else if (strdata[3].equals("AB")) {
                    //负数
                    String tmpNum = strdata[1] + strdata[2];
                    int weight = Integer.valueOf(tmpNum, 16);
                    mDataField.setText("-" + weight + "g");
                } else if (strdata[3].equals("B0")) {
                    //标定数据：
                    String tmpNum = strdata[1] + strdata[2];
                    int weight = Integer.valueOf(tmpNum, 16);
                    mDataField.setText("标定点数据：" + weight + "g");
                } else if (strdata[3].equals("B1")) {
                    mDataField.setText("标定完成");
                } else if (strdata[3].equals("C0")) {
                    //系统配置参数
                    mDataField.setText("系统配置参数    参数1：" + strdata[1] + "  参数2：" + strdata[2]);
                } else if (strdata[3].equals("A0")) {
                    //临时数据
                    String tmpNum = strdata[1] + strdata[2];
                    int weight = Integer.valueOf(tmpNum, 16);
                    mDataField.setText(weight + "g");
                    mChceked = false;
                } else if (strdata[3].equals("AF")) {
                    mDataField.setText("超载Error");

                }
            }
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

            if (gattService.getUuid().toString().equals("0000ffb0-0000-1000-8000-00805f9b34fb")) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
                    if (characteristic.getUuid().toString().trim().equals("0000ffb2-0000-1000-8000-00805f9b34fb")) {
                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                        mNotifyCharacteristic = characteristic;
                    }
                    if (characteristic.getUuid().toString().trim().equals("0000ffb2-0000-1000-8000-00805f9b34fb")) {
                        mWriteCharacteristic = characteristic;
                    }
                }
            }
            if (gattService.getUuid().toString().equals("0000fff0-0000-1000-8000-00805f9b34fb")) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
                    if (characteristic.getUuid().toString().trim().equals("0000fff1-0000-1000-8000-00805f9b34fb")) {
                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                        mNotifyCharacteristic = characteristic;
                    }
                    if (characteristic.getUuid().toString().trim().equals("0000fff2-0000-1000-8000-00805f9b34fb")) {
                        mWriteCharacteristic = characteristic;
                    }
                }
            }
        }
//		new Thread(new TimeThread()).start();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConfigBluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(ConfigBluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ConfigBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ConfigBluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ConfigBluetoothLeService.ACTION_DATA_RSSI);
        return intentFilter;
    }


}
