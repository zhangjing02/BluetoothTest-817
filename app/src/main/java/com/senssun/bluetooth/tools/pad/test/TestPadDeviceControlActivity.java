package com.senssun.bluetooth.tools.pad.test;

import android.app.Activity;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.entity.BluetoothDeviceObject;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;


public class TestPadDeviceControlActivity extends Activity {
    private static final String TAG = TestPadDeviceControlActivity.class.getSimpleName();

    private Context mContext;
    private boolean isCloseScreen = false;

    private TestPadBluetoothLeService mBluetoothLeService;

    private ScanService mScanService;

//    private String mDeviceAddress;
//    private String mDeviceName;

    private TextView tvState;
    private TextView tvDeviceName;
    private TextView tvDeviceAddress;
    private TextView tvConnectState;
    private TextView tvLog;
//    private Button btnMarkTime;

    private String mConnName="SENSSUN Pad";
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;


    public final static byte[] powerVolumnBuffer=new byte[]{(byte)0xA5,(byte)0x83,0,0,0,0,0,0x7C};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_pad_device_control);
        mContext = this;
        KeyguardManager mKeyguardManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
        isCloseScreen = mKeyguardManager.inKeyguardRestrictedInputMode();
        getActionBar().setTitle(R.string.menu_back);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        tvState = (TextView) findViewById(R.id.tvState);
        tvDeviceName = (TextView) findViewById(R.id.device_name);
        tvDeviceAddress = (TextView) findViewById(R.id.device_address);
        tvConnectState = (TextView) findViewById(R.id.connection_state);
        tvLog = (TextView) findViewById(R.id.tvLog);


        Intent gattServiceIntent = new Intent(this, TestPadBluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Intent scanServiceIntent = new Intent(this,ScanService.class);
        bindService(scanServiceIntent,mScanServiceConnection,BIND_AUTO_CREATE );
    }

    @Override
    protected void onResume() {
        Log.e(TAG,"------onResume");
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
//        if (mBluetoothLeService == null ) {
//            mBluetoothLeService.startScan();
//        }
        if(mBluetoothLeService != null){
            mBluetoothLeService.initialize();
        }

    }

    @Override
    protected void onPause() {
        Log.e(TAG, "------onPause");
        super.onPause();
        if(mRunnable != null && mHandler != null){
            mHandler.removeCallbacks(mRunnable);
            mRunnable = null;
            mHandler = null;
        }
        if(mBluetoothLeService != null) {
            mBluetoothLeService.clear();
        }
        changeConnectState(false,null,null);
        changeState("已断开连接");
        markLog("设备已断开");

        unregisterReceiver(mGattUpdateReceiver);

    }

    @Override
    protected void onRestart(){
        Log.e(TAG,"------onRestart");
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG,"------onDestroy");
        super.onDestroy();
        unbindService(mServiceConnection);
        unbindService(mScanServiceConnection);
        mBluetoothLeService = null;
        clearUI();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearUI(){
//        tvState.setText("");
        tvDeviceName.setText("");
        tvDeviceAddress.setText("");
        tvConnectState.setText(getResources().getString(R.string.disconnected));
        tvConnectState.setBackgroundColor(getResources().getColor(R.color.disconn));
    }

    private final ServiceConnection mScanServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mScanService = ((ScanService.LocalBinder) service).getService();
            if (!mScanService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            mBluetoothLeService = ((TestPadBluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mBluetoothAdapter = mBluetoothLeService.getBluetoothAdapter();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if(mBluetoothLeService.clear()){
                mBluetoothLeService = null;
            }
        }
    };

    private void changeState(final String strState){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvState.setText(strState);
            }
        });

    }

    private void markLog(final String strLog){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sDateFormat = new SimpleDateFormat("hh:mm:ss");
                String date = sDateFormat.format(new java.util.Date());
                String s = tvLog.getText().toString();
                if(s.equalsIgnoreCase(""))
                    tvLog.setText(s + strLog + " ("+date+")");
                else
                    tvLog.setText(s + "\n" + strLog + " ("+date+")");

            }
        });

    }

    private void changeConnectState(boolean isConnected, String deviceName,String deviceAddress){
        if(isConnected){
            tvDeviceName.setText(deviceName);
            tvDeviceAddress.setText(deviceAddress);
            tvConnectState.setText(getResources().getString(R.string.connected));
            tvConnectState.setBackgroundColor(getResources().getColor(R.color.conn));
        }else{
            clearUI();
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TestPadBluetoothLeService.ACTION_GATT_START_SCAN.equals(action)){
                changeState("正在搜索设备...");
                markLog("\n正在搜索设备...");
            }else if (TestPadBluetoothLeService.ACTION_GATT_SCAN_FOUND.equals(action)){
                markLog("已搜索到设备，正在连接...");
            }else if (TestPadBluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                changeConnectState(true, intent.getStringExtra("deviceName"), intent.getStringExtra("deviceAddress"));
                changeState("已连接");
                markLog("设备已连接");
            } else if (TestPadBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                if(mHandler != null && mRunnable != null){
                    mHandler.removeCallbacks(mRunnable);
                    mRunnable = null;
                    mHandler = null;
                    changeState("写入失败");
                    mBluetoothLeService.disconnect();
                }
                changeConnectState(false, null, null);
                changeState("已断开连接");
                markLog("设备已断开");
                if(mBluetoothLeService.clear()){
                    if(mBluetoothLeService.initialize()){
                        Log.e(TAG,"---------mBluetoothLeService.initialize() success");
                    }
                }
            } else if (TestPadBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (TestPadBluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(TestPadBluetoothLeService.EXTRA_DATA));
            }
        }
    };


    private void displayData(String data) {

        if (data != null) {
            String[] strdata=data.split("-");
            Log.e("Notification", data);
            if(mHandler != null && mRunnable != null){
                mHandler.removeCallbacks(mRunnable);
                mRunnable = null;
                mHandler = null;
                changeState("写入成功");
                markLog("写入成功");
                tvConnectState.setText("写入成功");
                tvConnectState.setBackgroundColor(getResources().getColor(R.color.pink));
//                mBluetoothLeService.disconnect();
            }
        }
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
                    if (characteristic.getUuid().toString().trim().equals("0000ffb2-0000-1000-8000-00805f9b34fb")){
                        mBluetoothLeService.setCharacteristicNotification(characteristic,true);
                        mNotifyCharacteristic = characteristic;
                    }
                    if (characteristic.getUuid().toString().trim().equals("0000ffb2-0000-1000-8000-00805f9b34fb")){
                        mWriteCharacteristic=characteristic;
                    }
                }
            }
            if (gattService.getUuid().toString().equals("0000fff0-0000-1000-8000-00805f9b34fb")) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
                    if (characteristic.getUuid().toString().trim().equals("0000fff1-0000-1000-8000-00805f9b34fb")){
                        mBluetoothLeService.setCharacteristicNotification(characteristic,true);
                        mNotifyCharacteristic = characteristic;
                    }
                    if (characteristic.getUuid().toString().trim().equals("0000fff2-0000-1000-8000-00805f9b34fb")){
                        mWriteCharacteristic=characteristic;
                    }
                }
            }

        }
        writeResult = false;
        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                if (mWriteCharacteristic != null) {
                    mWriteCharacteristic.setValue(powerVolumnBuffer);
                    mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                }
                Log.e(TAG, "----------writeResult:" + writeResult);
                mHandler.postDelayed(this,500);
            }
        };
        mHandler.postDelayed(mRunnable, 500);
        changeState("正在写入命令...");
        markLog("正在写入命令...");
    }

    private Handler mHandler;
    private Runnable mRunnable;
    boolean writeResult = false;
    boolean threadIsRunning = false;

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TestPadBluetoothLeService.ACTION_GATT_START_SCAN);
        intentFilter.addAction(TestPadBluetoothLeService.ACTION_GATT_SCAN_FOUND);
        intentFilter.addAction(TestPadBluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(TestPadBluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(TestPadBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(TestPadBluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(TestPadBluetoothLeService.ACTION_DATA_RSSI);
        return intentFilter;
    }

}
