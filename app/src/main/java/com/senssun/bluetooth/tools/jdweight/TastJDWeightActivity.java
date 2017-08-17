package com.senssun.bluetooth.tools.jdweight;


import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.application.MyApp;
import com.senssun.bluetooth.tools.adapter.DeviceAdapter;
import com.senssun.bluetooth.tools.entity.MessObject;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目的主Activity，所有的Fragment都嵌入在这里。
 *
 * @author guolin
 */

public class TastJDWeightActivity extends Activity implements OnClickListener {
    static final String TAG = "TabJDWeightActivity";
    private boolean autoSend = true;

    private MyHandler handler;
    private ListView deviceList;
    private DeviceAdapter deviceAdapter;
    private List<MessObject> messList;
    private TestJDWeightBluetoothLeService mBluetoothLeService;

    public EditText inputRssi, inputOverRssi;
    public int OverRssi = 55;
    public int InputRssi = 55;
    private MyApp myApp;

    // Code to manage Service lifecycle.
    public ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((TestJDWeightBluetoothLeService.LocalBinder) service).getService();//
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("BluetoothLeService", "执行断开Service");
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.layout_jd_weight_noti);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);//
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        handler = new MyHandler();
        myApp = (MyApp) getApplication();
        messList =new ArrayList<MessObject>();
        init();
        Intent gattServiceIntent = new Intent(this, TestJDWeightBluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        DessRssi();
        Dess();
    }

    private void init() {
        deviceAdapter = new DeviceAdapter(this, messList);
        deviceList = (ListView) findViewById(R.id.deviceList);

        deviceList.setAdapter(deviceAdapter);
        RelativeLayout confirm = (RelativeLayout) findViewById(R.id.confirm);
        confirm.setOnClickListener(this);

        inputRssi = (EditText) findViewById(R.id.inputRssi);
        inputOverRssi = (EditText) findViewById(R.id.inputOverRssi);

        inputOverRssi.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                try {
                    OverRssi = Integer.valueOf(inputOverRssi.getText().toString());
                } catch (NumberFormatException e) {
                    OverRssi = 55;
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                          int arg3) {
                try {
                    OverRssi = Integer.valueOf(inputOverRssi.getText().toString());
                } catch (NumberFormatException e) {
                    OverRssi = 55;
                }
            }

            @Override
            public void afterTextChanged(Editable arg0) {
                try {
                    OverRssi = Integer.valueOf(inputOverRssi.getText().toString());
                } catch (NumberFormatException e) {
                    OverRssi = 55;
                }
            }
        });

        inputRssi.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                try {
                    InputRssi = Integer.valueOf(inputRssi.getText().toString());
                    mBluetoothLeService.setInputRssi(inputRssi.getText().toString());
                } catch (NumberFormatException e) {
                    InputRssi = 55;
                    mBluetoothLeService.setInputRssi("55");
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                          int arg3) {
                try {
                    InputRssi = Integer.valueOf(inputRssi.getText().toString());
                    mBluetoothLeService.setInputRssi(inputRssi.getText().toString());
                } catch (NumberFormatException e) {
                    InputRssi = 55;
                    mBluetoothLeService.setInputRssi("55");
                }
            }

            @Override
            public void afterTextChanged(Editable arg0) {
                try {
                    InputRssi = Integer.valueOf(inputRssi.getText().toString());
                    mBluetoothLeService.setInputRssi(inputRssi.getText().toString());
                } catch (NumberFormatException e) {
                    InputRssi = 55;
                    mBluetoothLeService.setInputRssi("55");
                }
            }
        });
//
//			mBluetoothLeService.scanLeDevice(true);
//			Toast.makeText(MainActivity.this, "执行搜索",Toast.LENGTH_SHORT).show();
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {    //广播接收
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TestJDWeightBluetoothLeService.JD_ACTION_GATT_CONNECTED.equals(action)) {
                String address = intent.getStringExtra(TestJDWeightBluetoothLeService.JD_EXTRA_ADDRESS);
                String name = intent.getStringExtra(TestJDWeightBluetoothLeService.JD_SEND_NAME);
                int inNew = 0;

                for(int i=messList.size()-1;i>=0;i--){
                    MessObject tmp = messList.get(i);
                    if (tmp.getAddress().equals(address)) {
                        tmp.setStatus("连接成功");
                        inNew = 1;
                        break;
                    }
                }

                if (inNew == 0) {
                    MessObject messObject = new MessObject();
                    messObject.setName(name);
                    messObject.setAddress(address);
                    messObject.setStatus("连接成功");
                    messList.add(messObject);
                }
                deviceAdapter.notifyDataSetChanged();
            } else if (TestJDWeightBluetoothLeService.JD_ACTION_GATT_DISCONNECTED.equals(action)) {
                String address = intent.getStringExtra(TestJDWeightBluetoothLeService.JD_EXTRA_ADDRESS);

                for(int i=messList.size()-1;i>=0;i--){
                    MessObject tmp = messList.get(i);
                    if (tmp.getAddress().equals(address)) {
                        tmp.setStatus("断开连接");
                    }
                }
                deviceAdapter.notifyDataSetChanged();
            } else if (TestJDWeightBluetoothLeService.JD_ACTION_DATA_NOTIFY.equals(action)) {
                String data = intent.getStringExtra(TestJDWeightBluetoothLeService.JD_EXTRA_DATA);
                String address = intent.getStringExtra(TestJDWeightBluetoothLeService.JD_EXTRA_ADDRESS);

                for(int i=messList.size()-1;i>=0;i--){
                    MessObject tmp = messList.get(i);
                    if (tmp.getAddress().equals(address)) {
                        if (data.length() > 8) {
                            String[] strdata = data.split("-");
                            if (strdata[1].equals("04")) {
                                String tmpNum = strdata[9] + strdata[10];
                                tmp.setMess(String.valueOf(Integer.valueOf(tmpNum, 16)));
                                break;
                            }
                        }
                    }
                }
                deviceAdapter.notifyDataSetChanged();
            } else if (TestJDWeightBluetoothLeService.JD_ACTION_DATA_RSSI.equals(action)) {
                String RSSI = intent.getStringExtra(TestJDWeightBluetoothLeService.JD_SEND_RSSI);
                String address = intent.getStringExtra(TestJDWeightBluetoothLeService.JD_EXTRA_ADDRESS);

                for(int i=messList.size()-1;i>=0;i--){
                    MessObject tmp = messList.get(i);
                    if (tmp.getAddress().equals(address)) {
                        tmp.setRssi(Integer.valueOf(RSSI));
                    }
                }
                deviceAdapter.notifyDataSetChanged();
            } else if (TestJDWeightBluetoothLeService.JD_ACTION_DATA_SendProduct.equals(action)) {
                String address = intent.getStringExtra(TestJDWeightBluetoothLeService.JD_EXTRA_ADDRESS);

                for(int i=messList.size()-1;i>=0;i--){
                    MessObject tmp = messList.get(i);
                    if (tmp.getAddress().equals(address)) {
                        tmp.setSendProduct(true);
                    }
                }
                deviceAdapter.notifyDataSetChanged();
            } else if (TestJDWeightBluetoothLeService.JD_ACTION_DATA_SendProductSuc.equals(action)) {
                String address = intent.getStringExtra(TestJDWeightBluetoothLeService.JD_EXTRA_ADDRESS);

                for(int i=messList.size()-1;i>=0;i--){
                    MessObject tmp = messList.get(i);
                    if (tmp.getAddress().equals(address)) {
                        tmp.setSendProductSuc(true);


                    }
                }

                deviceAdapter.notifyDataSetChanged();
            }

        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TestJDWeightBluetoothLeService.JD_ACTION_GATT_CONNECTED);
        intentFilter.addAction(TestJDWeightBluetoothLeService.JD_ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(TestJDWeightBluetoothLeService.JD_ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(TestJDWeightBluetoothLeService.JD_ACTION_DATA_NOTIFY);

        intentFilter.addAction(TestJDWeightBluetoothLeService.JD_SEND_ADDRESS);
        intentFilter.addAction(TestJDWeightBluetoothLeService.JD_SEND_RSSI);
        intentFilter.addAction(TestJDWeightBluetoothLeService.JD_SEND_NAME);

        intentFilter.addAction(TestJDWeightBluetoothLeService.JD_ACTION_DATA_RSSI);

        intentFilter.addAction(TestJDWeightBluetoothLeService.JD_ACTION_DATA_SendProduct);
        intentFilter.addAction(TestJDWeightBluetoothLeService.JD_ACTION_DATA_SendProductSuc);

        return intentFilter;
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
        Log.d(TAG, "释放页面");
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        autoSend = false;
    }

    private void Dess() {
        new Thread() {
            public void run() {
                while (autoSend) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    Message m = new Message();
                    handler.sendMessage(m);

                }
            }
        }.start();
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

    class MyHandler extends Handler {
        public void handleMessage(Message msg) {
            if (mBluetoothLeService != null) {
                mBluetoothLeService.sendTimeHander();

                mBluetoothLeService.sendTestHander();

            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                onBackPressed();
                break;
            default:
                break;
            case R.id.scan:
                mBluetoothLeService.scanLeDevice(true);
                break;
        }
    }


}
