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
import com.senssun.bluetooth.tools.mdweight.MDDeviceScanActivity;
import com.senssun.bluetooth.tools.mdweight.MDDeviceScanActivity02;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MideaActivity extends Activity {

    private ListView mMidea_lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_midea);

        getActionBar().setTitle("美的系列");
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mMidea_lv= (ListView) findViewById(R.id.midea_lv);

        SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.item_device,
                new String[]{"title", "info", "image"},
                new int[]{R.id.deviceName, R.id.deviceModel, R.id.deviceIcon});

        mMidea_lv.setAdapter(adapter);
        mMidea_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

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
            case 0://美的体脂秤
                intent.setClass(this, MDDeviceScanActivity.class);
                intent.putExtra(MDDeviceScanActivity.modeStr, MDDeviceScanActivity.mode_standard);
                startActivity(intent);
                break;
            case 1://美的体脂秤（测试）
                intent.setClass(this, MDDeviceScanActivity.class);
                intent.putExtra(MDDeviceScanActivity.modeStr, MDDeviceScanActivity.mode_test);
                startActivity(intent);
                break;
            case 2://美的体脂秤（测试）
                intent.setClass(this, MDDeviceScanActivity02.class);
                intent.putExtra(MDDeviceScanActivity.modeStr, MDDeviceScanActivity.mode_standard);
                startActivity(intent);
                break;

        }
    }

    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        Map<String, Object> map ;  //1
        map = new HashMap<String, Object>();  //13
        map.put("title", "美的体脂秤");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);

        map = new HashMap<String, Object>();  //14
        map.put("title", "美的体脂秤(测试)");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_fat);
        list.add(map);
        map = new HashMap<String, Object>();  //14
        map.put("title", "美的体脂秤(最新测试)");
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
