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

package com.senssun.bluetooth.tools.aliweight_health_new;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class ALHealthNewDeviceControlActivity extends Activity implements OnClickListener {
    private final static String TAG = ALHealthNewDeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_MODEL = "DEVICE_MODEL";

    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mTimeField;
    private TextView mRssiField;
    private LinearLayout mResistance_show,mResistance_show2;
    private TextView mResistanceField,mResistanceField2;


    private String mDeviceName;
    private String mDeviceAddress;
    private ALHealthNewBluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private boolean mDess = true;
    private BluetoothGattCharacteristic mDataWriteCharacteristic;
    private BluetoothGattCharacteristic mTimeWriteCharacteristic;



//    private int modeAutoCheck;// 3 和 4选项
//
//	public final static byte[] sendBuffer = new byte[]{(byte) Integer.parseInt("A5", 16),
//		(byte) Integer.parseInt("5A", 16),0, 0, 0, 0, 0, 0, 0};//


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((ALHealthNewBluetoothLeService.LocalBinder) service).getService();
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
            if (ALHealthNewBluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (ALHealthNewBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
//                SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
//                if (mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 2) {//|| mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 3
//                    finish();
//                } else {
                mBluetoothLeService.close();
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
//                mBluetoothLeService.connect(mDeviceAddress);
//                }

            } else if (ALHealthNewBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (ALHealthNewBluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getByteArrayExtra(ALHealthNewBluetoothLeService.EXTRA_DATA), (UUID) intent.getSerializableExtra(ALHealthNewBluetoothLeService.EXTRA_UUID));
            } else if (ALHealthNewBluetoothLeService.ACTION_DATA_RSSI.equals(action)) {
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
        setContentView(R.layout.activity_ali_health_test_device_control);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mRssiField = (TextView) findViewById(R.id.rssi_value);
        mTimeField= (TextView) findViewById(R.id.time_value);
        mResistance_show= (LinearLayout) findViewById(R.id.resistance_show_layout);
        mResistance_show2= (LinearLayout) findViewById(R.id.resistance_show_layout2);
        mResistanceField= (TextView) findViewById(R.id.data_value2);
        mResistanceField2= (TextView) findViewById(R.id.data_value3);


        getActionBar().setTitle(R.string.menu_back);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.button1).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                onBackPressed();
            }
        });

        Intent gattServiceIntent = new Intent(this, ALHealthNewBluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        DessRssi();
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
        mDess = false;
        mBluetoothLeService.disconnect();
        mBluetoothLeService.close();
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


    private void displayData(byte[] data,UUID uuID) {
        StringBuffer stringBuffer = new StringBuffer();
        if (data != null) {
            for (byte byteChar : data) {
                String ms = String.format("%02X ", byteChar).trim()+"-";
                stringBuffer.append(ms);
            }
            Log.e("Mess",stringBuffer.toString());
            String[] displayData=stringBuffer.toString().split("-");
            if (data.length >= 0) {

                switch (uuID.toString()){

                    case "00002a9c-0000-1000-8000-00805f9b34fb":
                        switch (displayData[0]+displayData[1]){
                            case "0200":
                                Log.i("zhangjing",data[0]+"数据是？"+data[1]);
                                mTimeField.setText( Integer.valueOf(displayData[6],16)+"时"+
                                        Integer.valueOf(displayData[7],16)+"分"+
                                        Integer.valueOf(displayData[8],16)+"秒");

                                mResistance_show.setBackgroundResource(R.color.blankgray_opaque);
                                mResistance_show2.setBackgroundResource(R.color.blankgray_opaque);
                                mResistanceField.setText("**");
                                mResistanceField2.setText("**");

                                mDataField.setText(
                                       // "体重秤:"+
//                                        Integer.valueOf(displayData[3]+displayData[2],16)+"年"+
//                                        Integer.valueOf(displayData[4],16)+ "月"+
//                                        Integer.valueOf(displayData[5],16)+"日"+

                                 //       "电阻1："+String.format("%.1f",Integer.valueOf(displayData[10]+displayData[9],16)/10f)+
                                     //   "体重："+
                                                String.format("%.2f",Integer.valueOf(displayData[12]+displayData[11],16)/100f)+"kg");
//                                        "电阻2："+String.format("%.1f",Integer.valueOf(displayData[14]+displayData[13],16)/10f)+
                                      //  "\n");
                                mDataField.setTextColor(Color.BLUE);
                                break;
                            case "0603":
                                Log.i("zhangjing02",data[0]+"数据是？"+data[1]);
                                mTimeField.setText( Integer.valueOf(displayData[6],16)+"时"+
                                        Integer.valueOf(displayData[7],16)+"分"+
                                        Integer.valueOf(displayData[8],16)+"秒");

                                mResistanceField.setText(String.format("%.1f",Integer.valueOf(displayData[10]+displayData[9],16)/10f)+"Ω");

                                mDataField.setText(
                                        String.format("%.2f",Integer.valueOf(displayData[12]+displayData[11],16)/100f)+"kg");
//                                        "电阻2："+String.format("%.1f",Integer.valueOf(displayData[14]+displayData[13],16)/10f)+
                                      //  "\n");
                                mResistanceField.setTextColor(Color.BLUE);
                                mDataField.setTextColor(Color.BLUE);

                                mResistance_show2.setBackgroundResource(R.color.blankgray_opaque);
                                mResistanceField2.setText("**");
                                break;
                            case "0623":
                                Log.i("zhangjing03",data[0]+"数据是？"+data[1]);

                                mTimeField.setText( Integer.valueOf(displayData[6],16)+"时"+
                                        Integer.valueOf(displayData[7],16)+"分"+
                                        Integer.valueOf(displayData[8],16)+"秒");
                                mResistanceField.setText(String.format("%.1f",Integer.valueOf(displayData[10]+displayData[9],16)/10f)+"Ω");
                                mResistanceField2.setText(String.format("%.1f",Integer.valueOf(displayData[14]+displayData[13],16)/10f)+"Ω");

                                mDataField.setText(
                                        String.format("%.2f",Integer.valueOf(displayData[12]+displayData[11],16)/100f)+"kg");
//                                        "电阻2："+String.format("%.1f",Integer.valueOf(displayData[14]+displayData[13],16)/10f)+
                                      //  "\n");
                                mResistanceField.setTextColor(Color.BLUE);
                                mDataField.setTextColor(Color.BLUE);
                                mResistanceField2.setTextColor(Color.BLUE);

                                break;
                        }
                        break;
                    case "0000fa9c-0000-1000-8000-00805f9b34fb":
                        Log.i("zhangjing04",data[0]+"数据是？"+data[1]);

                        switch (displayData[0]+displayData[1]){
                            case "0200":
                                mDataField.setText(mDataField.getText()+
                                        "历史体重秤:"+
//                                        Integer.valueOf(displayData[3]+displayData[2],16)+"年"+
//                                        Integer.valueOf(displayData[4],16)+ "月"+
//                                        Integer.valueOf(displayData[5],16)+"日"+
                                        Integer.valueOf(displayData[6],16)+"时"+
                                        Integer.valueOf(displayData[7],16)+"分"+
                                        Integer.valueOf(displayData[8],16)+"秒"+
                                        "电阻1："+String.format("%.1f",Integer.valueOf(displayData[10]+displayData[9],16)/10f)+
                                        "体重："+String.format("%.2f",Integer.valueOf(displayData[12]+displayData[11],16)/100f)+
//                                        "电阻2："+String.format("%.1f",Integer.valueOf(displayData[14]+displayData[13],16)/10f)+
                                        "\n");
                                break;
                            case "0603":
                                Log.i("zhangjing05",data[0]+"数据是？"+data[1]);
                                mDataField.setText(mDataField.getText()+
                                        "历史体脂秤(一个电阻):"+
//                                        Integer.valueOf(displayData[3]+displayData[2],16)+"年"+
//                                        Integer.valueOf(displayData[4],16)+ "月"+
//                                        Integer.valueOf(displayData[5],16)+"日"+
                                        Integer.valueOf(displayData[6],16)+"时"+
                                        Integer.valueOf(displayData[7],16)+"分"+
                                        Integer.valueOf(displayData[8],16)+"秒"+
                                        "电阻1："+String.format("%.1f",Integer.valueOf(displayData[10]+displayData[9],16)/10f)+
                                        "体重："+String.format("%.2f",Integer.valueOf(displayData[12]+displayData[11],16)/100f)+
//                                        "电阻2："+String.format("%.1f",Integer.valueOf(displayData[14]+displayData[13],16)/10f)+
                                        "\n");
                                break;
                            case "0623":
                                Log.i("zhangjing06",data[0]+"数据是？"+data[1]);
                                mDataField.setText(mDataField.getText()+
                                        "历史体脂秤(两个电阻):"+
//                                        Integer.valueOf(displayData[3]+displayData[2],16)+"年"+
//                                        Integer.valueOf(displayData[4],16)+ "月"+
//                                        Integer.valueOf(displayData[5],16)+"日"+
                                        Integer.valueOf(displayData[6],16)+"时"+
                                        Integer.valueOf(displayData[7],16)+"分"+
                                        Integer.valueOf(displayData[8],16)+"秒"+
                                        "电阻1："+String.format("%.1f",Integer.valueOf(displayData[10]+displayData[9],16)/10f)+
                                        "体重："+String.format("%.2f",Integer.valueOf(displayData[12]+displayData[11],16)/100f)+
//                                        "电阻2："+String.format("%.1f",Integer.valueOf(displayData[14]+displayData[13],16)/10f)+
                                        "\n");
                                break;
                        }
                        break;
                    case "00002a08-0000-1000-8000-00805f9b34fb":
                        Log.i("zhangjing07",data[0]+"数据是？"+data[1]);
                       // mDataField.setText(mDataField.getText()+stringBuffer.toString()+"\n");
                        mDataField.setText(stringBuffer.toString()+"\n");
                        break;
                }
            }
        }





    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        Log.e(TAG, "-------Runnable:" );
        if (gattServices == null) return;

        for (BluetoothGattService gattService : gattServices) {
            String tmp = gattService.getUuid().toString();
            if (gattService.getUuid().toString().equals("0000181b-0000-1000-8000-00805f9b34fb")) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
                    if (characteristic.getUuid().toString().trim().equals("00002a9c-0000-1000-8000-00805f9b34fb")) {
                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                    }
                    if (characteristic.getUuid().toString().trim().equals("0000fa9c-0000-1000-8000-00805f9b34fb")) {
                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                    }
                    if (characteristic.getUuid().toString().trim().equals("00002a9c-0000-1000-8000-00805f9b34fb")) {
                        mDataWriteCharacteristic = characteristic;
                    }

                }
            }

            if (gattService.getUuid().toString().equals("00001805-0000-1000-8000-00805f9b34fb")) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
                    if (characteristic.getUuid().toString().trim().equals("00002a08-0000-1000-8000-00805f9b34fb")) {
                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                    }
                    if (characteristic.getUuid().toString().trim().equals("00002a08-0000-1000-8000-00805f9b34fb")) {
                        mTimeWriteCharacteristic = characteristic;
                    }
                }
            }
        }
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ALHealthNewBluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(ALHealthNewBluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ALHealthNewBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ALHealthNewBluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ALHealthNewBluetoothLeService.ACTION_DATA_RSSI);
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
                if (mTimeWriteCharacteristic != null) {
                    byte[] sendBuffer = new byte[7];
                    Calendar cal=Calendar.getInstance();

                    int yearl=cal.get(Calendar.YEAR);
//                    byte bYear=(byte) Integer.parseInt(String.valueOf(yearl), 16);
//                    String mYear = String.format("%02X", Integer.parseInt(String.valueOf(yearl), 16)).trim();
                    String mYear=Long.toHexString(cal.get(Calendar.YEAR));

                    Log.e("Mess",mYear+String.valueOf(cal.get(Calendar.YEAR)).substring(0,2)+"."+
                            String.valueOf(cal.get(Calendar.YEAR)).substring(2,4)+"."+
                            String.valueOf(cal.get(Calendar.MONTH)+1)+"."+
                            String.valueOf(cal.get(Calendar.DAY_OF_MONTH))+"."+
                            String.valueOf(cal.get(Calendar.HOUR_OF_DAY))+"."+
                            String.valueOf(cal.get(Calendar.MINUTE))+"."+
                            String.valueOf(cal.get(Calendar.SECOND)));


                    mYear=mYear.length()==1?"000"+mYear:
                            mYear.length()==2?"00"+mYear:
                                    mYear.length()==3?"0"+mYear:mYear;


                    sendBuffer[1]=(byte) Integer.parseInt(mYear.substring(0,2), 16);
                    sendBuffer[0]=(byte) Integer.parseInt(mYear.substring(2,4), 16);
                    sendBuffer[2]=(byte) Integer.parseInt(Long.toHexString(cal.get(Calendar.MONTH)+1), 16);
                    sendBuffer[3]=(byte) Integer.parseInt(Long.toHexString(cal.get(Calendar.DAY_OF_MONTH)), 16);
                    sendBuffer[4]=(byte) Integer.parseInt(Long.toHexString(cal.get(Calendar.HOUR_OF_DAY)), 16);
                    sendBuffer[5]=(byte) Integer.parseInt(Long.toHexString(cal.get(Calendar.MINUTE)), 16);
                    sendBuffer[6]=(byte) Integer.parseInt(Long.toHexString(cal.get(Calendar.SECOND)), 16);

                    StringBuffer stringBuffer = new StringBuffer();

                    for (byte byteChar : sendBuffer) {
                        String ms = String.format("%02X ", byteChar).trim()+"-";
                        stringBuffer.append(ms);
                    }
                    Log.e("Mess",stringBuffer.toString());
                        mTimeWriteCharacteristic.setValue(sendBuffer);
                        mBluetoothLeService.writeCharacteristic(mTimeWriteCharacteristic);

                }
                break;
        }
    }
}
