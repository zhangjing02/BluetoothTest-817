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

package com.senssun.bluetooth.tools.eight_electrodes;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.entity.GattObject;

import java.math.BigDecimal;
import java.util.List;


public class TestEightFatActivity extends Activity implements OnClickListener {

    private TextView mConnectionState;
    private TextView weightKG;//weightLB
    private TextView leanNum, muscleNum, proteinNum, boneNum, cellNum, fatNum, fatPercentNum, neifatNum, bmiNum, caloriesNum,   bodyAge, healthGrade;

    public TextView currRssi, device_name;//device_address
    public EditText heightNum, ageNum;
    public CheckedTextView maleSelect, femaleSelect;
    public int OverRssi = 55;

    private BluetoothAdapter mBtAdapter = null;
    private TestEightFatBluetoothLeService mBluetoothLeService;
    private int weight = 0;

    // Code to manage Service lifecycle.
    public ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((TestEightFatBluetoothLeService.LocalBinder) service).getService();//
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TestEightFatBluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnectionState.setText(R.string.connected);
                mConnectionState.setBackgroundResource(R.color.conn);
//				device_address.setText(intent.getStringExtra(TestEightFatBluetoothLeService.EXTRA_ADDRESS));
                device_name.setText(intent.getStringExtra(TestEightFatBluetoothLeService.SEND_NAME));
            } else if (TestEightFatBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                    mConnectionState.setText(R.string.disconnected);
                    mConnectionState.setBackgroundResource(R.color.disconn);
//                    device_address.setText("");//intent.getStringExtra(BluetoothLeService.EXTRA_ADDRESS));
                    device_name.setText("");//intent.getStringExtra(BluetoothLeService.SEND_NAME));
            } else if (TestEightFatBluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
                displayData(intent.getStringExtra(TestEightFatBluetoothLeService.EXTRA_DATA));
//				device_address.setText(intent.getStringExtra(TestEightFatBluetoothLeService.EXTRA_ADDRESS));
                device_name.setText(intent.getStringExtra(TestEightFatBluetoothLeService.SEND_NAME));

                mConnectionState.setText(R.string.connected);
                mConnectionState.setBackgroundResource(R.color.conn);

            } else if (TestEightFatBluetoothLeService.ACTION_DATA_RSSI.equals(action)) {
                String currRssiStr = intent.getStringExtra(TestEightFatBluetoothLeService.SEND_RSSI);
                currRssi.setText(currRssiStr);
//                device_address.setText(intent.getStringExtra(TestEightFatBluetoothLeService.EXTRA_ADDRESS));
                device_name.setText(intent.getStringExtra(TestEightFatBluetoothLeService.SEND_NAME));
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_eightfat);
        getActionBar().setTitle(R.string.menu_back_eight_weight);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);//
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBtAdapter.isEnabled()) {
            mBtAdapter.enable();
        }
        // Sets up UI references.
//		device_address=(TextView) findViewById(R.id.device_address);
        device_name = (TextView) findViewById(R.id.device_name);
        mConnectionState = (TextView) findViewById(R.id.connection_state);

        weightKG = (TextView) findViewById(R.id.weightKG);
        leanNum = (TextView) findViewById(R.id.leanNum);
        muscleNum = (TextView) findViewById(R.id.muscleNum);
        proteinNum = (TextView) findViewById(R.id.proteinNum);
        boneNum = (TextView) findViewById(R.id.boneNum);
        cellNum = (TextView) findViewById(R.id.cellNum);
        fatNum = (TextView) findViewById(R.id.fatNum);
        fatPercentNum = (TextView) findViewById(R.id.fatPercentNum);
        neifatNum = (TextView) findViewById(R.id.neifatNum);
        bmiNum = (TextView) findViewById(R.id.bmiNum);
        caloriesNum = (TextView) findViewById(R.id.caloriesNum);
        bodyAge = (TextView) findViewById(R.id.bodyAge);
        healthGrade = (TextView) findViewById(R.id.healthGrade);

        currRssi = (TextView) findViewById(R.id.currRssi);
        heightNum = (EditText) findViewById(R.id.heightNum);
        ageNum = (EditText) findViewById(R.id.ageNum);
        maleSelect = (CheckedTextView) findViewById(R.id.maleSelect);
        femaleSelect = (CheckedTextView) findViewById(R.id.femaleSelect);


        Intent gattServiceIntent = new Intent(this, TestEightFatBluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        DessRssi();

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
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

    private void DessRssi() {
        new Thread() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    try {
                        mBluetoothLeService.GetRssi();
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
            }
        }.start();
    }

    //	private void updateConnectionState(final int resourceId) {
    //		runOnUiThread(new Runnable() {
    //			@Override
    //			public void run() {
    //				mConnectionState.setText(resourceId);
    //			}
    //		});
    //	}

    private void displayData(String data) {
        Log.e("TAG", "---------------data:" + data);
        if (data.length() > 8) {
            String[] strdata = data.split("-");
            //体重
            if ((strdata[6].equals("1A") || strdata[6].equals("10")) && strdata[1].equals("A5") && !(strdata[2].equals("31")) && !(strdata[2].equals("30")) && !(strdata[2].equals("10")) && !(strdata[2].equals("FF"))) {
                String tmpNum = strdata[2] + strdata[3];
                weight = Integer.valueOf(tmpNum, 16);
                weightKG.setText(new BigDecimal(Integer.valueOf(tmpNum, 16)).divide(new BigDecimal(10)).setScale(1, BigDecimal.ROUND_HALF_UP) + "kg");

//                int height = Integer.valueOf(heightNum.getText().toString());
//                BigDecimal tmpBig = new BigDecimal(height).multiply(new BigDecimal(height)).divide(new BigDecimal(1000));
//                if (weight == 0) {
//                    bmiNum.setText("0");
//                } else {
//                    bmiNum.setText(new BigDecimal(weight).divide(tmpBig, 1, BigDecimal.ROUND_HALF_UP).toString());
//                }

            }
            //瘦体重 蛋白质
            if (strdata[6].equals("B2")) {//
                String tmpNum = strdata[2] + strdata[3];
                leanNum.setText(new BigDecimal(Integer.valueOf(tmpNum)).divide(new BigDecimal(10)).setScale(1, BigDecimal.ROUND_HALF_UP) + "kg");

                tmpNum = strdata[4] + strdata[5];
                proteinNum.setText(new BigDecimal(Integer.valueOf(tmpNum)).divide(new BigDecimal(10)).setScale(1, BigDecimal.ROUND_HALF_UP) + "kg");
            }

            if (strdata[6].equals("C0")) {//肌肉百分比 肌肉重 骨骼重
                String tmpNum = strdata[2] + strdata[3];
//                musclePercentNum.setText(new BigDecimal(Integer.valueOf(tmpNum)).divide(new BigDecimal(10)).setScale(1, BigDecimal.ROUND_HALF_UP) + "%");
                muscleNum.setText(new BigDecimal(Integer.valueOf(tmpNum)).divide(new BigDecimal(10)).setScale(1, BigDecimal.ROUND_HALF_UP) + "kg");
//                muscleNum.setText("根据计算得出： "+new BigDecimal(weight).multiply(new BigDecimal(Integer.valueOf(tmpNum))).divide(new BigDecimal(10000)).setScale(1, BigDecimal.ROUND_HALF_UP) + "kg");

                tmpNum = strdata[5] + strdata[4];
                boneNum.setText(new BigDecimal(Integer.valueOf(tmpNum)).divide(new BigDecimal(10)).setScale(1, BigDecimal.ROUND_HALF_UP) + "kg");
//                boneNum.setText("根据计算得出： "+new BigDecimal(weight).multiply(new BigDecimal(Integer.valueOf(tmpNum))).divide(new BigDecimal(10000)).setScale(2, BigDecimal.ROUND_HALF_UP) + "kg");
            }

            //内脏脂肪 细胞总水
            if (strdata[6].equals("B1")) {//
                String tmpNum = strdata[2] + strdata[3];
                neifatNum.setText(new BigDecimal(Integer.valueOf(tmpNum)).divide(new BigDecimal(10)).setScale(1, BigDecimal.ROUND_HALF_UP).toString());

                tmpNum = strdata[4] + strdata[5];
                cellNum.setText(new BigDecimal(Integer.valueOf(tmpNum)).divide(new BigDecimal(10)).setScale(1, BigDecimal.ROUND_HALF_UP) + "kg");
            }

            //脂肪 水分
            if (strdata[6].equals("B0")) {//
                String tmpNum = strdata[2] + strdata[3];
                fatPercentNum.setText(new BigDecimal(Integer.valueOf(tmpNum)).divide(new BigDecimal(10)).setScale(1, BigDecimal.ROUND_HALF_UP) + "%");
                fatNum.setText("根据计算得出： "+new BigDecimal(weight).multiply(new BigDecimal(Integer.valueOf(tmpNum))).divide(new BigDecimal(10000)).setScale(1, BigDecimal.ROUND_HALF_UP) + "kg");
            }

            //基础代谢率
            if (strdata[6].equals("D0")) {
                String tmpNum = strdata[2] + strdata[3];
                caloriesNum.setText(String.valueOf(Integer.valueOf(tmpNum))+ "KCAL");

                tmpNum = strdata[4] + strdata[5];
                bmiNum.setText(new BigDecimal(Integer.valueOf(tmpNum)).divide(new BigDecimal(10)).setScale(1, BigDecimal.ROUND_HALF_UP).toString());
            }

//            try {
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }


            if (strdata[6].equals("B3")) {
                String tmpNum = strdata[3];
                bodyAge.setText(new BigDecimal(Integer.valueOf(tmpNum)).setScale(1, BigDecimal.ROUND_HALF_UP).toString());

                tmpNum = strdata[5];
                healthGrade.setText(new BigDecimal(Integer.valueOf(tmpNum)).setScale(1, BigDecimal.ROUND_HALF_UP).toString());
            }
//
//			if (strdata[1].equals("53")){
//				String tmpNum= strdata[2];
//				heartNum.setText(String.valueOf(Integer.valueOf(tmpNum,16)));
//
//			}
        }
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TestEightFatBluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(TestEightFatBluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(TestEightFatBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(TestEightFatBluetoothLeService.ACTION_DATA_NOTIFY);

        intentFilter.addAction(TestEightFatBluetoothLeService.SEND_ADDRESS);
        intentFilter.addAction(TestEightFatBluetoothLeService.SEND_RSSI);
        intentFilter.addAction(TestEightFatBluetoothLeService.SEND_NAME);


        intentFilter.addAction(TestEightFatBluetoothLeService.ACTION_DATA_RSSI);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return intentFilter;
    }

    byte[] byBuffer = new byte[12];
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.sendButton:
                calBuffer();
                mBluetoothLeService.WriteChar(byBuffer);
                break;
            case R.id.sendTestButton:
                calBuffer();
                byBuffer[1]++;
                byBuffer[7]++;
                mBluetoothLeService.WriteChar(byBuffer);
                break;
            case R.id.maleSelect:
                maleSelect.setChecked(true);
                femaleSelect.setChecked(false);
                break;
            case R.id.femaleSelect:
                maleSelect.setChecked(false);
                femaleSelect.setChecked(true);
                break;
//            case R.id.back:
//                onBackPressed();
//                break;
        }
    }

    private void calBuffer(){
        String byte0 = Long.toHexString(165).toUpperCase();
        byBuffer[0] = (byte) Integer.parseInt("BC", 16);

        int intByte1 = 16;
        String byte1 = Long.toHexString(intByte1).toUpperCase();
        byBuffer[1] = (byte) Integer.parseInt(byte1, 16);
        int sex;
        if (maleSelect.isChecked()) {
            sex = 1;
        } else {
            sex = 0;
        }

        int intByte2 = (sex == 0 ? 0 : 8) * 16 + 1;//Integer.valueOf(inputSerialNum.getText().toString());
        //				intByte2=1;
        String byte2 = Long.toHexString(intByte2).toUpperCase();
//							byte2="01";
        byBuffer[2] = (byte) Integer.parseInt(byte2, 16);

        int intByte3 = Integer.valueOf(ageNum.getText().toString());
        String byte3 = Long.toHexString(intByte3).toUpperCase();
        //				byte3="17";
        byBuffer[3] = (byte) Integer.parseInt(byte3, 16);

        int intByte4 = Integer.valueOf(heightNum.getText().toString());
        String byte4 = Long.toHexString(intByte4).toUpperCase();
        byBuffer[4] = (byte) Integer.parseInt(byte4, 16);


        String HeightTmp = String.valueOf(new BigDecimal(intByte4 / 0.254).setScale(0, BigDecimal.ROUND_HALF_UP));
        String Height = HeightTmp.substring(0, HeightTmp.length() - 1) + ((Integer.valueOf(HeightTmp.substring(HeightTmp.length() - 1)) >= 5 && Integer.valueOf(HeightTmp.substring(HeightTmp.length() - 1)) <= 9) ? "5" : "0");

        int intbyte5and6 = Integer.valueOf(Height);
        String byte5and6 = Long.toHexString(intbyte5and6).toUpperCase();
        byte5and6 = byte5and6.length() == 2 ? "00" + byte5and6 :
                byte5and6.length() == 3 ? "0" + byte5and6 : byte5and6;

        String byte5 = byte5and6.substring(0, 2).toUpperCase();
        byBuffer[5] = (byte) Integer.parseInt(byte5, 16);
        int intByte5 = Integer.valueOf(byte5, 16);//Integer.valueOf(byte3.substring(0,1))*16+Integer.valueOf(byte3.substring(1,2));

        String byte6 = byte5and6.substring(2, 4).toUpperCase();
        byBuffer[6] = (byte) Integer.parseInt(byte6, 16);
        int intByte6 = Integer.valueOf(byte6, 16);//Integer.valueOf(byte4.substring(0,1))*16+Integer.valueOf(byte4.substring(1,2));
        ////////////////////////////////////////////////////////

        int intbyte7 = intByte1 + intByte2 + intByte3 + intByte4 + intByte5 + intByte6;//+yingcun+yingcunDecimal;
        String byte7 = Long.toHexString(intbyte7).toUpperCase();
        byte7 = byte7.substring(byte7.length() - 2, byte7.length());
        //				byte7="6E";
        byBuffer[7] = (byte) Integer.parseInt(byte7, 16);
        //        		byBuffer= strInput.getBytes();

        byBuffer[8]=(byte) Integer.parseInt("D4", 16);
        byBuffer[9]=(byte) Integer.parseInt("C6", 16);
        byBuffer[10]=(byte) Integer.parseInt("C8", 16);
        byBuffer[11]=(byte) Integer.parseInt("D4", 16);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        super.onMenuItemSelected(featureId, item);
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
        }
        return true;
    }
}

