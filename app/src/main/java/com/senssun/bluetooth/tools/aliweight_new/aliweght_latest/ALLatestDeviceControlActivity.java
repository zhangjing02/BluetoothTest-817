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

package com.senssun.bluetooth.tools.aliweight_new.aliweght_latest;

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
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.relative.Information;

import java.util.ArrayList;
import java.util.List;

import widget.BorderTextView;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class ALLatestDeviceControlActivity extends Activity implements OnClickListener {
    private final static String TAG = ALLatestDeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_MODEL = "DEVICE_MODEL";

    private CheckedTextView mCheckField;
    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mRssiField;

    private String mDeviceName;
    private String mDeviceAddress;
    private int mDeviceModel;
    private ALLatestBluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private boolean mDess = true;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private BorderTextView mWeight_range;
    private BorderTextView mWeight_precision, mFat_set, mFat_precision;
    private SharedPreferences mySharedPreferences;
    private SharedPreferences.Editor editor;
    private int mWeight_down, mWeight_up, mWeight_in, mFat_in;
    private float mWeight_precision_f, mWeight_f, mFat_f;
    private String mFat_set_sth;
    private LinearLayout mFat_layout;
    private Button mChange_user_btn;
    private String user_weight, user_height, user_age;
    private boolean user_famle;
    private int this_fat;
    private LinearLayout mScreen_layout, mWeight_layout;
    private float weight_now;
    private byte[] sendBuffer;
    private boolean is_send_user = true;
    private boolean mIsSend = false;


    private boolean mIsfasong = true;
    private boolean mIsfasong02 = true;
    private boolean mIsfasong03 = true;
    StringBuffer stringBuffer = new StringBuffer();
    private int count_info=0;

//    private int modeAutoCheck;// 3 和 4选项
//
//	public final static byte[] sendBuffer = new byte[]{(byte) Integer.parseInt("A5", 16),
//		(byte) Integer.parseInt("5A", 16),0, 0, 0, 0, 0, 0, 0};//


    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((ALLatestBluetoothLeService.LocalBinder) service).getService();
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
            if (ALLatestBluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (ALLatestBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
//                SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
//                if (mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 2) {//|| mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 3
//                    finish();
//                } else {
                mBluetoothLeService.close();
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                mBluetoothLeService.connect(mDeviceAddress);
                invalidateOptionsMenu();
                clearUI();
                mBluetoothLeService.connect(mDeviceAddress);
//                }

            } else if (ALLatestBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (ALLatestBluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getByteArrayExtra(ALLatestBluetoothLeService.EXTRA_DATA));
            } else if (ALLatestBluetoothLeService.ACTION_DATA_RSSI.equals(action)) {
                displayRssi(intent.getStringExtra("Rssi"));
            }
        }
    };


    private void clearUI() {
        mDataField.setText(R.string.no_data);
        ((TextView) findViewById(R.id.mTv1)).setText("--");
        ((TextView) findViewById(R.id.mtv2)).setText("--");
        ((TextView) findViewById(R.id.mTv3)).setText("--");
        ((TextView) findViewById(R.id.mTv4)).setText("--");
        ((TextView) findViewById(R.id.mTv5)).setText("--");
        ((TextView) findViewById(R.id.mTv6)).setText("--");
        ((TextView) findViewById(R.id.mTv7)).setText("--");
        ((TextView) findViewById(R.id.mTv8)).setText("--");
        ((TextView) findViewById(R.id.mTv9)).setText("--");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ali_latest_device_control);

        mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
        editor = mySharedPreferences.edit();
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mDeviceModel = intent.getIntExtra(EXTRAS_DEVICE_MODEL, 0);

        user_weight = mySharedPreferences.getString("user_weight", "60");
        user_height = mySharedPreferences.getString("user_height", "165");
        user_age = mySharedPreferences.getString("user_age", "25");
        user_famle = mySharedPreferences.getBoolean("user_famle", true);


        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mScreen_layout = (LinearLayout) findViewById(R.id.screen_layout);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mRssiField = (TextView) findViewById(R.id.rssi_value);
        mCheckField = (CheckedTextView) findViewById(R.id.check_value);
        mFat_layout = (LinearLayout) findViewById(R.id.mTv1Layout);

        mWeight_range = (BorderTextView) findViewById(R.id.mWeight_range);
        mWeight_precision = (BorderTextView) findViewById(R.id.mWeight_precision);
        mFat_set = (BorderTextView) findViewById(R.id.mFat_set);
        mFat_precision = (BorderTextView) findViewById(R.id.fat_precision);
        mChange_user_btn = (Button) findViewById(R.id.button4);
        mWeight_layout = (LinearLayout) findViewById(R.id.weight_layout);
        String sex;
        if (user_famle) {
            sex = "女";
        } else {
            sex = "男";
        }
      //  mChange_user_btn.setText("发送用户：序号：1" + sex + user_age + "岁" + user_height + "cm" + user_weight + "kg");

        mWeight_down = mySharedPreferences.getInt("weight_down", 100);
        mWeight_up = mySharedPreferences.getInt("weight_up", 100);
        mWeight_precision_f = mySharedPreferences.getFloat("limitPrecision", 5 / 10f);
        mFat_set_sth = mySharedPreferences.getString("fat_set", "30");

        mWeight_in = mySharedPreferences.getInt("weight_in", 100);
        mWeight_f = mySharedPreferences.getFloat("weight_f", 5);
        mFat_in = mySharedPreferences.getInt("fat_in", 100);
        mFat_f = mySharedPreferences.getFloat("fat_f", 5);


        float x;
        float y;
        mFat_set.setText("(脂肪率)" + mFat_in + "%");
        mFat_precision.setText("±" + mFat_f + "%" + "(误差范围)");
        SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
        if (mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 3) {
            x = mWeight_down + mWeight_precision_f;
            y = mWeight_down - mWeight_precision_f;
            mWeight_range.setText("(体重范围)从" + y + "～" + x + "kg");
            mWeight_precision.setText("±" + mWeight_precision_f + "(误差范围)");
        } else {
            x = mWeight_in + mWeight_f;
            y = mWeight_in - mWeight_f;
            mWeight_range.setText("(体重范围)从" + y + "～" + x + "kg");
            mWeight_precision.setText("±" + mWeight_f + "(误差范围)");
        }


        mHandler = new Handler();
        if (mDeviceModel == 1) {
            findViewById(R.id.mTv1Layout).setVisibility(View.GONE);
            findViewById(R.id.mTv2Layout).setVisibility(View.GONE);
            findViewById(R.id.mTv3Layout).setVisibility(View.GONE);
            findViewById(R.id.mTv4Layout).setVisibility(View.GONE);
            findViewById(R.id.mTv5Layout).setVisibility(View.GONE);
        }

        getActionBar().setTitle(R.string.menu_back);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.button1).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                onBackPressed();
            }
        });

        Intent gattServiceIntent = new Intent(this, ALLatestBluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        DessRssi();
//		Dess();
//设置用户信息，按钮
//		findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//                Log.i("zhangjing","用户信息");
//				if(mWriteCharacteristic != null){
//                    byte[] sendBuffer = new byte[]{(byte) Integer.parseInt("02", 16),
//							(byte) Integer.parseInt("00", 16),(byte) Integer.parseInt("08", 16),(byte) Integer.parseInt("A5", 16),(byte) Integer.parseInt("10", 16),(byte) Integer.parseInt("01", 16),
//							(byte) Integer.parseInt("12", 16),(byte) Integer.parseInt("A7", 16),(byte) Integer.parseInt("02", 16),(byte) Integer.parseInt("8F", 16),(byte) Integer.parseInt("5B", 16)};
//					num = getSharedPreferences("sp",Context.MODE_PRIVATE).getInt(Information.DB.TEST_NUM, 1);
////					sendBuffer[5] = (byte)Integer.parseInt(Integer.toHexString(0 * 16+num),16);
//
//					mWriteCharacteristic.setValue(sendBuffer);
//					writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
//					Log.e(TAG,"-------writeResult:"+writeResult);
//
//                    Log.i("zhangjing","用户信息"+sendBuffer[9]);
//                    Log.i("zhangjing","用户信息"+sendBuffer[10]);
//                    Log.i("zhangjing","用户信息"+sendBuffer[11]);
//				}
//			}
//		});
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
        clearUI();
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        unregisterReceiver(mGattUpdateReceiver);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
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

    boolean writeResult = false;

    private void displayData(byte[] data) {
        if (mIsfasong) {
            for (int i = 0; i < 5; i++) {
                changeUserInfo();
                if (i >=3) {
                    mIsfasong = false;
                    break;
                }
            }
        }

//        stringBuffer=new StringBuffer();
        if (data != null) {
            for (byte byteChar : data) {
                String ms = String.format("%02X ", byteChar).trim();
                stringBuffer.append(ms);
            }
//            Log.e(TAG, "收到数据" + stringBuffer.toString());
            if (stringBuffer.length() >= 20) {
                String tmp = stringBuffer.substring(0, 6);
                if (tmp.equals("010025")) {
                    if (stringBuffer.length() >= 80) {
                        String dd = stringBuffer.substring(0, 80);

                        StringBuffer verifyData = new StringBuffer(stringBuffer.substring(0, 78));
                        int verify = 0;
                        while (verifyData.length() > 0) {
                            String tmpNum = verifyData.substring(0, 2);
                            verify = verify + Integer.valueOf(tmpNum, 16);
                            verifyData.delete(0, 2);
                        }

                        String byte5and6 = Long.toHexString(verify).toUpperCase();

                        boolean confirmVerify = byte5and6.substring(byte5and6.length() - 2, byte5and6.length()).equals(stringBuffer.substring(78, 80));
                        Log.e(TAG, "010025收到数据" + dd + "校验位" + stringBuffer.substring(78, 80) + "校验判断" + confirmVerify + "byte5and6" + byte5and6);

                        if (confirmVerify) {
                            int weight = Integer.valueOf(dd.substring(13, 16), 16);
                            mDataField.setText(String.valueOf(weight));

                            int fat = Integer.valueOf(dd.substring(36, 40), 16);
                            ((TextView) findViewById(R.id.mTv1)).setText(String.valueOf(fat));


                            int moisture = Integer.valueOf(dd.substring(21, 24), 16);
                            ((TextView) findViewById(R.id.mtv2)).setText(String.valueOf(moisture));

                            int muscle = Integer.valueOf(dd.substring(25, 28), 16);
                            ((TextView) findViewById(R.id.mTv3)).setText(String.valueOf(muscle));

                            int bone = Integer.valueOf(dd.substring(29, 32), 16);
                            ((TextView) findViewById(R.id.mTv4)).setText(String.valueOf(bone));

                            int calorie = Integer.valueOf(dd.substring(33, 36), 16);
                            ((TextView) findViewById(R.id.mTv5)).setText(String.valueOf(calorie));

                            int protein = Integer.valueOf(dd.substring(41, 44), 16);
                            ((TextView) findViewById(R.id.mTv6)).setText(String.valueOf(protein));

                            int bmi = Integer.valueOf(dd.substring(49, 52), 16);
                            ((TextView) findViewById(R.id.mTv7)).setText(String.valueOf(bmi));

                            int bodyAge = Integer.valueOf(dd.substring(45, 46), 16);
                            ((TextView) findViewById(R.id.mTv8)).setText(String.valueOf(bodyAge));

                            int healthGrade = Integer.valueOf(dd.substring(47, 48), 16);
                            ((TextView) findViewById(R.id.mTv9)).setText(String.valueOf(healthGrade));
                        }

                        stringBuffer.delete(0, 80);
                    }
                } else if (tmp.equals("010039")) {
                    ///****************************************zhangjing
//                    if (sendBuffer != null && is_send_user) {
//                        for (int i = 0; i < 5; i++) {
//                            mWriteCharacteristic.setValue(sendBuffer);
//                            writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
//                            if (i == 4) {
//                                is_send_user = false;
//                            }
//                        }
//                    }
                    if (stringBuffer.length() >= 120) {
//                        if (mIsfasong02) {
//                            for (int i = 0; i < 5; i++) {
//                                Log.i("zhangjign",i+"下发用户信息"+mIsfasong02);
//                                changeUserInfo();
//                                if (i >=2) {
//                                    Log.i("zhangjign002","下发用户信息");
//                                    mIsfasong02 = false;
//                                    break;
//                                }
//                            }
//                        }
                        String dd = stringBuffer.substring(0, 120);
                        StringBuffer verifyData = new StringBuffer(stringBuffer.substring(0, 118));
                        int verify = 0;
                        while (verifyData.length() > 0) {
                            String tmpNum = verifyData.substring(0, 2);
                            verify = verify + Integer.valueOf(tmpNum, 16);
                            verifyData.delete(0, 2);
                        }
                        String byte5and6 = Long.toHexString(verify).toUpperCase();
                        boolean confirmVerify = byte5and6.substring(byte5and6.length() - 2, byte5and6.length()).equals(stringBuffer.substring(118, 120));
                        Log.e(TAG, "010039收到数据" + dd + "校验位" + stringBuffer.substring(118, 120) + "校验判断" + confirmVerify + "byte5and6" + byte5and6);

                        if (confirmVerify) {
                            int weight = Integer.valueOf(dd.substring(13, 16), 16);
                            mDataField.setText(String.valueOf(weight / 10f));
                            Log.i("tamen", "我们的体重?" + weight);

                            //此处判断进行哪种模式的测量，如果是复检模式下，就判断如果在体重范围内就发CRL，并且finish如果超出范围，就报红。
                            SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
                            if (mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 3) {
                                int store_weight_in = mySharedPreferences.getInt("weight_down", 180);
                                float store_weight_Precision = mySharedPreferences.getFloat("limitPrecision", 5 / 10f);
                                float x = store_weight_in + store_weight_Precision;
                                float y = store_weight_in - store_weight_Precision;
                                Log.i("fengqingyang", "存储体重是？" + x);
                                Log.i("fengqingyang", "当前体重是？" + weight / 10f);
                                float weight_now = weight / 10f;
                                if (weight_now < x && weight_now > y) {

                                    num2 = 0;
                                    mHandler.postDelayed(mRunnable2, 3000);
                                } else if (weight_now > x) {
                                    // 超出范围，看怎么处理
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            mScreen_layout.setBackgroundResource(R.color.red);
                                            mCheckField.setText("体重超出范围");
                                            return;
                                        }
                                    }, 2000);
                                }
                            }
                            final int fat = Integer.valueOf(dd.substring(36, 40), 16);
                            this_fat = fat;
                            ((TextView) findViewById(R.id.mTv1)).setText(String.valueOf(fat / 10f) + "%");

                            int moisture = Integer.valueOf(dd.substring(21, 24), 16);
                            ((TextView) findViewById(R.id.mtv2)).setText(String.valueOf(moisture / 10f) + "%");

                            int muscle = Integer.valueOf(dd.substring(25, 28), 16);
                            ((TextView) findViewById(R.id.mTv3)).setText(String.valueOf(muscle / 10f) + "%");

                            int bone = Integer.valueOf(dd.substring(29, 32), 16);
                            ((TextView) findViewById(R.id.mTv4)).setText(String.valueOf(bone / 10f) + "%");

                            int calorie = Integer.valueOf(dd.substring(33, 36), 16);
                            ((TextView) findViewById(R.id.mTv5)).setText(String.valueOf(calorie));

                            int protein = Integer.valueOf(dd.substring(41, 44), 16);
                            ((TextView) findViewById(R.id.mTv6)).setText(String.valueOf(protein));

                            int bmi = Integer.valueOf(dd.substring(49, 52), 16);
                            ((TextView) findViewById(R.id.mTv7)).setText(String.valueOf(bmi / 10f));

                            int bodyAge = Integer.valueOf(dd.substring(45, 46), 16);
                            ((TextView) findViewById(R.id.mTv8)).setText(String.valueOf(bodyAge));

                            int healthGrade = Integer.valueOf(dd.substring(47, 48), 16);
                            ((TextView) findViewById(R.id.mTv9)).setText(String.valueOf(healthGrade));

//                            float xx = mWeight_in + mWeight_f;
//                            float yy = mWeight_in - mWeight_f;
//                            float this_weight_yy = weight / 10f;
//                            if (this_weight_yy <= xx && this_weight_yy >= yy)
//                                if (mIsfasong03) {
//                                for (int i = 0; i < 9; i++) {
//                                    Log.i("xxxxxxxxxxxx",i+"下发用户信息"+mIsfasong03);
//                                    changeUserInfo();
//                                    if (i >=1) {
//                                        Log.i("xxxxxxxxxxxxxx","下发用户信息");
//                                        mIsfasong03 = false;
//                                        break;
//                                    }
//                                }
//                            }
                            if (this_fat > 0) {
                                if (mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 1) {
                                    //脂肪判断和体重判断一起
                                    float x = mWeight_in + mWeight_f;
                                    float y = mWeight_in - mWeight_f;
                                    float m = mFat_in + mFat_f;
                                    float n = mFat_in - mFat_f;
                                    float this_weight = weight / 10f;

                                    if (this_fat / 10f <= m && this_fat / 10f >= n) {
                                        mFat_layout.setBackgroundResource(R.color.z);
                                        if (this_weight <= x && this_weight >= y) {
                                            //如果脂肪在范围内，并且体重也在范围内，就发送秤体CRL，并返回。
                                            mHandler.post(mRunnable3);
                                        }
                                    } else if (this_fat/10f > m || this_fat/10f < n) {
                                        mFat_layout.setBackgroundResource(R.color.lightcoral);
                                    }
                                    if (this_weight <= x && this_weight >= y) {
                                        mWeight_layout.setBackgroundResource(R.color.z);
                                    } else if (this_weight > x || this_weight < y) {
                                        mWeight_layout.setBackgroundResource(R.color.lightcoral);
                                    }
                                }
                            }

//                            if (fat > 0 && mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 3) {
//                                mHandler.postDelayed(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        finish();
//                                    }
//                                }, 1000);
//
//
////                                mHandler.postDelayed(new Runnable() {
////                                    @Override
////                                    public void run() {
////                                        if (mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 3 && fat > 0) {
////                                            Intent intent = new Intent();
////                                            Bundle bundle = new Bundle();
////                                            bundle.putString("BleSuc", mDeviceAddress);
////                                            intent.putExtras(bundle);
////                                            setResult(RESULT_OK, intent);
////                                            finish();
////                                        }
////                                    }
////                                }, 3000);
////                                mHandler.postDelayed(mRunnable2, 500);
//                            }
                        }
                        stringBuffer.delete(0, 120);
                    }
                }
            }
        }

//		if (data != null) {
//			String[] strdata=data.split("-");
//			Log.e("Notification", data);
//			if(strdata.length>=10){
//				if(data.equalsIgnoreCase("02-00-08-FF-A5-10-00-00-00-00-D5-")){
//					mHandler.removeCallbacks(mRunnable);
//					mCheckField.setChecked(true);
//					SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
//					if(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
//						Intent intent = new Intent();
//						Bundle bundle = new Bundle();
//						bundle.putString("BleSuc", mDeviceAddress);
//						intent.putExtras(bundle);
//						setResult(RESULT_OK, intent);
//						num++;
//						if(num>3) num = 1;
//						SharedPreferences.Editor editor = getSharedPreferences("sp",Context.MODE_PRIVATE).edit();
//						editor.putInt(Information.DB.TEST_NUM, num);
//						editor.commit();
//						finish();
//					}
//				}
//				if (strdata[9].equals("AA")||strdata[9].equals("A0")){
//					String tmpNum = strdata[5] + strdata[6];
//					int weight = Integer.valueOf(tmpNum, 16);
//					mDataField.setText(String.valueOf(weight));
//
//					if(mDeviceModel==1){
//						mCheckField.setChecked(true);
//						SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
//						if(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
//							Intent intent = new Intent();
//							Bundle bundle = new Bundle();
//							bundle.putString("BleSuc", mDeviceAddress);
//							intent.putExtras(bundle);
//							setResult(RESULT_OK, intent);
//							finish();
//						}
//					}
//				}else if(strdata[9].equals("D0")){
//					String tmpNum= strdata[5]+strdata[6];
//					((TextView)findViewById(R.id.mTv5)).setText(String.valueOf(Integer.valueOf(tmpNum,16)));
//				}else if(strdata[9].equals("C0")){
//					String tmpNum= strdata[5]+strdata[6];
//					((TextView)findViewById(R.id.mTv3)).setText(String.valueOf(Integer.valueOf(tmpNum,16)));
//
//					tmpNum= strdata[8]+strdata[7];
//					((TextView)findViewById(R.id.mTv4)).setText(String.valueOf(Integer.valueOf(tmpNum,16)));
//				}else if(strdata[9].equals("B0")){
//					String tmpNum= strdata[5]+strdata[6];
//					((TextView)findViewById(R.id.mtv2)).setText(String.valueOf(Integer.valueOf(tmpNum,16)));
//
//					 tmpNum= strdata[7]+strdata[8];
//					((TextView)findViewById(R.id.mTv1)).setText(String.valueOf(Integer.valueOf(tmpNum,16)));
//
//					if(mDeviceModel==2){
//						mCheckField.setChecked(true);
//						SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
//						if(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
//							Intent intent = new Intent();
//							Bundle bundle = new Bundle();
//							bundle.putString("BleSuc", mDeviceAddress);
//							intent.putExtras(bundle);
//							setResult(RESULT_OK, intent);
//							finish();
//						}
//					}
//				}
//
//			}
//		}
    }

    Runnable mRunnable2 = new Runnable() {
        @Override
        public void run() {
            if (mWriteCharacteristic != null && mBluetoothLeService != null && num2 < 5) {
                byte[] sendBuffer = new byte[]{0x01, 00, 0x11, (byte) 0xA1, 0x03, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xB7};
                mWriteCharacteristic.setValue(sendBuffer);
                writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                num2++;
            }
            if (num2 >= 5) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mHandler.removeCallbacks(mRunnable2);
                        mHandler.removeCallbacks(mRunnable);
                        mHandler.removeCallbacks(mRunnable3);
                        finish();
                    }
                }, 3000);
            }
            mHandler.postDelayed(mRunnable2, 500);
        }
    };

    Runnable mRunnable3 = new Runnable() {
        @Override
        public void run() {
            if (mWriteCharacteristic != null && mBluetoothLeService != null && num2 < 5) {
                byte[] sendBuffer_clear = new byte[]{0x01, 00, 0x11, (byte) 0xA1, 0x03, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xB7};
                mWriteCharacteristic.setValue(sendBuffer_clear);
                writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                num2++;
            }
            if (num2 >= 5) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mHandler.removeCallbacks(mRunnable3);
                        mHandler.removeCallbacks(mRunnable);
                        mHandler.removeCallbacks(mRunnable2);
                        SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
                        if (mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 1) {
                            Intent intent = new Intent();
                            Bundle bundle = new Bundle();
                            bundle.putString("BleSuc", mDeviceAddress);
                            intent.putExtras(bundle);
                            setResult(RESULT_OK, intent);
                        }
                        finish();
                    }
                }, 3000);
            }
            mHandler.postDelayed(mRunnable3, 500);
        }
    };

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        Log.e(TAG, "-------Runnable:");
        if (gattServices == null) return;

        for (BluetoothGattService gattService : gattServices) {
            String tmp = gattService.getUuid().toString();
            if (gattService.getUuid().toString().equals("0000feb3-0000-1000-8000-00805f9b34fb")) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
                    if (characteristic.getUuid().toString().trim().equals("0000fed6-0000-1000-8000-00805f9b34fb")) {

                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                        mNotifyCharacteristic = characteristic;
                    }
                    if (characteristic.getUuid().toString().trim().equals("0000fed5-0000-1000-8000-00805f9b34fb")) {
                        mWriteCharacteristic = characteristic;
                        writeResult = false;
                    }
                }
            }
        }
        mHandler.postDelayed(mRunnable, 500);
    }

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (mWriteCharacteristic != null && num < 5) {//&&!mIsSend

                byte[] sendBuffer = null;

                SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
                switch (mySharedPreferences.getInt(Information.DB.AliCheck, 0)) {
                    case 0:
                        break;
                    case 1:
//                            sendBuffer = new byte[]{0x01, 00, 0x11, (byte) 0xA1, 0x01, 0x01, 0x01, 0x12, (byte) 0xA7, 0x02, (byte) 0x8F,
//                                    0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x00};
//                        for (int i = 0; i < 5; i++) {
//                            changeUserInfo();
//                        }

                        //脂肪判断

//                        int mFat_set_in = parseInt(mFat_set_sth);
//                        if (this_fat / 10f <= mFat_set_in + 0.5 && this_fat / 10f >= mFat_set_in - 0.5) {
//                            Log.i("women", mFat_set_in + "----这里是合格的脂肪" + this_fat / 10f);
//                            mFat_layout.setBackgroundResource(R.color.z);
//                        } else {
//                            Log.i("women", mFat_set_in + "----这里是超出范围的脂肪" + this_fat / 10f);
//                            mFat_layout.setBackgroundResource(R.color.lightcoral);
//                        }
                        break;
                    //清楚用户1
                    case 2:
                        sendBuffer = new byte[]{0x01, 00, 0x11, (byte) 0xA1, 0x03, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
                                0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xB7};
                        break;
                    //发送当前用户
                    case 3:
//                        sendBuffer = new byte[]{0x01, 00, 0x11, (byte) 0xA1, 0x01, 0x01, 0x01, 0x19, (byte) 0xA6, 0x02, (byte) 0x8F,
//                                0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x206};
//                        changeUserInfo(sendBuffer);


                        //太快了，得想办法延时处理，或移动位置。
//                        sendBuffer = new byte[]{0x01, 00, 0x11, (byte) 0xA1, 0x03, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
//                                    0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xB7};
                        break;
                }

//					byte[] sendBuffer =new byte[]{0x01,00,0x08,(byte)0xA1,0x01,0x01,0x01,0x12,(byte)0xA7,0x02,(byte)0x8F,
//							0,0,0,0,0,0,0,0,(byte)0xF7};

                Log.e(TAG, "-------Runnable:");
                if (sendBuffer != null && mWriteCharacteristic != null) {
//					sendBuffer[5] = (byte)Integer.parseInt(Integer.toHexString(0 * 16+num),16);
                    //****************************zhangjing
//                    mWriteCharacteristic.setValue(sendBuffer);
//                    writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                    Log.e(TAG, "-------writeResult:" + writeResult);
                    if (writeResult) {
                        num++;
                        if (num >= 5 && mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 2) {
                            finish();
                        }
                        mCheckField.setChecked(true);
                    }
                    mIsSend = writeResult;
                }
            }
           mHandler.postDelayed(mRunnable, 500);
        }
    };
    Handler mHandler;
    int num = 0;
    int num2 = 0;
    int count = 0;

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ALLatestBluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(ALLatestBluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ALLatestBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ALLatestBluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ALLatestBluetoothLeService.ACTION_DATA_RSSI);
        return intentFilter;
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button4:
                if (mWriteCharacteristic != null) {
//                    byte[] sendBuffer = new byte[]{0x01, 00, 0x11, (byte) 0xA1, 0x01, 0x01, 0x01, 0x12, (byte) 0xA7, 0x02, (byte) 0x8F,
//                            0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x00};

                  //  changeUserInfo();
                    if (sendBuffer != null) {
                        mWriteCharacteristic.setValue(sendBuffer);
                        writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                    }
                }
                break;
            case R.id.button5:
                if (mWriteCharacteristic != null) {
                    byte[] sendBuffer = new byte[]{0x01, 00, 0x11, (byte) 0xA1, 0x03, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
                            0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xB7};
                    mWriteCharacteristic.setValue(sendBuffer);
                    writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                }
                break;
        }
    }

    private void changeUserInfo() {
//        byte[] sendBuffer = new byte[]{0x01, 00, 0x11, (byte) 0xA1, 0x01, 0x01, 0x00, 0x19, (byte) 0xA6, 0x02, (byte) 0xE6,
//                0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x25D};

        sendBuffer = new byte[]{0x01, 00, 0x11, (byte) 0xA1, 0x01, 0x01, 0x00, 0x19, (byte) 0xA6, 0x02, (byte) 0x8F,
                0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x200};

        Log.i("zhangjing", "我们的用户信息" + user_weight + "kg" + user_height + "cm" + user_age + "岁");

        int weight_in = Integer.parseInt(user_weight) * 10;
        if (weight_in >= 512) {
            sendBuffer[9] = (byte) Integer.parseInt("02", 16);
            sendBuffer[10] = (byte) (weight_in - 512);
        } else if (weight_in >= 256) {
            sendBuffer[9] = (byte) Integer.parseInt("01", 16);
            sendBuffer[10] = (byte) (weight_in - 256);
        } else if (weight_in < 256) {
            sendBuffer[9] = (byte) Integer.parseInt("00", 16);
            sendBuffer[10] = (byte) (weight_in);
        }
        sendBuffer[8] = (byte) Integer.parseInt(user_height);
        sendBuffer[7] = (byte) Integer.parseInt(user_age);
        if (user_famle) {
            Log.i("jinbu", "性别" + user_famle);
            sendBuffer[6] = (byte) Integer.parseInt("00");
        } else if (!user_famle) {
            Log.i("jinbu", "性别" + user_famle);
            sendBuffer[6] = (byte) Integer.parseInt("01");
        }
        int x = 0;
        for (int i = 0; i < sendBuffer.length - 1; i++) {
            x += sendBuffer[i] & 0xFF;
        }
        Log.i("zhangjing", "校验和" + x);
        sendBuffer[19] = (byte) x;
      //  handler.sendEmptyMessageDelayed(5,5000);
        count_info++;
        Toast.makeText(ALLatestDeviceControlActivity.this,"发送"+count_info+"次用户信息",Toast.LENGTH_SHORT).show();
    }

//    Handler handler=new Handler(){
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            if (msg.what==5){
//                Toast.makeText(ALLatestDeviceControlActivity.this,"发送用户信息",Toast.LENGTH_SHORT).show();
//            }
//        }
//    };

}
