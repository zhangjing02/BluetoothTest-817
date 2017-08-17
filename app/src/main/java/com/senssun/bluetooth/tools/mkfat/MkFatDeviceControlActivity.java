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

package com.senssun.bluetooth.tools.mkfat;

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
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.infantscale.InfantBluetoothLeService;
import com.senssun.bluetooth.tools.relative.Information;

import java.util.ArrayList;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class MkFatDeviceControlActivity extends Activity {
    private final static String TAG = MkFatDeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private Context mContext;
    private CheckedTextView mCheckField;
    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mRssiField;
    private String mDeviceName;
    private String mDeviceAddress;
    private MkFatBluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private boolean mChceked = false;

    //默认165cm 25year female
    public final static byte[] sendBuffer = new byte[]{(byte) 0xa5, (byte) 0x10, (byte) 0x01, (byte) 0x19, (byte) 0xa5,
            0, 0, (byte) 0x74};//
    //清除历史
    public final byte[] ClearDataBuffer = new byte[]{(byte) 0xa5, 0x5a, 0, 0, 0, 0, 0, 0x5a};


    private EditText etHeight, etAge, etUserNum;
    private RadioButton rbMale, rbFemale;
    private Button btnEdit, sendUserInfo;
    private boolean isEditAble = false;
    private SharedPreferences mySharedPreferences;
    private Handler mHandler;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((MkFatBluetoothLeService.LocalBinder) service).getService();
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
            if (InfantBluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (InfantBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
                mBluetoothLeService.connect(mDeviceAddress);
                SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
                if (mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
                    onBackPressed();
                }
            } else if (InfantBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (InfantBluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(MkFatBluetoothLeService.EXTRA_DATA));
            } else if (InfantBluetoothLeService.ACTION_DATA_RSSI.equals(action)) {
                displayRssi(intent.getStringExtra("Rssi"));
            }
        }
    };


    private void clearUI() {
        mDataField.setText(R.string.no_data);
        mDataField.setTextColor(Color.BLACK);
        ((TextView) findViewById(R.id.mTv1)).setText("");
        ((TextView) findViewById(R.id.mTv2)).setText("");
        ((TextView) findViewById(R.id.mTv3)).setText("");
        ((TextView) findViewById(R.id.mTv4)).setText("");
        ((TextView) findViewById(R.id.mTv5)).setText("");
        ((TextView) findViewById(R.id.mTv6)).setText("");
        ((TextView) findViewById(R.id.mTv7)).setText("");
        ((TextView) findViewById(R.id.mTv8)).setText("");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fat_device_control);
        mContext = this;
        mHandler = new Handler();
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
        mChceked = mySharedPreferences.getBoolean(Information.DB.AutoCheck, true);
        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mRssiField = (TextView) findViewById(R.id.rssi_value);
        mCheckField = (CheckedTextView) findViewById(R.id.check_value);

        etUserNum = (EditText) findViewById(R.id.etUserNum);
        etAge = (EditText) findViewById(R.id.etAge);
        etHeight = (EditText) findViewById(R.id.etHeight);
        rbMale = (RadioButton) findViewById(R.id.rbMale);
        rbFemale = (RadioButton) findViewById(R.id.rbFemale);
        btnEdit = (Button) findViewById(R.id.btnEdit);
        sendUserInfo = (Button) findViewById(R.id.sendUserInfo);
        etUserNum.setText(String.valueOf(mySharedPreferences.getInt(Information.DB.USER_NUM, 1)));
        etAge.setText(String.valueOf(mySharedPreferences.getInt(Information.DB.USER_AGE, 25)));
        etHeight.setText(String.valueOf(mySharedPreferences.getInt(Information.DB.USER_HEIGHT, 165)));
        if (mySharedPreferences.getInt(Information.DB.USER_SEX, 2) == 2) {
            rbFemale.setChecked(true);
            rbMale.setChecked(false);
        } else {
            rbFemale.setChecked(false);
            rbMale.setChecked(true);
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        this.findViewById(R.id.SendClear).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < 11; i++) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mWriteCharacteristic.setValue(ClearDataBuffer);
                            boolean writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                        }
                    }, 500 * i);
                }

            }
        });
        btnEdit.setText("编辑");
        etAge.setEnabled(false);
        etUserNum.setEnabled(false);
        etHeight.setEnabled(false);
//		rbMale.setChecked(false);
        rbMale.setEnabled(false);
//		rbFemale.setChecked(true);
        rbFemale.setEnabled(false);

        rbMale.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rbFemale.setChecked(false);
                rbMale.setChecked(true);
            }
        });
        rbFemale.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rbMale.setChecked(false);
                rbFemale.setChecked(true);
            }
        });
        sendUserInfo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mWriteCharacteristic.setValue(calSendBuffer());
                boolean writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
            }
        });
        btnEdit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEditAble) {
                    isEditAble = false;
                    btnEdit.setText("编辑");
                    etAge.setEnabled(false);
                    etHeight.setEnabled(false);
                    etUserNum.setEnabled(false);
                    rbMale.setEnabled(false);
                    rbFemale.setEnabled(false);

                    SharedPreferences.Editor editor = mySharedPreferences.edit();
                    editor.putInt(Information.DB.USER_NUM, Integer.valueOf(etUserNum.getText().toString()));
                    editor.putInt(Information.DB.USER_AGE, Integer.valueOf(etAge.getText().toString()));
                    editor.putInt(Information.DB.USER_HEIGHT, Integer.valueOf(etHeight.getText().toString()));
                    if (rbMale.isChecked()) {
                        editor.putInt(Information.DB.USER_SEX, 1);
                    } else {
                        editor.putInt(Information.DB.USER_SEX, 2);
                    }
                    editor.commit();
                    if (!mChceked) {
                        mWriteCharacteristic.setValue(calSendBuffer());
                        boolean writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                    }

                } else {
                    isEditAble = true;
                    btnEdit.setText("确认");
                    etAge.setEnabled(true);
                    etHeight.setEnabled(true);
                    rbMale.setEnabled(true);
                    rbFemale.setEnabled(true);
                    etUserNum.setEnabled(true);
                    clearUI();
                }
            }
        });

        getActionBar().setTitle(R.string.menu_back);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.button1).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                onBackPressed();
            }
        });


        Intent gattServiceIntent = new Intent(this, MkFatBluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
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

    private byte[] calSendBuffer() {
        sendBuffer[3] = (byte) Integer.parseInt(Integer.toHexString(Integer.valueOf(etAge.getText().toString().trim())), 16);
        sendBuffer[4] = (byte) Integer.parseInt(Integer.toHexString(Integer.valueOf(etHeight.getText().toString().trim())), 16);
        int userNum = Integer.valueOf(etUserNum.getText().toString());
        sendBuffer[2] = (byte) Integer.parseInt(Integer.toHexString((rbMale.isChecked() ? 8 : 0) * 16 + userNum), 16);
//					if (rbMale.isChecked()) {
//						sendBuffer[2] = (byte) Integer.parseInt(Integer.toHexString(81), 16);
//					} else {
//						sendBuffer[2] = (byte) Integer.parseInt(Integer.toHexString(1), 16);
//					}
        String strSum = Integer.toHexString((sendBuffer[1] & 0xff)
                + (sendBuffer[2] & 0xff) + (sendBuffer[3] & 0xff)
                + (sendBuffer[4] & 0xff) + (sendBuffer[5] & 0xff) + (sendBuffer[6] & 0xff));
        if (strSum.length() > 2) {
            strSum = strSum.substring(strSum.length() - 2, strSum.length());
        } else {
            strSum = "0" + strSum;
        }
        sendBuffer[7] = (byte) Integer.parseInt(strSum, 16);
        return sendBuffer;
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

    boolean writeResult = false;
    String str = "";

    private void displayData(String data) {
        Log.e(TAG, "------data:" + data);
        if (data != null) {
            String[] strdata = data.split("-");
            Log.e("Notification", data);
            if (strdata.length >= 8) {
                String tmpNum;
                int number;
                float fNumber;
                switch (strdata[6]) {
                    case "A0":
                        tmpNum = strdata[2] + strdata[3];
                        number = Integer.valueOf(tmpNum, 16);
                        fNumber = number / 10f;
                        mDataField.setText(String.valueOf(fNumber) + "kg");
                        mDataField.setTextColor(Color.BLACK);
                        if (!writeResult) {
                            for (int i = 0; i < 5; i++) {
                                mWriteCharacteristic.setValue(calSendBuffer());
                                writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                            }
                        }
                        if (!str.contains("A0"))
                            str += "A0";
                        break;
                    case "AA":
                        tmpNum = strdata[2] + strdata[3];
                        number = Integer.valueOf(tmpNum, 16);
                        fNumber = number / 10f;
                        for (int i = 0; i < 3; i++) {
                            mWriteCharacteristic.setValue(calSendBuffer());
                            writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                        }
                        mDataField.setText(String.valueOf(fNumber) + "kg");
                        mDataField.setTextColor(Color.GREEN);
                        if (!str.contains("AA"))
                            str += "AA";
                        break;
                    case "B0":
                        tmpNum = strdata[2] + strdata[3];
                        number = Integer.valueOf(tmpNum, 16);
                        fNumber = number / 10f;
                        ((TextView) findViewById(R.id.mTv1)).setText(String.valueOf(fNumber));
                        tmpNum = strdata[4] + strdata[5];
                        number = Integer.valueOf(tmpNum, 16);
                        fNumber = number / 10f;
                        ((TextView) findViewById(R.id.mTv2)).setText(String.valueOf(fNumber));
                        if (!str.contains("B0"))
                            str += "B0";
                        break;
                    case "C0":
                        tmpNum = strdata[2] + strdata[3];
                        number = Integer.valueOf(tmpNum, 16);
                        fNumber = number / 10f;
                        ((TextView) findViewById(R.id.mTv4)).setText(String.valueOf(fNumber));
                        tmpNum = strdata[5] + strdata[4];
                        number = Integer.valueOf(tmpNum, 16);
                        fNumber = number / 10f;
                        ((TextView) findViewById(R.id.mTv3)).setText(String.valueOf(fNumber));
                        if (!str.contains("C0"))
                            str += "C0";
                        break;
                    case "D0":
                        tmpNum = strdata[2] + strdata[3];
                        number = Integer.valueOf(tmpNum, 16);
//                        fNumber = number / 10f;
                        ((TextView) findViewById(R.id.mTv5)).setText(String.valueOf(number));
                        if (!str.contains("D0"))
                            str += "D0";
                        break;
                    case "B1":
                        tmpNum = strdata[2] + strdata[3];
                        number = Integer.valueOf(tmpNum, 16);
//                        fNumber = number / 10f;
                        ((TextView) findViewById(R.id.mTv6)).setText(String.valueOf(number));
                        if (!str.contains("B1"))
                            str += "B1";
                        break;
                    case "B3":
                        tmpNum = strdata[3];
                        number = Integer.valueOf(tmpNum, 16);
//                        fNumber = number / 10f;
                        ((TextView) findViewById(R.id.mTv7)).setText(String.valueOf(number));
                        tmpNum = strdata[5];
                        number = Integer.valueOf(tmpNum, 16);
                        ((TextView) findViewById(R.id.mTv8)).setText(String.valueOf(number));
                        if (!str.contains("B3"))
                            str += "B3";
                        break;
                    default:
                        break;
                }
                if (str.equalsIgnoreCase("A0AAB0C0D0")) {
                    mCheckField.setChecked(true);
                    if (mChceked) {
                        Intent intent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putString("BleSuc", mDeviceAddress);
                        intent.putExtras(bundle);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }
            }
        }

        mBluetoothLeService.getmBluetoothGatt().readRemoteRssi();
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
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MkFatBluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(MkFatBluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(MkFatBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(MkFatBluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(MkFatBluetoothLeService.ACTION_DATA_RSSI);
        return intentFilter;
    }
}
