package com.senssun.bluetooth.tools.mdweight;

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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.application.MyApp;
import com.senssun.bluetooth.tools.relative.Information;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import widget.BorderTextView;

public class MDDeviceControlActivity02 extends Activity {
    private final static String TAG = MDDeviceControlActivity02.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_STANDARD_FAT = "STANDARD_FAT";

    private Context mContext;
    private CheckedTextView mCheckField;
    private TextView mConnectionState;
    private TextView mDataField, bmi;
    private TextView mRssiField;
    private String mDeviceName;
    private String mDeviceAddress;
    private MDBluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private boolean mChceked = false;
    private LinearLayout mFrame_layout;
    private String standard_fats;
    private BorderTextView mSet,mSex_set,mAge_set,mHeight_set;
    private SharedPreferences sp = MyApp.getContext().getSharedPreferences("base_fat", MODE_PRIVATE);
    private SharedPreferences.Editor editor = sp.edit();

    //默认165cm 25year female
    public final static byte[] sendBuffer = new byte[]{(byte) 0xaa, (byte) 0x55, (byte) 0x0a, (byte) 0x01, (byte) 0x01,
            (byte) 0xa5, (byte) 0x19, (byte) 0x00, (byte) 0x00, (byte) 0x00};//

    public final static byte[] closeDevice = new byte[]{(byte) 0xaa, (byte) 0x55, (byte) 0x07, (byte) 0x01, (byte) 0x02,
            0};

    private EditText etHeight, etAge;
    private RadioButton rbMale, rbFemale;
    private Button btnEdit;
    private boolean isEditAble = false;

    private Handler mHandler;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((MDBluetoothLeService.LocalBinder) service).getService();
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
            if (MDBluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (MDBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
//                clearUI();
                SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
                if (mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
                  //  onBackPressed();
                    //连接失败，尝试重新连接而不是退出
                    mBluetoothLeService.connect(mDeviceAddress);

                }
            } else if (MDBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (MDBluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(MDBluetoothLeService.EXTRA_DATA));
            } else if (MDBluetoothLeService.ACTION_DATA_RSSI.equals(action)) {
                displayRssi(intent.getStringExtra("Rssi"));
            }
        }
    };


    private void clearUI() {
        mDataField.setTextColor(Color.BLACK);
        ((TextView) findViewById(R.id.mTv1)).setText("");
        ((TextView) findViewById(R.id.mTv2)).setText("");
        ((TextView) findViewById(R.id.mTv3)).setText("");
        ((TextView) findViewById(R.id.mTv4)).setText("");
        ((TextView) findViewById(R.id.mTv5)).setText("");
        ((TextView) findViewById(R.id.mTv6)).setText("");
        ((TextView) findViewById(R.id.mTv7)).setText("");
        ((TextView) findViewById(R.id.mTv8)).setText("");
        ((TextView) findViewById(R.id.mTv9)).setText("");
        ((TextView) findViewById(R.id.mTv10)).setText("");
        ((TextView) findViewById(R.id.mTv11)).setText("");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_md_device_control02);
       // Log.i("haojiahuo","进来了么？" +sp.getString("myBaseFat",""));
        String mBase_fat=sp.getString("myBaseFat","");

        mContext = this;
        mHandler = new Handler();
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        standard_fats = mBase_fat;


        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        bmi = (TextView) findViewById(R.id.mTv11);
        mRssiField = (TextView) findViewById(R.id.rssi_value);
        mCheckField = (CheckedTextView) findViewById(R.id.check_value);

        etAge = (EditText) findViewById(R.id.etAge);
        etHeight = (EditText) findViewById(R.id.etHeight);
        rbMale = (RadioButton) findViewById(R.id.rbMale);
        rbFemale = (RadioButton) findViewById(R.id.rbFemale);
        btnEdit = (Button) findViewById(R.id.btnEdit);
        mSet = (BorderTextView) findViewById(R.id.mSet);
        mSex_set= (BorderTextView) findViewById(R.id.sex_set);
        mAge_set= (BorderTextView) findViewById(R.id.age_set);
        mHeight_set= (BorderTextView) findViewById(R.id.height_set);


        mSet.setText(mBase_fat+"%"+"（脂肪率）");
//        if (standard_fats!=null){
//            mSet.setText(mBase_fat+"%"+"（脂肪率）");
//        }
        mFrame_layout = (LinearLayout) findViewById(R.id.frame_layout);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        btnEdit.setText("编辑");
        etAge.setEnabled(false);
        etHeight.setEnabled(false);
       // rbMale.setChecked(false);
        rbMale.setEnabled(false);
       // rbFemale.setChecked(true);
        rbFemale.setEnabled(false);
        if (sp!=null){
            boolean isMale= sp.getBoolean("sex",false);
            if (isMale){
                rbMale.setChecked(true);
                rbFemale.setEnabled(false);
                mSex_set.setText("男");
            }else {
                rbMale.setChecked(false);
                rbFemale.setEnabled(true);
                mSex_set.setText("女");
            }
            String age=sp.getString("age","25");
            etAge.setText(age);
            mAge_set.setText(age+"岁");
            String height=sp.getString("height","165");
            etHeight.setText(height);
            mHeight_set.setText(height+"cm");
        }




        rbMale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rbFemale.setChecked(false);
                rbMale.setChecked(true);
            }
        });
        rbFemale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rbMale.setChecked(false);
                rbFemale.setChecked(true);
            }
        });

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEditAble) {
                    isEditAble = false;
                    btnEdit.setText("编辑");
                    etAge.setEnabled(false);
                    etHeight.setEnabled(false);
                    rbMale.setEnabled(false);
                    rbFemale.setEnabled(false);
                    sendBuffer[6] = (byte) Integer.parseInt(Integer.toHexString(Integer.valueOf(etAge.getText().toString().trim())), 16);
                    sendBuffer[5] = (byte) Integer.parseInt(Integer.toHexString(Integer.valueOf(etHeight.getText().toString().trim())), 16);
                    if (rbMale.isChecked()) {
                        sendBuffer[7] = (byte) Integer.parseInt(Integer.toHexString(1), 16);
                        editor.putBoolean("sex",true);
                    } else {
                        sendBuffer[7] = (byte) Integer.parseInt(Integer.toHexString(0), 16);
                        editor.putBoolean("sex",false);
                    }

                    if (!mChceked) {
                        if (mWriteCharacteristic != null) {
                            mWriteCharacteristic.setValue(sendBuffer);
                            boolean writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                        }
                    }
                    editor.putString("age",etAge.getText().toString());
                    editor.putString("height",etHeight.getText().toString());
                    editor.commit();



                } else {
                    isEditAble = true;
                    btnEdit.setText("确认");
                    etAge.setEnabled(true);
                    etHeight.setEnabled(true);
                    rbMale.setEnabled(true);
                    rbFemale.setEnabled(true);
                    clearUI();
                }
            }
        });

        getActionBar().setTitle(R.string.menu_back);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                onBackPressed();
            }
        });

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWriteCharacteristic != null) {
                    mWriteCharacteristic.setValue(closeDevice);
                    if (mBluetoothLeService.writeCharacteristic(mWriteCharacteristic)) {
                        Toast.makeText(mContext, "关闭设备成功，正在返回搜索页面...", Toast.LENGTH_SHORT).show();
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                onBackPressed();
                            }
                        }, 1500);
                    } else {
                        Toast.makeText(mContext, "关闭设备失败！", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        Intent gattServiceIntent = new Intent(this, MDBluetoothLeService.class);
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
            Log.e("Notification", data);
            String[] strdata = data.split("-");
//            //新协议-头的部分
//            if (strdata[0].equals("10")) {
//                data = data.substring(18, data.length());
//                strdata = data.split("-");
//                Log.e("Notification", "分离完后的数据" + data);
//            }
            if (strdata.length >= 8) {
                if (strdata[3].equalsIgnoreCase("01") && strdata[4].equalsIgnoreCase("FE")) {
                    String tmpNum = strdata[6] + strdata[7];
                    String unit = "kg";
                    switch (strdata[5]) {
                        case "00":
                            unit = "kg";
                            break;
                        case "01":
                            unit = "斤";
                            break;
                        case "02":
                            unit = "lb";
                            break;
                        default:
                            break;
                    }
                    int weight = Integer.valueOf(tmpNum, 16);

                    float fWeight = weight / 10f;
                    mDataField.setText(String.valueOf(fWeight) + unit);
                    bmi.setText(CountBmi(fWeight, Float.valueOf(etHeight.getText().toString())).setScale(1, BigDecimal.ROUND_HALF_UP) + "");
                    if (strdata[8].equalsIgnoreCase("AA")) {
                        mDataField.setTextColor(Color.BLUE);
                    } else
                        mDataField.setTextColor(Color.BLACK);
                    if (!mChceked) {
                        mWriteCharacteristic.setValue(sendBuffer);
                        boolean writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);

                    }
                }
                if (strdata[3].equalsIgnoreCase("01") && strdata[4].equalsIgnoreCase("FD")) {
                    mCheckField.setChecked(true);
                    //脂肪
                    float fat = Integer.valueOf(strdata[7] + strdata[8], 16);
                    float fat2=fat/10;

                    ((TextView) findViewById(R.id.mTv1)).setText(String.valueOf(fat2) + "%");
                    ((TextView) findViewById(R.id.mTv2)).setText(String.valueOf((float) (Integer.valueOf(strdata[9] + strdata[10], 16)) / 10));
                    ((TextView) findViewById(R.id.mTv3)).setText(String.valueOf((float) (Integer.valueOf(strdata[11] + strdata[12], 16)) / 10));
                    ((TextView) findViewById(R.id.mTv4)).setText(String.valueOf((float) (Integer.valueOf(strdata[13] + strdata[14], 16)) / 10));
                    ((TextView) findViewById(R.id.mTv5)).setText(String.valueOf(Integer.valueOf(strdata[15] + strdata[16], 16)));

                    if (standard_fats != null) {
                        float standard_fat = Float.parseFloat(standard_fats);

                        if (fat2 >= standard_fat - 0.5 && fat2 <= standard_fat + 0.5) {
                            Log.i(TAG, "脂肪在正常范围内");
                            mFrame_layout.setBackgroundResource(R.color.mintcream);
                            if (strdata[16]!=null){
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        onBackPressed();
                                    }
                                }, 1500);
                            }

                        } else {
                            Log.i(TAG, "脂肪超出范围");
                            mFrame_layout.setBackgroundResource(R.color.salmon);
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    onBackPressed();
                                }
                            }, 3000);
                        }
                    }else {
                        standard_fats="0";
                    }


//                    ((TextView) findViewById(R.id.mTv6)).setText(String.valueOf(Integer.valueOf(strdata[17] + strdata[18], 16)));
//                    ((TextView) findViewById(R.id.mTv7)).setText(String.valueOf(Integer.valueOf(strdata[19] + strdata[20], 16)));
//                    ((TextView) findViewById(R.id.mTv8)).setText(String.valueOf(Integer.valueOf(strdata[21] + strdata[22], 16)));
//                    ((TextView) findViewById(R.id.mTv9)).setText(String.valueOf(Integer.valueOf(strdata[23] + strdata[24], 16)));
//                    ((TextView) findViewById(R.id.mTv10)).setText(String.valueOf(Integer.valueOf(strdata[25] + strdata[26], 16)));
//                    ((TextView) findViewById(R.id.mTv11)).setText(String.valueOf(Integer.valueOf(strdata[15] + strdata[16], 16) * 1.35));
                }
                if (strdata[3].equalsIgnoreCase("01") && strdata[4].equalsIgnoreCase("FA")) {
                    mCheckField.setChecked(true);
                    ((TextView) findViewById(R.id.mTv6)).setText(String.valueOf((float) (Integer.valueOf(strdata[5] + strdata[6], 16)) / 10));
                    ((TextView) findViewById(R.id.mTv7)).setText(String.valueOf((float) (Integer.valueOf(strdata[7] + strdata[8], 16)) / 10));
                    ((TextView) findViewById(R.id.mTv8)).setText(String.valueOf((float) (Integer.valueOf(strdata[9] + strdata[10], 16)) / 10));
                    ((TextView) findViewById(R.id.mTv9)).setText(String.valueOf((Integer.valueOf(strdata[11], 16))));
                    ((TextView) findViewById(R.id.mTv10)).setText(String.valueOf(Integer.valueOf(strdata[12], 16)));
//                    ((TextView) findViewById(R.id.mTv6)).setText(String.valueOf(Integer.valueOf(strdata[17] + strdata[18], 16)));
//                    ((TextView) findViewById(R.id.mTv7)).setText(String.valueOf(Integer.valueOf(strdata[19] + strdata[20], 16)));
//                    ((TextView) findViewById(R.id.mTv8)).setText(String.valueOf(Integer.valueOf(strdata[21] + strdata[22], 16)));
//                    ((TextView) findViewById(R.id.mTv9)).setText(String.valueOf(Integer.valueOf(strdata[23] + strdata[24], 16)));
//                    ((TextView) findViewById(R.id.mTv10)).setText(String.valueOf(Integer.valueOf(strdata[25] + strdata[26], 16)));
//                    ((TextView) findViewById(R.id.mTv11)).setText(String.valueOf(Integer.valueOf(strdata[27] + strdata[28], 16)));
                }
                SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
                if (mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
                    Intent intent = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putString("BleSuc", mDeviceAddress);
                    intent.putExtras(bundle);
                    setResult(RESULT_OK, intent);
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

            if (gattService.getUuid().toString().equalsIgnoreCase("0000ffb0-0000-1000-8000-00805f9b34fb")) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
                    if (characteristic.getUuid().toString().trim().equalsIgnoreCase("0000ffb2-0000-1000-8000-00805f9b34fb")) {
                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                        mNotifyCharacteristic = characteristic;
                    }
                    if (characteristic.getUuid().toString().trim().equalsIgnoreCase("0000ffb2-0000-1000-8000-00805f9b34fb")) {
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
                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                        mWriteCharacteristic = characteristic;
                    }
                }
            }
//            if (gattService.getUuid().toString().equals("0000fff3-0000-1000-8000-00805f9b34fb")) {
//                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
//                for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
//                    if (characteristic.getUuid().toString().trim().equals("0000fff4-0000-1000-8000-00805f9b34fb")) {
//                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
//                        mNotifyCharacteristic = characteristic;
//                    }
//                    if (characteristic.getUuid().toString().trim().equals("0000fff5-0000-1000-8000-00805f9b34fb")) {
//                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
//                        mWriteCharacteristic = characteristic;
//                    }
//                }
//            }
        }


        mRunnable = new Runnable() {
            @Override
            public void run() {
                if (mWriteCharacteristic != null && num < 5) {//&&!mIsSend

                    if (sendBuffer != null && mWriteCharacteristic != null) {
//					sendBuffer[5] = (byte)Integer.parseInt(Integer.toHexString(0 * 16+num),16);
                        mWriteCharacteristic.setValue(sendBuffer);
                        if (mBluetoothLeService!=null){
                            boolean writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                            Log.e(TAG, "-------writeResult:" + writeResult);
                            if (writeResult) {
                                num++;
                                mCheckField.setChecked(true);
                            }
                        }
                    }
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mHandler.postDelayed(mRunnable, 1000);
    }

    Runnable mRunnable;
    int num = 0;

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MDBluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(MDBluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(MDBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(MDBluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(MDBluetoothLeService.ACTION_DATA_RSSI);
        return intentFilter;
    }

    public BigDecimal CountBmi(float weight, float height) {
//		Log.e("bmi","bmi"+(weight/10f)/(countHeight.getCmHeight()*countHeight.getCmHeight()/10000f));
        return new BigDecimal((weight) / (height * height / 10000f));
    }

}
