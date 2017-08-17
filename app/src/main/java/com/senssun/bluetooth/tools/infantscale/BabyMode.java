package com.senssun.bluetooth.tools.infantscale;

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
import android.widget.RadioButton;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.relative.Information;

import java.util.ArrayList;
import java.util.List;

public class BabyMode extends Activity {

    private final static String TAG = "BabyMode";

    private Context mContext;
    private CheckedTextView mCheckField;
    private TextView mConnectionState;
    private TextView mAdultDataField, mBabyDataField;
    private TextView mRssiField;
    private String mDeviceName;
    private String mDeviceAddress;
    private InfantBluetoothLeService02 mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private boolean mChceked = false;
    private Button mGoto_infant, mOut_infant;
    private boolean writeResult = false;


    //默认165cm 25year female
    public final static byte[] sendBuffer = new byte[]{(byte) 0xa5, (byte) 0x10, (byte) 0x01, (byte) 0x19, (byte) 0xa5,
            0, 0, (byte) 0x74};//
    //清除历史
    public final byte[] ClearDataBuffer = new byte[]{(byte) 0xa5, 0x5a, 0, 0, 0, 0, 0, 0x5a};


    public final static byte[] OUTChildBuffer = new byte[]{(byte) 0xA5, 0x1A, 0x00, 0, 0, 0,
            0, 0x1A};//退出抱婴测量模式


    public final static byte[] GOChildBuffer = new byte[]{(byte) 0xA5, 0x1A, 0x01, 0, 0, 0, 0,
            0x1B};//进入抱婴测量模式


    private EditText etHeight, etAge, etUserNum;
    private RadioButton rbMale, rbFemale;
    private Button btnEdit, sendUserInfo;
    private boolean isEditAble = false;
    private SharedPreferences mySharedPreferences;
    private Handler mHandler;
    private String mUnit;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {


        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((InfantBluetoothLeService02.LocalBinder) service).getService();
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
    private final BroadcastReceiver mGattUpdateReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (InfantBluetoothLeService02.ACTION_GATT_CONNECTED.equals(action)) {

                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (InfantBluetoothLeService02.ACTION_GATT_DISCONNECTED.equals(action)) {

                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();

                mBluetoothLeService.connect(mDeviceAddress);
                SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
                if (mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
                   // onBackPressed();
                    //自己加入逻辑，如果连接失败，就不退出，而是直接再继续连接
                    mBluetoothLeService.connect(mDeviceAddress);
                }
            } else if (InfantBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (InfantBluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                displayData(intent.getStringExtra(InfantBluetoothLeService.EXTRA_DATA));
            } else if (InfantBluetoothLeService.ACTION_DATA_RSSI.equals(action)) {

                displayRssi(intent.getStringExtra("Rssi"));
            }
        }
    };


    private void clearUI() {
        // mAdultDataField.setText(R.string.no_data);
        mBabyDataField.setTextColor(Color.BLACK);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_baby_mode);

        mContext = this;
        mHandler = new Handler();

        mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
        mChceked = mySharedPreferences.getBoolean(Information.DB.AutoCheck, true);
        // Sets up UI references.

        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mAdultDataField = (TextView) findViewById(R.id.adult_value);
        mBabyDataField = (TextView) findViewById(R.id.baby_value);
        mRssiField = (TextView) findViewById(R.id.rssi_value);
        mCheckField = (CheckedTextView) findViewById(R.id.check_value);


        mGoto_infant = (Button) findViewById(R.id.goto_infant);
        mOut_infant = (Button) findViewById(R.id.out_infant);

        Intent intent1 = getIntent();
        mUnit = intent1.getStringExtra("unit");
        mDeviceName = intent1.getStringExtra(InfantDeviceControlActivity.EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent1.getStringExtra(InfantDeviceControlActivity.EXTRAS_DEVICE_ADDRESS);
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);


        mOut_infant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("zhangjing", mWriteCharacteristic + "抱婴退出抱婴模式" + OUTChildBuffer);
                if (mWriteCharacteristic != null) {
                    mWriteCharacteristic.setValue(OUTChildBuffer);
                    boolean writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                    Intent intent2 = new Intent(BabyMode.this, InfantDeviceScanActivity.class);
                    startActivity(intent2);
                    finish();
                }
            }
        });

        mGoto_infant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("zhangjing", "抱婴进入抱婴模式" + mWriteCharacteristic);

                if (mWriteCharacteristic != null) {
                    mWriteCharacteristic.setValue(GOChildBuffer);
                    boolean writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);

                }
            }
        });


        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        this.findViewById(R.id.SendClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                for (int i = 0; i < 11; i++) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mWriteCharacteristic.setValue(ClearDataBuffer);
                            if (mWriteCharacteristic!=null&&mBluetoothLeService!=null){
                                boolean writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                            }

                        }
                    }, 500 * i);
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

        Intent gattServiceIntent = new Intent(BabyMode.this, InfantBluetoothLeService02.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        if (mWriteCharacteristic != null) {
            for (int i = 0; i < 3; i++) {
                mWriteCharacteristic.setValue(GOChildBuffer);
                boolean writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
            }
        }
        if (mWriteCharacteristic != null) {
            if (!writeResult) {
                for (int i = 0; i < 4; i++) {
                    mWriteCharacteristic.setValue(GOChildBuffer);
                    writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                    if (i == 3) {
                        writeResult = true;
                    }
                }
            }

        }


    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver2, makeGattUpdateIntentFilter2());

        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver2);

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


    String str = "";

    private void displayData(String data) {
        Log.e(TAG, "------data:" + data);
        Log.e("jiejue", "判断是否？" + writeResult);

        if (!writeResult) {
            for (int i = 0; i < 3; i++) {
                mWriteCharacteristic.setValue(GOChildBuffer);
                writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                if (i == 2) {
                    writeResult = true;
                }
            }
        }


//        for (int i = 0; i < 3; i++) {
//            mWriteCharacteristic.setValue(OUTChildBuffer);
//            writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
//        }
        if (data != null) {
            String[] strdata = data.split("-");
            Log.e("Notification", data);
            if (strdata.length >= 8) {
                if ("A4".equals(strdata[1])) {
                    String tmpNum, tmpNum2;
                    int number, number2;
                    float fNumber, fNumber2;
                    switch (strdata[6]) {
                        //A0表示临时体重
                        case "A0":
                            tmpNum = strdata[2] + strdata[3];
                            number = Integer.valueOf(tmpNum, 16);
                            tmpNum2 = strdata[4] + strdata[5];
                            number2 = Integer.valueOf(tmpNum2, 16);

                            //  if (("00").equals(strdata[4]) && ("00").equals(strdata[5])) {
                            if (("00").equals(strdata[4]) && ("00").equals(strdata[5])) {
                                if ("kg".equals(mUnit)) {
                                    fNumber = number / 10f;
                                    mAdultDataField.setText(String.valueOf(fNumber) + "kg");
                                    mAdultDataField.setTextColor(Color.BLACK);
                                    // mBabyDataField.setTextColor(Color.BLACK);
                                } else if (("g").equals(mUnit)) {
                                    fNumber = number * 10f;
                                    mAdultDataField.setText(String.valueOf(fNumber) + "g");
                                    mAdultDataField.setTextColor(Color.BLACK);
                                    mBabyDataField.setTextColor(Color.BLACK);
                                } else {
                                    fNumber = number / 10f;
                                    mAdultDataField.setText(String.valueOf(fNumber) + "kg");
                                    mAdultDataField.setTextColor(Color.BLACK);
                                    // mBabyDataField.setTextColor(Color.BLACK);
                                }
                            } else if (!("00").equals(strdata[4]) || !("00").equals(strdata[5])) {
                                if ("kg".equals(mUnit)) {
                                    fNumber2 = number2 / 100f;
                                    mAdultDataField.setText(String.valueOf(fNumber2) + "kg");
                                    mAdultDataField.setTextColor(Color.BLACK);
                                    //  mBabyDataField.setTextColor(Color.BLACK);
                                } else if (("g").equals(mUnit)) {
                                    fNumber2 = number2 * 100f;
                                    mAdultDataField.setText(String.valueOf(fNumber2) + "g");
                                    mAdultDataField.setTextColor(Color.BLACK);
                                    // mBabyDataField.setTextColor(Color.BLACK);
                                } else {
                                    fNumber2 = number2 / 100f;
                                    mAdultDataField.setText(String.valueOf(fNumber2) + "kg");
                                    mAdultDataField.setTextColor(Color.BLACK);
                                    // mBabyDataField.setTextColor(Color.BLACK);
                                }
                            }


                            // }


//                        if (!writeResult) {
//                            for (int i = 0; i < 5; i++) {
//                                mWriteCharacteristic.setValue(calSendBuffer());
//                                writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
//                            }
//                        }
                            if (!str.contains("A0"))
                                str += "A0";
                            break;

                        //AA表示稳定体重
                        case "AA":
                            tmpNum = strdata[2] + strdata[3];
                            tmpNum2 = strdata[4] + strdata[5];
                            number = Integer.valueOf(tmpNum, 16);
                            number2 = Integer.valueOf(tmpNum2, 16);

                            //有稳定数据过来时，先下发三次用户信息抱婴模式下不需要？
//                        for (int i = 0; i < 3; i++) {
//                            mWriteCharacteristic.setValue(calSendBuffer());
//                            writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
//                        }
                            if (("kg").equals(mUnit)) {
                                fNumber = number / 100f;
                                fNumber2 = number2 / 100f;
                                mBabyDataField.setText(String.valueOf(fNumber) + "kg");
                                mAdultDataField.setText(String.valueOf(fNumber2) + "kg");
                                mBabyDataField.setTextColor(Color.BLUE);
                                mAdultDataField.setTextColor(Color.BLUE);

                            } else if (("g").equals(mUnit)) {
                                fNumber = number * 10f;
                                fNumber2 = number2 * 10f;
                                mBabyDataField.setText(String.valueOf(fNumber) + "g");
                                mAdultDataField.setText(String.valueOf(fNumber2) + "g");
                                mBabyDataField.setTextColor(Color.BLUE);
                                mAdultDataField.setTextColor(Color.BLUE);
                            } else {
                                fNumber = number / 100f;
                                fNumber2 = number2 / 100f;
                                mBabyDataField.setText(String.valueOf(fNumber) + "kg");
                                mAdultDataField.setText(String.valueOf(fNumber2) + "kg");
                                mBabyDataField.setTextColor(Color.BLUE);
                                mAdultDataField.setTextColor(Color.BLUE);

                            }
                            if (!str.contains("AA"))
                                str += "AA";
                            break;
                        case "00":
                            Log.i("zhangjing", "抱婴模式测量结束?");

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
        }

//        mBluetoothLeService.getmBluetoothGatt().readRemoteRssi();
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

    private static IntentFilter makeGattUpdateIntentFilter2() {
        final IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction(InfantBluetoothLeService02.ACTION_GATT_CONNECTED);
        intentFilter2.addAction(InfantBluetoothLeService02.ACTION_GATT_DISCONNECTED);
        intentFilter2.addAction(InfantBluetoothLeService02.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter2.addAction(InfantBluetoothLeService02.ACTION_DATA_AVAILABLE);
        intentFilter2.addAction(InfantBluetoothLeService02.ACTION_DATA_RSSI);

        return intentFilter2;
    }
}
