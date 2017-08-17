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
import com.senssun.bluetooth.tools.alarmFat.AlarmFatDeviceScanActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlarmClockActivity extends Activity {

    private ListView mAlarmClock_lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_clock);
        getActionBar().setTitle("闹钟系列");
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mAlarmClock_lv= (ListView) findViewById(R.id.alarm_clock_lv);

        SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.item_device,
                new String[]{"title", "info", "image"},
                new int[]{R.id.deviceName, R.id.deviceModel, R.id.deviceIcon});

        mAlarmClock_lv.setAdapter(adapter);
        mAlarmClock_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

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
            case 0:
                intent.setClass(this, com.senssun.bluetooth.tools.alarm.AlarmDeviceScanActivity.class);
                startActivity(intent);
                break;
            case 1:
                intent.setClass(this, AlarmFatDeviceScanActivity.class);
                startActivity(intent);
                break;

        }
    }

    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        Map<String, Object> map ;  //1
        map = new HashMap<String, Object>();  //20
        map.put("title", "闹钟秤测试");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //21
        map.put("title", "众筹闹钟秤测试");
        map.put("info", "型号：IF1940B");
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
