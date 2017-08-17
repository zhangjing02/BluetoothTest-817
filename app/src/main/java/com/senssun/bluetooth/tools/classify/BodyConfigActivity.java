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
import com.senssun.bluetooth.tools.aliconfigweight.ALConfigDeviceScanActivity;
import com.senssun.bluetooth.tools.configfatweight.ConfigDeviceScanActivity;
import com.senssun.bluetooth.tools.jdconfigweight.JDConfigDeviceScanActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BodyConfigActivity extends Activity {

    private ListView mBodyConfig_lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body_config);
        getActionBar().setTitle("人体秤参数系列");
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mBodyConfig_lv= (ListView) findViewById(R.id.body_config_lv);

        SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.item_device,
                new String[]{"title", "info", "image"},
                new int[]{R.id.deviceName, R.id.deviceModel, R.id.deviceIcon});

        mBodyConfig_lv.setAdapter(adapter);
        mBodyConfig_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

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
            case 0://人体秤参数配置
                intent.setClass(this, ConfigDeviceScanActivity.class);
                startActivity(intent);
                break;
            case 1://人体秤参数配置(阿里秤)
                intent.setClass(this, ALConfigDeviceScanActivity.class);
                startActivity(intent);
                break;
            case 2://人体秤参数配置(京东秤)
                intent.setClass(this, JDConfigDeviceScanActivity.class);
                startActivity(intent);
                break;

        }
    }

    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        Map<String, Object> map ;  //1
        map = new HashMap<String, Object>();  //15
        map.put("title", "人体秤参数配置");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //16
        map.put("title", "人体秤参数配置(阿里秤)");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //17
        map.put("title", "人体秤参数配置(京东秤)");
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