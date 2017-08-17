package com.senssun.bluetooth.tools.JustHere;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.adapter.DeviceAdapter;
import com.senssun.bluetooth.tools.application.MyApp;
import com.senssun.bluetooth.tools.entity.MessObject;
import com.senssun.bluetooth.tools.jdweight.TestJDWeightBluetoothLeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.Gallery;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 项目的主Activity，所有的Fragment都嵌入在这里。
 *
 * @author guolin
 */

public class TestJustHereActivity extends Activity implements OnClickListener {
    static final String TAG = "TabJDWeightActivity";
    private boolean autoSend = true;

    private MyHandler handler;
    private ListView deviceList;
    private DeviceAdapter deviceAdapter;
    private List<MessObject> messList;
    private TestJDWeightBluetoothLeService mBluetoothLeService;

    private Gallery inputRssi;
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
        setContentView(R.layout.layout_justhere_noti);

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

        inputRssi = (Gallery) findViewById(R.id.inputRssi);

        SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.item_num,
                new String[]{"title"},
                new int[]{R.id.num});

        inputRssi.setAdapter(adapter); 			// gallery添加ImageAdapter图片资源
        inputRssi.setSelection(3);
        inputRssi.setOnItemSelectedListener(new OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView num=(TextView)view.findViewById(R.id.num);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        inputRssi.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                TextView num=(TextView)arg1.findViewById(R.id.num);
            }
        });
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

    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        for(int i=10;i<120;i=i+5){
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("title", i);
        list.add(map);
        }

        return list;
    }

}
