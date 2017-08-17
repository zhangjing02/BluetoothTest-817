package com.senssun.bluetooth.tools.weight;


import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.relative.Information;
import com.senssun.bluetooth.tools.relative.WheelViewSelect;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import widget.TosAdapterView;
import widget.WheelView;

public class TestWeightActivity extends Activity implements OnClickListener {

    private SharedPreferences mySharedPreferences;
    private int limitRssi;

    private BluetoothAdapter mBtAdapter;
    private boolean mScanning = true;
    private static final long SCAN_PERIOD = 200;
    private TextView device_name, device_address, mConnectionState, weightKG, currRssi;
    private TextView deviceModel;
    private SetData setData;
    private boolean connStatus;
    private Button btnBack;
    Dess mDess = new Dess();
    SetDess mSetDess = new SetDess();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = bluetoothManager.getAdapter();

        getActionBar().setTitle(R.string.menu_back_weight);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
        limitRssi=mySharedPreferences.getInt(Information.DB.LimitRssi, 0);

        setData = new SetData();
        init();
    }

    private void init() {
        device_name = (TextView) findViewById(R.id.device_name);
        device_address = (TextView) findViewById(R.id.device_address);
        deviceModel= (TextView) findViewById(R.id.deviceModel);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        weightKG = (TextView) findViewById(R.id.weightKG);
        currRssi = (TextView) findViewById(R.id.currRssi);
        btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(this);

        WheelView projectWheel =(WheelView) findViewById(R.id.projectWheel);
        WheelViewSelect.viewProjectNum(projectWheel, this);
        projectWheel.setOnItemSelectedListener(new TosAdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(TosAdapterView<?> parent, View view, int position, long id) {
                SharedPreferences.Editor editor = mySharedPreferences.edit();
                editor.putInt(Information.DB.LimitRssi, position + 40);
                editor.commit();
                limitRssi = position + 40;
            }

            public void onNothingSelected(TosAdapterView<?> parent) {

            }
        });
        projectWheel.setSelection(limitRssi - 40);


    }

    class Dess implements Runnable {
        @Override
        public void run() {
            while (mScanning) {
                mBtAdapter.startLeScan(mLeScanCallback);
                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mBtAdapter.stopLeScan(mLeScanCallback);
            }
        }
    }

    class SetDess implements Runnable {
        @Override
        public void run() {
            while (mScanning) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Message m = new Message();
                TestWeightActivity.this.setData.sendMessage(m);
                mBtAdapter.stopLeScan(mLeScanCallback);
            }
        }
    }

    class SetData extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (connStatus == true) {
                mConnectionState.setText(R.string.connected);
                mConnectionState.setBackgroundResource(R.color.conn);
            } else {
                mConnectionState.setText(R.string.disconnected);
                mConnectionState.setBackgroundResource(R.color.disconn);
            }
            connStatus = false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnBack:
                onBackPressed();
                break;
        }
    }

    // Device scan callback. �
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (device.getName() == null) return;
                            if (device.getName().trim().equals("IFit Scale") || device.getName().trim().equals("SENSSUN BODY") || device.getName().contains("EQi-99")) {
                                int targetRssi = 0;
                                try {
                                    targetRssi = limitRssi;
                                } catch (Exception e) {
                                    targetRssi = 0;
                                }
                                if (targetRssi < Math.abs(rssi)) {
                                    return;
                                }
                                if (device.getName().trim().equals("IFit Scale") || device.getName().trim().equals("SENSSUN BODY")) {
                                    deviceModel.setText("香山人体秤");
                                }else if(device.getName().contains("EQi-99")){
                                    deviceModel.setText("EQI人体秤");
                                }else{
                                    deviceModel.setText("其他厂家人体秤");
                                }


                                device_address.setText(device.getAddress());
                                device_name.setText(device.getName());
                                currRssi.setText(String.valueOf(rssi));

                                mConnectionState.setText(R.string.connected);
                                mConnectionState.setBackgroundResource(R.color.conn);

                                String testNum = String.format("%02X ", scanRecord[13]).trim() + String.format("%02X ", scanRecord[14]).trim();

                                if (String.format("%02X ", scanRecord[12]).trim().equals("AA")) {
                                    connStatus = true;

                                    int WeightNum = Integer.valueOf(testNum, 16);
                                    weightKG.setText(String.valueOf(WeightNum));
                                }
                            }
                        }
                    });
                }

            };

    private final BroadcastReceiver mGattUpdateReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
//            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
//                if (BluetoothAdapter.STATE_ON == mBtAdapter.getState()) {
//                    bluetoothSwitch.setChecked(true);
//                } else {
//                    bluetoothSwitch.setChecked(false);
//                }
//            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mScanning = true;
        Thread t1 = new Thread(mDess, "Window 1");
        t1.start();
        Thread t2 = new Thread(mSetDess, "Window 1");
        t2.start();
        registerReceiver(mGattUpdateReceive, makeUpdateIntentFilter());
    }

    ;

    @Override
    protected void onPause() {
        super.onPause();
        mScanning = false;
        if (mBtAdapter.isEnabled()) {
            mBtAdapter.stopLeScan(mLeScanCallback);
        }
        unregisterReceiver(mGattUpdateReceive);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        super.onMenuItemSelected(featureId, item);
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
        }
        return true;
    }

    private static IntentFilter makeUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return intentFilter;
    }
}
