package com.senssun.bluetooth.tools.classify;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.bleweight.BleWeightDeviceScanActivity;
import com.senssun.bluetooth.tools.jdweight.JdDeviceScanActivity;
import com.senssun.bluetooth.tools.weight.TestWeightActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BodyScaleActivity extends Activity {
    private ListView mBody_lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body_scale);

        Log.i("zhangjing","人体秤系列");
        getActionBar().setTitle("人体秤系列");
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mBody_lv= (ListView) findViewById(R.id.body_lv);

        SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.item_device,
                new String[]{"title", "info", "image"},
                new int[]{R.id.deviceName, R.id.deviceModel, R.id.deviceIcon});

        mBody_lv.setAdapter(adapter);
        mBody_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

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
            case 0: //人体秤
                intent.setClass(this, TestWeightActivity.class);//
                startActivity(intent);
                break;
            case 1: //ble人体秤
                intent.setClass(this, BleWeightDeviceScanActivity.class);//
                startActivity(intent);
                break;
            case 3:
                //京东人体秤
                intent.setClass(this, JdDeviceScanActivity.class);//
                startActivity(intent);

                break;

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
        map.put("title", "BLE人体秤");
        map.put("info", "型号：IB911B");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //10
        map.put("title", "京东人体秤");
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
