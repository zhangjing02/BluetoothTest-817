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
import com.senssun.bluetooth.tools.food.FoodDeviceScanActivity;
import com.senssun.bluetooth.tools.food.iN8301B.iN8301BFoodDeviceScanActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NutritionActivity extends Activity {
    private ListView mNutrition_lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutrition);


        Log.i("zhangjing","营养秤");
        getActionBar().setTitle("营养秤");
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mNutrition_lv= (ListView) findViewById(R.id.nutrition_lv);

        SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.item_device,
                new String[]{"title", "info", "image"},
                new int[]{R.id.deviceName, R.id.deviceModel, R.id.deviceIcon});

        mNutrition_lv.setAdapter(adapter);
        mNutrition_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

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
                //营养秤
                intent.setClass(this, FoodDeviceScanActivity.class);//
                startActivity(intent);
                break;

            case 1://营养秤(iN8301B)
                intent.setClass(this, iN8301BFoodDeviceScanActivity.class);//
                startActivity(intent);
                break;

        }
    }

    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map;

        map = new HashMap<String, Object>();  //4
        map.put("title", "营养秤");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_food);
        list.add(map);

        map = new HashMap<String, Object>();  //5
        map.put("title", "营养秤(iN8301B)");
        map.put("info", "型号：001");
        map.put("image", R.drawable.icon_food);
        list.add(map);
        return list;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

}
