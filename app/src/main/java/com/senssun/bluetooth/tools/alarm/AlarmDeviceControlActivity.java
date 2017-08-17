package com.senssun.bluetooth.tools.alarm;

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

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.relative.Information;

import java.util.ArrayList;
import java.util.List;

public class AlarmDeviceControlActivity extends Activity {
    private final static String TAG = AlarmDeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private Context mContext;
    private CheckedTextView mCheckField;
    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mRssiField;
    private String mDeviceName;
    private String mDeviceAddress;
    private AlarmBluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private boolean mChceked=false;

    private SharedPreferences mySharedPreferences;


    //默认165cm 25year female
    public final static byte[] sendBuffer = new byte[]{ (byte)0xa5,(byte)0x32,0,0,0,0,(byte)0x55,(byte)0x87};//


    private EditText etRingNum;
    private Button btnEdit;
    private boolean isEditAble = false;

    private Handler mHandler;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((AlarmBluetoothLeService.LocalBinder) service).getService();
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
            if (AlarmBluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (AlarmBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
                SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
                if(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
                    onBackPressed();
                }
            } else if (AlarmBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (AlarmBluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(AlarmBluetoothLeService.EXTRA_DATA));
            }else if (AlarmBluetoothLeService.ACTION_DATA_RSSI.equals(action)) {
                displayRssi(intent.getStringExtra("Rssi"));
            }
        }
    };


    private void clearUI() {
        mDataField.setText(R.string.no_data);
        mDataField.setTextColor(Color.BLACK);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_device_control);
        mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);

        mContext = this;
        mHandler = new Handler();
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mRssiField= (TextView) findViewById(R.id.rssi_value);
        mCheckField= (CheckedTextView) findViewById(R.id.check_value);

        etRingNum = (EditText)findViewById(R.id.etRingNum);
        etRingNum.setText(String.valueOf(mySharedPreferences.getInt(Information.DB.RING_NUM,0)));

        btnEdit = (Button)findViewById(R.id.btnEdit);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        btnEdit.setText("编辑");
        etRingNum.setEnabled(false);

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEditAble) {
                    isEditAble = false;
                    btnEdit.setText("编辑");
                    etRingNum.setEnabled(false);
                    SharedPreferences.Editor editor = mySharedPreferences.edit();
                    editor.putInt(Information.DB.RING_NUM, Integer.valueOf(etRingNum.getText().toString()));
                    editor.commit();
                    if (!mChceked) {
                        mWriteCharacteristic.setValue(calSendBuffer());
                        boolean writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                    }

                } else {
                    isEditAble = true;
                    btnEdit.setText("测试铃声");
                    etRingNum.setEnabled(true);
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



        Intent gattServiceIntent = new Intent(this, AlarmBluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private byte[] calSendBuffer(){
        int ringNum = Integer.valueOf(etRingNum.getText().toString());
        sendBuffer[4] = (byte) Integer.parseInt(Integer.toHexString(ringNum),16);
        String strSum = Integer.toHexString((sendBuffer[1] & 0xff)
                + (sendBuffer[2] & 0xff) + (sendBuffer[3] & 0xff)
                + (sendBuffer[4] & 0xff) + (sendBuffer[5] & 0xff) + (sendBuffer[6] & 0xff));
        if(strSum.length() > 2){
            strSum = strSum.substring(strSum.length()-2,strSum.length());
        }else{
            strSum = "0"+strSum;
        }
        sendBuffer[7] = (byte)Integer.parseInt(strSum,16);
        return sendBuffer;
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
                    default:
                        break;
                }
                SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
                if(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {

                    mChceked = true;
                    mWriteCharacteristic.setValue(calSendBuffer());
                    boolean writeResult = false;
                    while(!writeResult){
                        writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                    }
                    Intent intent = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putString("BleSuc", mDeviceAddress);
                    intent.putExtras(bundle);
                    setResult(RESULT_OK, intent);
                    finish();
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
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AlarmBluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(AlarmBluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(AlarmBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(AlarmBluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(AlarmBluetoothLeService.ACTION_DATA_RSSI);
        return intentFilter;
    }

}
