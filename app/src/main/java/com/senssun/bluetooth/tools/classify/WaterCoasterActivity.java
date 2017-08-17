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
import com.senssun.bluetooth.tools.pad.PadDeviceScanActivity;
import com.senssun.bluetooth.tools.pad.test.TestPadDeviceControlActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaterCoasterActivity extends Activity {
    private ListView mWaterCoaster_lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_coaster);
        mWaterCoaster_lv = (ListView) findViewById(R.id.water_coaster_lv);
        getActionBar().setTitle("饮水杯垫");
        getActionBar().setDisplayHomeAsUpEnabled(true);

        SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.item_device,
                new String[]{"title", "info", "image"},
                new int[]{R.id.deviceName, R.id.deviceModel, R.id.deviceIcon});

        mWaterCoaster_lv.setAdapter(adapter);
        mWaterCoaster_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

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
            case 0://饮水杯垫(标定)
                intent.setClass(this, PadDeviceScanActivity.class);//
                startActivity(intent);
                break;
            case 1://饮水杯垫(压力测试)
                intent.setClass(this, TestPadDeviceControlActivity.class);//
                startActivity(intent);
                break;

        }
    }

    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        Map<String, Object> map;  //1

        map = new HashMap<String, Object>();  //12
        map.put("title", "饮水杯垫(标定)");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //22
        map.put("title", "饮水杯垫压力测试");
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