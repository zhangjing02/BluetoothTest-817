package com.senssun.bluetooth.tools.alarmFat;

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
import android.app.Activity;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.senssun.bluetooth.tools.BluetoothBuffer;
import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.relative.Information;

import java.util.ArrayList;
import java.util.List;

public class AlarmFatDeviceControlActivity extends Activity implements View.OnClickListener {
    private final static String TAG = AlarmFatDeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private Context mContext;
    private CheckedTextView checkAlarmValue,checkFatValue,checkClearValue;

    private TextView mConnectionState;
    private TextView mDataField,mDatFataField;
    private TextView mRssiField;
    private String mDeviceName;
    private String mDeviceAddress;
    private AlarmFatBluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private boolean mAlarmChceked=false,mFatSendChceked=false,mFatChceked=false,mClearChceked=false;
    private boolean misSend=false;

    private SharedPreferences mySharedPreferences;


    private EditText etRingNum,etAge,etHeight,etUserNum;
    private RadioButton rbMale,rbFemale;
    private Button btnEdit;
    private boolean isEditAble = false;

    private Handler mSendDataHandler;
    private List<byte[]> mSendDataList=new ArrayList<byte[]>();


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((AlarmFatBluetoothLeService.LocalBinder) service).getService();
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
            if (BluetoothBuffer.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothBuffer.ACTION_GATT_DISCONNECTED.equals(action)) {
                misSend=false;
                mSendDataList.clear();
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
                SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
                if(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
                    onBackPressed();
                }
            } else if (BluetoothBuffer.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothBuffer.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothBuffer.EXTRA_DATA));
            }else if (BluetoothBuffer.ACTION_DATA_RSSI.equals(action)) {
                displayRssi(intent.getStringExtra("Rssi"));
            }
        }
    };


    private void clearUI() {
        mDataField.setText(R.string.no_data);
        mDataField.setTextColor(Color.BLACK);
        mDatFataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_fat_device_control);
        mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);

        mContext = this;
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mDatFataField= (TextView) findViewById(R.id.fat_value);
        mRssiField= (TextView) findViewById(R.id.rssi_value);
        checkAlarmValue= (CheckedTextView) findViewById(R.id.checkAlarmValue);
        checkFatValue= (CheckedTextView) findViewById(R.id.checkFatValue);
        checkClearValue = (CheckedTextView) findViewById(R.id.checkClearValue);


        etRingNum = (EditText)findViewById(R.id.etRingNum);
        etRingNum.setText(String.valueOf(mySharedPreferences.getInt(Information.DB.RING_NUM,0)));

        etAge= (EditText)findViewById(R.id.etAge);
        etHeight= (EditText)findViewById(R.id.etHeight);
        etUserNum= (EditText)findViewById(R.id.etUserNum);
        rbMale = (RadioButton)findViewById(R.id.rbMale);
        rbFemale = (RadioButton)findViewById(R.id.rbFemale);

        btnEdit = (Button)findViewById(R.id.btnEdit);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        btnEdit.setText("编辑");
        etRingNum.setEnabled(false);
        etAge.setEnabled(false);
        etHeight.setEnabled(false);
        etUserNum.setEnabled(false);
        rbMale.setEnabled(false);
        rbFemale.setEnabled(false);

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEditAble) {
                    isEditAble = false;
                    btnEdit.setText("编辑");
                    etRingNum.setEnabled(false);
                    etAge.setEnabled(false);
                    etHeight.setEnabled(false);
                    etUserNum.setEnabled(false);
                    rbMale.setEnabled(false);
                    rbFemale.setEnabled(false);

                    SharedPreferences.Editor editor = mySharedPreferences.edit();
                    editor.putInt(Information.DB.RING_NUM, Integer.valueOf(etRingNum.getText().toString()));
                    editor.putInt(Information.DB.USER_NUM, Integer.valueOf(etUserNum.getText().toString()));
                    editor.putInt(Information.DB.USER_AGE, Integer.valueOf(etAge.getText().toString()));
                    editor.putInt(Information.DB.USER_HEIGHT, Integer.valueOf(etHeight.getText().toString()));
                    if(rbMale.isChecked()){
                        editor.putInt(Information.DB.USER_SEX,1);
                    }else{
                        editor.putInt(Information.DB.USER_SEX,2);
                    }
                    editor.commit();
                } else {
                    isEditAble = true;
                    btnEdit.setText("保存");
                    etRingNum.setEnabled(true);
                    etAge.setEnabled(true);
                    etHeight.setEnabled(true);
                    rbMale.setEnabled(true);
                    rbFemale.setEnabled(true);
                    etUserNum.setEnabled(true);
                    clearUI();
                }
            }
        });

//        if (!mChceked) {
//            mWriteCharacteristic.setValue(calSendBuffer());
//            boolean writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
//        }

        getActionBar().setTitle(R.string.menu_back);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                onBackPressed();
            }
        });


        Intent gattServiceIntent = new Intent(this, AlarmFatBluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        mSendDataHandler=new Handler();
        mSendData();

    }

    private void mSendData(){
        mSendDataHandler.postDelayed(mSendRunnable, 500);
    }
    Runnable mSendRunnable=	new Runnable() {
        @Override
        public void run() {
            if(mSendDataList.size()!=0&&misSend&&mWriteCharacteristic!=null){
                byte[] outBuffer=mSendDataList.get(0);
                StringBuilder stringBuilder = new StringBuilder(outBuffer.length);
                for(byte byteChar : outBuffer){
                    String ms=String.format("%02X ", byteChar).trim();
                    stringBuilder.append(ms+"-");
                }
                Log.d(TAG, "发送1：" + stringBuilder.toString());

                mWriteCharacteristic.setValue(outBuffer);
                mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
            }
            mSendData(); //不断发送
        }
    };

    private byte[] SendTestAlarmBuffer(){
        int ringNum = Integer.valueOf(etRingNum.getText().toString());
        byte[] outBuffer=BluetoothBuffer.SendAlarmBuffer;
        outBuffer[4] = (byte) Integer.parseInt(Integer.toHexString(ringNum),16);
        String strSum = Integer.toHexString((outBuffer[1] & 0xff)
                + (outBuffer[2] & 0xff) + (outBuffer[3] & 0xff)
                + (outBuffer[4] & 0xff) + (outBuffer[5] & 0xff) + (outBuffer[6] & 0xff));
        if(strSum.length() > 2){
            strSum = strSum.substring(strSum.length()-2,strSum.length());
        }else{
            strSum = "0"+strSum;
        }
        outBuffer[7] = (byte)Integer.parseInt(strSum,16);
        return outBuffer;
    }


    private byte[] SendTestFatBuffer(){
        int etAgeNum=Integer.valueOf(etAge.getText().toString().trim());
        int etHeightNum=Integer.valueOf(etHeight.getText().toString().trim());
        int etUser=Integer.valueOf(etUserNum.getText().toString().trim());
        boolean rbSex=rbMale.isChecked();

        byte[] outBuffer=BluetoothBuffer.SendBodyBuffer;
        outBuffer[3] = (byte) Integer.parseInt(Integer.toHexString(etAgeNum), 16);
        outBuffer[4] = (byte) Integer.parseInt(Integer.toHexString(etHeightNum), 16);
        int userNum = etUser;
        outBuffer[2] = (byte) Integer.parseInt(Integer.toHexString((rbSex?8:0)*16+userNum),16);
//					if (rbMale.isChecked()) {
//						sendBuffer[2] = (byte) Integer.parseInt(Integer.toHexString(81), 16);
//					} else {
//						sendBuffer[2] = (byte) Integer.parseInt(Integer.toHexString(1), 16);
//					}
        String strSum = Integer.toHexString((outBuffer[1] & 0xff)
                + (outBuffer[2] & 0xff) + (outBuffer[3] & 0xff)
                + (outBuffer[4] & 0xff) + (outBuffer[5] & 0xff) + (outBuffer[6] & 0xff));
        if(strSum.length() > 2){
            strSum = strSum.substring(strSum.length()-2,strSum.length());
        }else{
            strSum = "0"+strSum;
        }
        outBuffer[7] = (byte)Integer.parseInt(strSum,16);
        return outBuffer;
    }

    private byte[] SendTestClearBuffer(){
        byte[] outBuffer=BluetoothBuffer.ClearDataBuffer;
        return outBuffer;
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
        mSendDataHandler.removeCallbacks(mSendRunnable);
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
        switch(item.getItemId()) {
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
        if (data != null) {
            misSend=true;
            Log.e("displayData",data);
            String[] strdata=data.split("-");
            if(strdata.length>=8) {
                String tmpNum;
                int number;
                float fNumber;
                switch(strdata[6]){
                    case "A0":
                        tmpNum = strdata[2] + strdata[3];
                        number = Integer.valueOf(tmpNum, 16);
                        fNumber = number / 10f;
                        mDataField.setText(String.valueOf(fNumber) + "kg");
                        mDataField.setTextColor(Color.BLACK);
                        break;
                    case "AA":
                        tmpNum = strdata[2] + strdata[3];
                        number = Integer.valueOf(tmpNum, 16);
                        fNumber = number / 10f;
                        mDataField.setText(String.valueOf(fNumber) + "kg");
                        mDataField.setTextColor(Color.GREEN);
                        break;
                    case "B0":
                        checkFatValue.setChecked(true);
                        mFatChceked=true;

                        tmpNum = strdata[2] + strdata[3];
                        number = Integer.valueOf(tmpNum, 16);
                        fNumber = number / 10f;
                        mDatFataField.setText(" 脂肪:"+String.valueOf(fNumber) + "%");

                        break;
                    default:
                        break;
                }

                if (strdata[2].equals("10")){
                    if(mSendDataList.size()!=0){
                        byte[] outBuffer=mSendDataList.get(0);

                        if (String.format("%02X ", outBuffer[1]).trim().equals("10")){
                            mSendDataList.remove(outBuffer);
                        }
                    }
                }


                if (data.equals("FF-A5-03-00-00-00-00-03-")){
                    if(mSendDataList.size()!=0){
                        byte[] outBuffer=mSendDataList.get(0);

                        if (String.format("%02X ", outBuffer[1]).trim().equals("50")){
                            mSendDataList.remove(outBuffer);
                        }
                    }
                }

                if (strdata[2].equals("32")&&strdata[3].equals("55")){
                    if(mSendDataList.size()!=0){
                        byte[] outBuffer=mSendDataList.get(0);

                        if (String.format("%02X ", outBuffer[1]).trim().equals("32")){
                            mSendDataList.remove(outBuffer);
                        }
                    }
                }

                if(data.equals("FF-A5-03-00-00-00-00-03-")){
                    checkClearValue.setChecked(true);
                    mClearChceked=true;
                }
                if(strdata[2].equals("32")&&strdata[3].equals("55")){
                    checkAlarmValue.setChecked(true);
                    mAlarmChceked=true;
                }
                SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
                if(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {

                    if(mClearChceked&&mAlarmChceked&&mFatChceked){
                        Intent intent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putString("BleSuc", mDeviceAddress);
                        intent.putExtras(bundle);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
//                    mChceked = true;
//                    mWriteCharacteristic.setValue(calSendBuffer());
//                    boolean writeResult = false;
//                    while(!writeResult){
//                        writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
//                    }
//                    Intent intent = new Intent();
//                    Bundle bundle = new Bundle();
//                    bundle.putString("BleSuc", mDeviceAddress);
//                    intent.putExtras(bundle);
//                    setResult(RESULT_OK, intent);
//                    finish();
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
                    if (characteristic.getUuid().toString().trim().equals("0000ffb2-0000-1000-8000-00805f9b34fb")){
                        mBluetoothLeService.setCharacteristicNotification(characteristic,true);
                        mNotifyCharacteristic = characteristic;
                    }
                    if (characteristic.getUuid().toString().trim().equals("0000ffb2-0000-1000-8000-00805f9b34fb")){
                        mWriteCharacteristic=characteristic;

                        if(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
                            mSendDataList.add(SendTestClearBuffer());
                            mSendDataList.add(SendTestFatBuffer());
                            mSendDataList.add(SendTestAlarmBuffer());
                        }
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

                        if(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
                            mSendDataList.add(SendTestClearBuffer());
                            mSendDataList.add(SendTestFatBuffer());
                            mSendDataList.add(SendTestAlarmBuffer());
                        }
                    }
                }
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothBuffer.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothBuffer.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothBuffer.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothBuffer.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothBuffer.ACTION_DATA_RSSI);
        return intentFilter;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.SendAlarm:{
                if(mWriteCharacteristic!=null) {
                    mSendDataList.add(SendTestAlarmBuffer());
//                    mWriteCharacteristic.setValue(SendTestAlarmBuffer());
//                    boolean writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                }
            }
            break;
            case R.id.SendFat:{
                if(mWriteCharacteristic!=null) {
                    mSendDataList.add(SendTestFatBuffer());
//                    mWriteCharacteristic.setValue(SendTestFatBuffer());
//                    boolean writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                }
            }
            break;
            case R.id.SendClear:{
                if(mWriteCharacteristic!=null){
                    mSendDataList.add(SendTestClearBuffer());
//                    mWriteCharacteristic.setValue(SendTestClearBuffer());
//                    boolean writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                }

            }
            break;
        }
    }
}
