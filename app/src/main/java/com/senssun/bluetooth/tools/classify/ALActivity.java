package com.senssun.bluetooth.tools.classify;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.aliweight.ALDeviceScanActivity;
import com.senssun.bluetooth.tools.aliweight.old.OldALDeviceScanActivity;
import com.senssun.bluetooth.tools.aliweight.test.TestALDeviceScanActivity;
import com.senssun.bluetooth.tools.aliweight_health_new.ALHealthNewDeviceScanActivity;
import com.senssun.bluetooth.tools.aliweight_new.ALNewDeviceScanActivity;
import com.senssun.bluetooth.tools.aliweight_new.aliweght_latest.ALLatestDeviceScanActivity;
import com.senssun.bluetooth.tools.aliweight_new.liang.LiangALBleWeightDeviceScanActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ALActivity extends Activity {
    private ListView mAL_lv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_al);
        getActionBar().setTitle("阿里系列");
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mAL_lv = (ListView) findViewById(R.id.al_lv);

        SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.item_device,
                new String[]{"title", "info", "image"},
                new int[]{R.id.deviceName, R.id.deviceModel, R.id.deviceIcon});

        mAL_lv.setAdapter(adapter);
        mAL_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                intentTurn(arg2);
                //				Toast.makeText(IndexActivity.this, "再按一次最小化程序"+arg2, Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void intentTurn(int i) {
        Intent intent = new Intent();
        switch (i) {
            case 0://阿里秤旧
                intent.setClass(this, OldALDeviceScanActivity.class);//
                startActivity(intent);
                break;
            case 1://阿里秤
                intent.setClass(this, ALDeviceScanActivity.class);//
                startActivity(intent);
                break;
            case 2://阿里秤(蓝牙模块测试)
                intent.setClass(this, TestALDeviceScanActivity.class);//
                startActivity(intent);
                break;
            case 3://阿里秤(新版阿里协议)EF987i
                intent.setClass(this, ALNewDeviceScanActivity.class);//
                startActivity(intent);
                break;
            case 4://阿里秤(云端版秤)EF987i
                intent.setClass(this, ALHealthNewDeviceScanActivity.class);//
                startActivity(intent);
                break;
            case 5://阿里体重称
                intent.setClass(this, LiangALBleWeightDeviceScanActivity.class);//
                startActivity(intent);
                break;
            case 6://阿里秤（最新）EF987i
                intent.setClass(this, ALLatestDeviceScanActivity.class);//
                startActivity(intent);
                break;
        }
    }

    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map;  //1

        map = new HashMap<String, Object>();  //6
        map.put("title", "阿里秤(旧)");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //7
        map.put("title", "阿里秤");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //8
        map.put("title", "阿里秤(蓝牙模块测试)");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //8
        map.put("title", "阿里秤(新版阿里协议)EF987i");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //9
        map.put("title", "阿里秤(云端版秤)");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //23
        map.put("title", "阿里秤(新版阿里协议)IB06A");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //23
        map.put("title", "阿里秤(最新版)EF987i");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        return list;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        finish();
        return super.onOptionsItemSelected(item);
    }
}