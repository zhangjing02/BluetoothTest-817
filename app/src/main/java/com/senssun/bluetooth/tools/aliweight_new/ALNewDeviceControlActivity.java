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

package com.senssun.bluetooth.tools.aliweight_new;

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
public class ALNewDeviceControlActivity extends Activity implements OnClickListener {
    private final static String TAG = ALNewDeviceControlActivity.class.getSimpleName();

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
    private ALNewBluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private boolean mDess = true;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;

    private boolean mIsSend = false;

    StringBuffer stringBuffer = new StringBuffer();


//    private int modeAutoCheck;// 3 和 4选项
//
//	public final static byte[] sendBuffer = new byte[]{(byte) Integer.parseInt("A5", 16),
//		(byte) Integer.parseInt("5A", 16),0, 0, 0, 0, 0, 0, 0};//


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((ALNewBluetoothLeService.LocalBinder) service).getService();
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
            if (ALNewBluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (ALNewBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
//                SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
//                if (mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 2) {//|| mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 3
//                    finish();
//                } else {
                    mBluetoothLeService.close();
                    mConnected = false;
                    updateConnectionState(R.string.disconnected);
                    invalidateOptionsMenu();
                    clearUI();
                    mBluetoothLeService.connect(mDeviceAddress);
//                }

            } else if (ALNewBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (ALNewBluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getByteArrayExtra(ALNewBluetoothLeService.EXTRA_DATA));
            } else if (ALNewBluetoothLeService.ACTION_DATA_RSSI.equals(action)) {
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
        setContentView(R.layout.activity_ali_test_device_control);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mDeviceModel = intent.getIntExtra(EXTRAS_DEVICE_MODEL, 0);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mRssiField = (TextView) findViewById(R.id.rssi_value);
        mCheckField = (CheckedTextView) findViewById(R.id.check_value);
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

        Intent gattServiceIntent = new Intent(this, ALNewBluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        DessRssi();
//		Dess();

//		findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				if(mWriteCharacteristic != null){
//					byte[] sendBuffer = new byte[]{(byte) Integer.parseInt("02", 16),
//							(byte) Integer.parseInt("00", 16),(byte) Integer.parseInt("08", 16),(byte) Integer.parseInt("A5", 16),(byte) Integer.parseInt("10", 16),(byte) Integer.parseInt("01", 16),
//							(byte) Integer.parseInt("12", 16),(byte) Integer.parseInt("A7", 16),(byte) Integer.parseInt("02", 16),(byte) Integer.parseInt("8F", 16),(byte) Integer.parseInt("5B", 16)};
//					num = getSharedPreferences("sp",Context.MODE_PRIVATE).getInt(Information.DB.TEST_NUM, 1);
////					sendBuffer[5] = (byte)Integer.parseInt(Integer.toHexString(0 * 16+num),16);
//
//					mWriteCharacteristic.setValue(sendBuffer);
//					writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
//					Log.e(TAG,"-------writeResult:"+writeResult);
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
                    if (stringBuffer.length() >= 120) {
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
                            Log.i("wwww", "confirmVerify");
                            int weight = Integer.valueOf(dd.substring(13, 16), 16);
                            mDataField.setText(String.valueOf(weight));

                            //3

                            Log.i("wwww", "weight=" + weight);
                            SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
                            if (mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 1) {
//                                if ((Math.abs(weight / 10) > 10 && Math.abs(weight / 10) < 50)) {
//                                    Log.i("wwww", "wwwww");
//                                    mHandler.postDelayed(mRunnable2, 0);
//                                }
                                if ((Math.abs(weight) > 149 && Math.abs(weight) < 151) || (Math.abs(weight) > 179 && Math.abs(weight) < 181)) {
                                    mHandler.postDelayed(mRunnable2, 0);
                                }
                            }


                            final int fat = Integer.valueOf(dd.substring(36, 40), 16);
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


                            if (fat > 0 && mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 3) {
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        finish();
                                    }
                                }, 1000);

//                                mHandler.postDelayed(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        if (mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 3 && fat > 0) {
//                                            Intent intent = new Intent();
//                                            Bundle bundle = new Bundle();
//                                            bundle.putString("BleSuc", mDeviceAddress);
//                                            intent.putExtras(bundle);
//                                            setResult(RESULT_OK, intent);
//                                            finish();
//                                        }
//                                    }
//                                }, 3000);
//                                mHandler.postDelayed(mRunnable2, 500);
                            }
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
                finish();
            }
            mHandler.postDelayed(mRunnable2, 500);
        }
    };

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        Log.e(TAG, "-------Runnable:" );
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
                        break;
                    //清楚用户1
                    case 2:
                        sendBuffer = new byte[]{0x01, 00, 0x11, (byte) 0xA1, 0x03, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
                                0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xB7};
                        break;
                    //发送当前用户
                    case 3:
                        sendBuffer = new byte[]{0x01, 00, 0x11, (byte) 0xA1, 0x01, 0x01, 0x01, 0x19, (byte) 0xA6, 0x02, (byte) 0x8F,
                                0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x206};
                        break;
                }

//					byte[] sendBuffer =new byte[]{0x01,00,0x08,(byte)0xA1,0x01,0x01,0x01,0x12,(byte)0xA7,0x02,(byte)0x8F,
//							0,0,0,0,0,0,0,0,(byte)0xF7};

                Log.e(TAG, "-------Runnable:" );
                if (sendBuffer != null && mWriteCharacteristic != null) {
//					sendBuffer[5] = (byte)Integer.parseInt(Integer.toHexString(0 * 16+num),16);
                    mWriteCharacteristic.setValue(sendBuffer);
                    writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
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

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ALNewBluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(ALNewBluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ALNewBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ALNewBluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ALNewBluetoothLeService.ACTION_DATA_RSSI);
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
                    byte[] sendBuffer = new byte[]{0x01, 00, 0x11, (byte) 0xA1, 0x01, 0x01, 0x01, 0x19, (byte) 0xA6, 0x02, (byte) 0x8F,
                            0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x206};
                    mWriteCharacteristic.setValue(sendBuffer);
                    writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
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
}
