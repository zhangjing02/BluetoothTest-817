package com.senssun.bluetooth.tools;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.senssun.bluetooth.tools.babyscale.BabylScaleScanActivity;
import com.senssun.bluetooth.tools.blutooth_pressure.BluetoothPressureScanActivity;
import com.senssun.bluetooth.tools.classify.ALActivity;
import com.senssun.bluetooth.tools.classify.AlarmClockActivity;
import com.senssun.bluetooth.tools.classify.BodyConfigActivity;
import com.senssun.bluetooth.tools.classify.BodyScaleActivity;
import com.senssun.bluetooth.tools.classify.MideaActivity;
import com.senssun.bluetooth.tools.classify.NutritionActivity;
import com.senssun.bluetooth.tools.classify.WaterCoasterActivity;
import com.senssun.bluetooth.tools.cocktail.CocktailDeviceScanActivity;
import com.senssun.bluetooth.tools.eight_electrodes.TestEightFatActivity;
import com.senssun.bluetooth.tools.fat.FatDeviceScanActivity;
import com.senssun.bluetooth.tools.infantscale.InfantDeviceScanActivity;
import com.senssun.bluetooth.tools.jdweight.JdDeviceScanActivity;
import com.senssun.bluetooth.tools.mkfat.MkFatDeviceScanActivity;
import com.senssun.bluetooth.tools.senssuncloud02.DeviceScanActivity02;
import com.senssun.bluetooth.tools.senssuncloud03.DeviceScanActivity03;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexActivity extends Activity implements OnClickListener {
    private BluetoothAdapter mBluetoothAdapter;
    private ListView deviceList;
    private List<Map<String, Object>> mData;
    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        deviceList = (ListView) findViewById(R.id.deviceList);

        SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.item_device,
                new String[]{"title", "info", "image"},
                new int[]{R.id.deviceName, R.id.deviceModel, R.id.deviceIcon});

        deviceList.setAdapter(adapter);

        deviceList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                intentTurn(arg2);
                //				Toast.makeText(IndexActivity.this, "再按一次最小化程序"+arg2, Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.index, menu);
        menu.findItem(R.id.menu_exit).setVisible(true);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == R.id.menu_exit) {
            QuitDialog();
        }
        return true;
    }

    private void intentTurn(int i) {
        Intent intent = new Intent();
        switch (i) {
            case 0: //人体秤
                intent.setClass(this, BodyScaleActivity.class);//
                startActivity(intent);
                break;
            case 1:
                //脂肪秤
                Log.i("zhangjing", "脂肪秤001");
                intent.setClass(this, FatDeviceScanActivity.class);//
                startActivity(intent);
                break;
            case 2:
                //营养秤
                intent.setClass(this, NutritionActivity.class);//
                startActivity(intent);
                break;
            case 3:
                //阿里系
                intent.setClass(this, ALActivity.class);//
                startActivity(intent);
                break;
            case 4:
                //京东人体秤
                intent.setClass(this, JdDeviceScanActivity.class);//
                startActivity(intent);
                break;
            case 5:
                //八电极体脂秤
                intent.setClass(this, TestEightFatActivity.class);//
                startActivity(intent);
                break;
            case 6:
                //饮水杯垫(标定)
                intent.setClass(this, WaterCoasterActivity.class);//
                startActivity(intent);
                break;
            case 7:
                //美的体脂秤
                intent.setClass(this, MideaActivity.class);
                startActivity(intent);
                break;
            case 8:
                //人体秤参数配置
                intent.setClass(this, BodyConfigActivity.class);
                startActivity(intent);
                break;
            case 9:
                //芯海模块测试(阿里秤)
                intent.setClass(this, com.senssun.bluetooth.tools.xinhai.ali.ALDeviceScanActivity.class);
                startActivity(intent);
                break;
            case 10:
                //澳佐脂肪秤
                intent.setClass(this, com.senssun.bluetooth.tools.fat.aozuo.FatAoZuoDeviceScanActivity.class);
                startActivity(intent);
                break;
            case 11:
                //闹钟秤
                intent.setClass(this, AlarmClockActivity.class);
                startActivity(intent);
                break;
            case 12:
                //玫琳凯
                intent.setClass(this, MkFatDeviceScanActivity.class);//
                startActivity(intent);
                break;
            case 13:
                //飞鹤抱婴秤
                intent.setClass(this, InfantDeviceScanActivity.class);
                startActivity(intent);

                break;
            case 14:
                //香山云
                intent.setClass(this, DeviceScanActivity02.class);
                startActivity(intent);
                break;
            case 15:
                //新添加香山云
                intent.setClass(this, DeviceScanActivity03.class);
                startActivity(intent);
                break;
            case 16:
                //鸡尾酒秤
                intent.setClass(this, CocktailDeviceScanActivity.class);
                startActivity(intent);
                break;
            case 17:
                //婴儿秤
                intent.setClass(this, BabylScaleScanActivity.class);
                startActivity(intent);
                break;
            case 18:
                //蓝牙压力测试
                intent.setClass(this, BluetoothPressureScanActivity.class);
                startActivity(intent);

//            case 7://行李追踪器
//                intent.setClass(this, TestJustHereActivity.class);//
//                startActivity(intent);
//                break;
        }
    }


    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        Map<String, Object> map = new HashMap<String, Object>();  //1
        map.put("title", "人体秤(包含印度澳佐)");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_weight);
        list.add(map);


        map = new HashMap<String, Object>();  //2
        map.put("title", "脂肪秤");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);


        map = new HashMap<String, Object>();  //4
        map.put("title", "营养秤");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_food);
        list.add(map);


        map = new HashMap<String, Object>();  //6
        map.put("title", "阿里系");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);


        map = new HashMap<String, Object>();  //10
        map.put("title", "京东人体秤");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);


        map = new HashMap<String, Object>();  //11
        map.put("title", "八电极体脂秤");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //12
        map.put("title", "饮水杯垫(标定)");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //13
        map.put("title", "美的体脂秤");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);


        map = new HashMap<String, Object>();  //15
        map.put("title", "人体秤参数配置");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);


        map = new HashMap<String, Object>();  //18
        map.put("title", "芯海模块测试(阿里秤)");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //19
        map.put("title", "印度澳佐脂肪秤");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //20
        map.put("title", "闹钟秤测试");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //24
        map.put("title", "玫琳凯脂肪秤");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<>();
        map.put("title", "飞鹤抱婴秤");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<>();
        map.put("title", "香山云测试02");
        map.put("info", "型号：002");
        map.put("image", R.drawable.xiangshanyun);
        list.add(map);

        map = new HashMap<>();
        map.put("title", "香山云测试03");
        map.put("info", "型号：003");
        map.put("image", R.drawable.xiangshanyun);
        list.add(map);


        map = new HashMap<>();
        map.put("title", "鸡尾酒秤");
        map.put("info", "型号：001");
        map.put("image", R.drawable.cocktail);
        list.add(map);


        map = new HashMap<>();
        map.put("title", "婴儿秤");
        map.put("info", "型号：iR721B");
        map.put("image", R.drawable.baby_scale);
        list.add(map);


        map=new HashMap<>();
        map.put("title","蓝牙压力测试");
        map.put("info","型号：XXXXX");
        map.put("image",R.drawable.pressuretest );
        list.add(map);


//        map = new HashMap<String, Object>();
//        map.put("title", "饮水杯垫(秤重)");
//        map.put("info", "型号：001");
//        map.put("image", R.drawable.icon_fat);
//        list.add(map);

//        map = new HashMap<String, Object>();
//        map.put("title", "行李追踪器");
//        map.put("info", "型号：001");
//        map.put("image", R.drawable.icon_fat);
//        list.add(map);


        return list;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                System.exit(0);
                break;

            default:
                break;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if ((event.getAction() == KeyEvent.ACTION_DOWN) && (event.getKeyCode() == KeyEvent.KEYCODE_BACK)) {
            QuitDialog();
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * 退出对话框
     */
    private void QuitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("")
                .setIcon(null)
                .setCancelable(false)
                .setMessage("是否确定退出？")
                .setPositiveButton("是",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    finish();
                                    System.exit(0);
                                } catch (Exception e) {
                                    // TODO: handle exception
                                }
                            }
                        })
                .setNegativeButton("否",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
    }

}
