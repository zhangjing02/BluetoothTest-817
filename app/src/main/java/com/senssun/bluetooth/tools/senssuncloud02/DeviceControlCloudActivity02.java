package com.senssun.bluetooth.tools.senssuncloud02;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.senssun.bluetooth.tools.R;

import java.math.BigDecimal;

import cn.senssun.ble.sdk.BleDevice;
import cn.senssun.ble.sdk.BleScan;
import cn.senssun.ble.sdk.BluetoothBuffer;
import cn.senssun.ble.sdk.cloud.BleCloudSDK;
import cn.senssun.ble.sdk.cloud.CloudOnActionMethod;

public class DeviceControlCloudActivity02 extends Activity implements OnClickListener {
    private final static String TAG = "DeviceControlFatActivity";
    BleDevice mDevice;
    boolean mConnected;
    TextView KgWeightNum, LbWeightNum, IfStable;
    TextView FatNum, HydrationNum, KcalNum, MuscleNum, BoneNum;
    TextView his;
    BluetoothAdapter mAdapter;
    private boolean isSendUser = false;
    private String pin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_control);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        final Intent intent = getIntent();
        mDevice = (BleDevice) intent.getParcelableExtra(BleScan.EXTRAS_DEVICE);

        KgWeightNum = (TextView) findViewById(R.id.KgWeightNum);
        LbWeightNum = (TextView) findViewById(R.id.LbWeightNum);
        IfStable = (TextView) findViewById(R.id.IfStable);
        FatNum = (TextView) findViewById(R.id.FatNum);
        HydrationNum = (TextView) findViewById(R.id.HydrationNum);
        KcalNum = (TextView) findViewById(R.id.KcalNum);
        MuscleNum = (TextView) findViewById(R.id.muscleNum);
        BoneNum = (TextView) findViewById(R.id.boneNum);
        his = (TextView) findViewById(R.id.his);

        BleCloudSDK.getInstance().InitSDK(this);
        BleCloudSDK.getInstance().setOnInitService(new CloudOnActionMethod.OnInitService() {

            @Override
            public void OnInit() {
                // TODO Auto-generated method stub
                //				if(!BleCloudSDK.getInstance().isBind(DeviceControlFatActivity.this)){

                BleCloudSDK.getInstance().ConnectDeviceId(mDevice.getManuData());//进行设备连接
                //					BleCloudSDK.getInstance().setBind(DeviceControlFatActivity.this, mDevice.getManuData());//进行绑定
                //				}
                //				bleSDK.ConnectDeviceId(mDevice.getManuData());//进行设备连接
            }
        });
        BleCloudSDK.getInstance().setOnConnectState(new CloudOnActionMethod.OnConnectState() {//连接状态发生变化回调
            @Override
            public void OnState(boolean State) {
                if (State) {
                    mConnected = true;
                    Toast.makeText(DeviceControlCloudActivity02.this, "连接", Toast.LENGTH_SHORT).show();
                    //					if(!BleCloudSDK.getInstance().isBind(DeviceControlCloudActivity02.this)){
                    //						BleCloudSDK.getInstance().setBind(DeviceControlCloudActivity02.this, mDevice.getManuData());//进行绑定
                    //					}
                } else {
                    mConnected = false;
                    Toast.makeText(DeviceControlCloudActivity02.this, "断开", Toast.LENGTH_SHORT).show();
                }
                invalidateOptionsMenu();
            }
        });

//        boolean connectStatus = BleCloudSDK.getInstance().isConnect();//主动查询连接状态
//        BleCloudSDK.getInstance().setOnDisplayDATA(new OnDisplayDATA() {
//
//            @Override
//            public void OnDATA(String data) {
//                // TODO Auto-generated method stub
//
//
//            }
//        });
//        BleCloudSDK.getInstance().setOnUserInfoStatus(new OnUserInfoStatus() {
//
//            @Override
//            public void OnListener(int status) {
//                switch (status) {
//                    case 0:
//                        Toast.makeText(DeviceControlCloudActivity02.this, "秤体账户新增成功", Toast.LENGTH_SHORT).show();
//                        break;
//                    case 1:
//                        Toast.makeText(DeviceControlCloudActivity02.this, "秤体账户修改成功", Toast.LENGTH_SHORT).show();
//                        break;
//                    case 2:
//                        Toast.makeText(DeviceControlCloudActivity02.this, "秤体账户删除成功", Toast.LENGTH_SHORT).show();
//                        break;
//                    case 3:
//                        Toast.makeText(DeviceControlCloudActivity02.this, "秤体账户已满", Toast.LENGTH_SHORT).show();
//                        break;
//                }
//            }
//        });

//        BleCloudSDK.getInstance().setOnAllUsers(new OnAllUsers() {
//
//            @Override
//            public void OnShow(List<SysUserInfo> users, boolean isfull) {
//                // TODO Auto-generated method stub
//                Toast.makeText(DeviceControlCloudActivity02.this, "回调用户数量" + users.size() + isfull, Toast.LENGTH_SHORT).show();
//            }
//        });
        //		bleSDK.setOnMeasure(new OnMeasure() {//数据回调
        //			@Override
        //			public void OnMeasure(FatMeasure fatMeasure) {
        //				switch (fatMeasure.getDataType().getValue()) {
        //				case -1:
        //					break;
        //				case 0:
        //					KgWeightNum.setText(String.format("%.1f", fatMeasure.getWeightKg()/10f)+"KG");
        //					LbWeightNum.setText(String.format("%.1f", fatMeasure.getWeightLb()/10f)+"LB");
        //					if(fatMeasure.isIfStable()){
        //						IfStable.setText("稳定");
        //					}else{
        //						IfStable.setText("不稳定");
        //					}
        //					break;
        //				case 1:
        //					KgWeightNum.setText(String.format("%.1f", fatMeasure.getWeightKg()/10f)+"KG");
        //					LbWeightNum.setText(String.format("%.1f", fatMeasure.getWeightLb()/10f)+"LB");
        //					if(fatMeasure.isIfStable()){
        //						IfStable.setText("稳定");
        //					}else{
        //						IfStable.setText("不稳定");
        //					}
        //					FatNum.setText(String.format("%.1f", fatMeasure.getFat()/10f)+"%");
        //					HydrationNum.setText(String.format("%.1f", fatMeasure.getHydration()/10f)+"%");
        //					KcalNum.setText(fatMeasure.getKcal()+"Kcal");
        //					MuscleNum.setText(String.format("%.1f", fatMeasure.getMuscle()/10f)+"%");
        //					BoneNum.setText(String.format("%.1f", fatMeasure.getBone()/10f)+"%");
        //					break;
        //				case 2:
        //					his.setText(his.getText().toString()+" 用户ID："+fatMeasure.getUserID()+"\n"+
        //							"历史序号："+fatMeasure.getNumber()+ " 日期："+fatMeasure.getHistoryDate()+"\n"+
        //							"重量KG："+String.format("%.1f", fatMeasure.getHistoryWeightKg()/10f)+"KG"+" 重量LB："+String.format("%.1f", fatMeasure.getHistoryWeightLb()/10f)+"LB"+"\n"+
        //							"脂肪："+String.format("%.1f", fatMeasure.getHistoryFat()/10f)+"%"+" 水份："+String.format("%.1f", fatMeasure.getHistoryHydration()/10f)+"%"+ " 肌肉："+String.format("%.1f", fatMeasure.getHistoryMuscle()/10f)+"%"+"\n"+
        //							"骨骼："+String.format("%.1f", fatMeasure.getHistoryBone()/10f)+"%"+" 卡路里："+fatMeasure.getHistoryKcal()+"Kcal"+"\n"
        //							);
        //					break;
        //				}
        //
        //				Log.e("DIS", "type"+fatMeasure.getDataType().getValue()+"\n"+
        //						String.valueOf(fatMeasure.getWeightKg())+"\n"+
        //						String.valueOf(fatMeasure.getWeightLb())+"\n"+
        //						fatMeasure.isIfStable()+"\n"+
        //						String.valueOf(fatMeasure.getHydration())+"\n"+
        //						String.valueOf(fatMeasure.getKcal())+"\n"+
        //						String.valueOf(fatMeasure.getMuscle())+"\n"+
        //						String.valueOf(fatMeasure.getBone())
        //						);
        //			}
        //		});
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            switch (action) {
                case BluetoothBuffer.ACTION_GATT_CONNECTED:


                    break;
                case BluetoothBuffer.ACTION_GATT_DISCONNECTED:
                   onBackPressed();
                   // BleCloudSDK.getInstance().ConnectDeviceId(mDevice.getManuData());//进行设备连接
                    break;

                case BluetoothBuffer.ACTION_INIT:
                    BleCloudSDK.getInstance().ConnectDeviceId(mDevice.getManuData());//进行设备连接

                    break;

                case BluetoothBuffer.ACTION_DATA:
                    if (!isSendUser){
                        for (int i = 0; i < 4; i++) {
                             addUser();
                            if (i>2){
                                isSendUser=true;
                            }
                        }

                    }

                    String data = intent.getStringExtra(BluetoothBuffer.EXTRA_DATA);
                    //	Log.e(TAG, "现在data:"+data);
                    Log.i("xxxxxxxxx","现在的数据是？"+data);
                     his.setText(his.getText().toString() + data + "\n");
                    String[] strdata = data.split("-");
                    switch (strdata[5]) {
                        case "03":
                            switch (strdata[6]) {
                                case "80":
                                    if (strdata[12].equals("A0")) {
                                        String tmpNum = strdata[7] + strdata[8];
                                        KgWeightNum.setText(String.format("%.1f", Integer.valueOf(tmpNum, 16) / 10f) + "KG");
                                        IfStable.setText("不稳定");
                                    } else {
                                        String tmpNum = strdata[7] + strdata[8];
                                        KgWeightNum.setText(String.format("%.1f", Integer.valueOf(tmpNum, 16) / 10f) + "KG");
                                        IfStable.setText("稳定");
                                    }
                                    break;
                                case "82":
                                    KgWeightNum.setText(new BigDecimal(Integer.valueOf(strdata[10] + strdata[11], 16) / 10f).setScale(1, BigDecimal.ROUND_HALF_UP).toString());
                                    FatNum.setText(new BigDecimal(Integer.valueOf(strdata[12] + strdata[13], 16) / 10f).setScale(1, BigDecimal.ROUND_HALF_UP).toString());
                                    HydrationNum.setText(new BigDecimal(Integer.valueOf(strdata[16] + strdata[17], 16) / 10f).setScale(1, BigDecimal.ROUND_HALF_UP).toString());
                                    KcalNum.setText(String.valueOf(Integer.valueOf(strdata[22] + strdata[23], 16)));
                                    MuscleNum.setText(new BigDecimal(Integer.valueOf(strdata[18] + strdata[19], 16) / 10f).setScale(1, BigDecimal.ROUND_HALF_UP).toString());
                                    BoneNum.setText(new BigDecimal(Integer.valueOf(strdata[20] + strdata[21], 16) / 10f).setScale(1, BigDecimal.ROUND_HALF_UP).toString());
                                    IfStable.setText("稳定");
                                    break;
                            }
                            break;
                    }
            }
        }
    };

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothBuffer.ACTION_INIT);
        intentFilter.addAction(BluetoothBuffer.ACTION_DATA);
        intentFilter.addAction(BluetoothBuffer.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothBuffer.ACTION_GATT_DISCONNECTED);

        return intentFilter;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.AddUser: {
                addUser();
            }
            break;
            case R.id.DeleUser: {
                pin = ((TextView) findViewById(R.id.pin)).getText().toString();
                BleCloudSDK.getInstance().DelUserInfo(pin);
            }
            break;
            case R.id.SendDataCommun: {
                String pin = ((TextView) findViewById(R.id.pin)).getText().toString();
                BleCloudSDK.getInstance().SendDataCommun(pin);//同步秤体上1号用户历史数据
            }
            break;
            case R.id.SendDataCommunAll: {
                String pin = ((TextView) findViewById(R.id.pin)).getText().toString();
                BleCloudSDK.getInstance().SendDataCommunAll(pin);//同步秤体上1号用户历史数据
            }
            break;
            case R.id.QueryUserInfoBuffer:
                BleCloudSDK.getInstance().QueryAllUserInfo();
                break;
            case R.id.ResetBuffer:
                BleCloudSDK.getInstance().ResetBuffer();
                break;
        }
    }

    private void addUser() {
        String pin = ((EditText) findViewById(R.id.pin)).getText().toString();
        Integer height = Integer.valueOf(((EditText) findViewById(R.id.height)).getText().toString());
        Integer age = Integer.valueOf(((EditText) findViewById(R.id.age)).getText().toString());
        Integer sex = Integer.valueOf(((EditText) findViewById(R.id.sex)).getText().toString());
        Integer weight = Integer.valueOf(((EditText) findViewById(R.id.weight)).getText().toString());
        int unit = 0;
        BleCloudSDK.getInstance().AddUserInfo(pin, sex, height, age, 1, unit, weight);//pin码,性别,身高,年龄,运动模式,体重

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
                Log.i("zhangjing666", "连接按钮" + mDevice.getManuData());
                BleCloudSDK.getInstance().ConnectDeviceId(mDevice.getManuData());//进行设备连接
                return true;
            case R.id.menu_disconnect:
                BleCloudSDK.getInstance().Disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        BleCloudSDK.getInstance().DelUserInfo(pin);
//        BleCloudSDK.getInstance().RemoveAllOnUserInfoStatus();
//        BleCloudSDK.getInstance().RemoveAllOnConnectState();
//        BleCloudSDK.getInstance().Disconnect();
        if (BleCloudSDK.getInstance()!=null)
        BleCloudSDK.getInstance().stopSDK(this);
        unregisterReceiver(mGattUpdateReceiver);

    }


}
