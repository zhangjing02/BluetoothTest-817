package com.senssun.bluetooth.tools.aliweight_new.aliweght_latest;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.relative.WheelViewSelect02;
import com.senssun.bluetooth.tools.relative.WheelViewSelect03;
import com.senssun.bluetooth.tools.relative.WheelViewSelect04;

import widget.TosAdapterView;
import widget.WheelView;

public class SettingActivity extends Activity {

    private SharedPreferences mySharedPreferences;
    private SharedPreferences.Editor editor;
    private float limitWeight, limitPrecision;
    private Button mUpdate_btn, mChange_user_btn;
    private EditText mNewPassword_et, mWeight_et, mAge_et, mHeight_et;

    private RadioButton mFamle, mMale;
    private WheelView projectWheel_down, projectWheel02, mWeight_wheel_in, mWeight_wheel_f, mFat_wheel_in, mFat_wheel_f;
    private int x = 0;
    private int weight_up, weight_down;
    private Button mEnable_btn;
    private boolean isEdit = false;

    private int weight_in, fat_in;
    private float weight_f, fat_f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        getActionBar().setTitle(R.string.menu_back_ali_latest);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        init();
        //复检模式的，体重选择
        WheelViewSelect02.viewProjectNum(projectWheel_down, this);
        projectWheel_down.setOnItemSelectedListener(new TosAdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(TosAdapterView<?> parent, View view, int position, long id) {
                Log.i("shuaxin", "体重下限是？" + position);
                weight_down = position + 10;
            }

            public void onNothingSelected(TosAdapterView<?> parent) {
            }
        });
        projectWheel_down.setSelection(mySharedPreferences.getInt("weight_down", 100) - 10);

        //复检模式的，体重精度
        WheelViewSelect03.viewProjectNum(projectWheel02, this);
        projectWheel02.setOnItemSelectedListener(new TosAdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(TosAdapterView<?> parent, View view, int position, long id) {
                limitPrecision = position / 10f;
                Log.i("shuaxin", "精度是？" + x);
            }

            @Override
            public void onNothingSelected(TosAdapterView<?> parent) {
            }
        });
        projectWheel02.setSelection((int) (mySharedPreferences.getFloat("limitPrecision", 5 / 10f) * 10));

        //自动测试，体重选择
        WheelViewSelect04.viewProjectNum(mWeight_wheel_in, this);
        mWeight_wheel_in.setOnItemSelectedListener(new TosAdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(TosAdapterView<?> parent, View view, int position, long id) {
                Log.i("gengxin001", "体重下限是？" + position);
                weight_in = position;
                Log.i("gengxin002", "体重下限是？" + weight_in);
            }

            @Override
            public void onNothingSelected(TosAdapterView<?> parent) {

            }
        });
        mWeight_wheel_in.setSelection(mySharedPreferences.getInt("weight_in", 100));

        //自动测试，体重精度
        WheelViewSelect03.viewProjectNum(mWeight_wheel_f, this);
        mWeight_wheel_f.setOnItemSelectedListener(new TosAdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(TosAdapterView<?> parent, View view, int position, long id) {
                weight_f = position / 10f;

            }

            @Override
            public void onNothingSelected(TosAdapterView<?> parent) {
            }
        });
        mWeight_wheel_f.setSelection((int) (mySharedPreferences.getFloat("weight_f", 5 / 10f) * 10));

        //自动测试，脂肪选择
        WheelViewSelect02.viewProjectNum(mFat_wheel_in, this);
        mFat_wheel_in.setOnItemSelectedListener(new TosAdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(TosAdapterView<?> parent, View view, int position, long id) {

                fat_in = position + 10;
            }

            public void onNothingSelected(TosAdapterView<?> parent) {
            }
        });
        mFat_wheel_in.setSelection(mySharedPreferences.getInt("fat_in", 100) - 10);

        //自动测试，脂肪精度
        WheelViewSelect03.viewProjectNum(mFat_wheel_f, this);
        mFat_wheel_f.setOnItemSelectedListener(new TosAdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(TosAdapterView<?> parent, View view, int position, long id) {
                fat_f = position / 10f;

            }

            @Override
            public void onNothingSelected(TosAdapterView<?> parent) {
            }
        });
        mFat_wheel_f.setSelection((int) (mySharedPreferences.getFloat("fat_f", 5 / 10f) * 10));


        mUpdate_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("password", String.valueOf(mNewPassword_et.getText()));
                editor.putInt("weight_down", weight_down);
                editor.putInt("weight_in", weight_in);
                editor.putFloat("weight_f", weight_f);
                editor.putInt("fat_in", fat_in);
                editor.putFloat("fat_f", fat_f);
                editor.putFloat("limitPrecision", limitPrecision);

                editor.commit();
                finish();
            }
        });
        mChange_user_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("user_weight", String.valueOf(mWeight_et.getText()));
                editor.putString("user_height", String.valueOf(mHeight_et.getText()));
                editor.putString("user_age", String.valueOf(mAge_et.getText()));
                editor.putBoolean("user_famle", mFamle.isChecked());
                editor.commit();
            }
        });

    }

    private void init() {
        mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
        editor = mySharedPreferences.edit();
        mUpdate_btn = (Button) findViewById(R.id.update_btn);
        mNewPassword_et = (EditText) findViewById(R.id.new_password_et);
        mWeight_wheel_in = (WheelView) findViewById(R.id.projectWheel_weight);
        mWeight_wheel_f = (WheelView) findViewById(R.id.projectWheel_weightprecision);
        mFat_wheel_in = (WheelView) findViewById(R.id.projectWheel_fat);
        mFat_wheel_f = (WheelView) findViewById(R.id.projectWheel_fatprecision);


        mChange_user_btn = (Button) findViewById(R.id.btnEdit);
        mWeight_et = (EditText) findViewById(R.id.etWeight);
        mHeight_et = (EditText) findViewById(R.id.etHeight);
        mAge_et = (EditText) findViewById(R.id.etAge);
        mMale = (RadioButton) findViewById(R.id.rbMale);
        mFamle = (RadioButton) findViewById(R.id.rbFemale);

        mWeight_et.setText(mySharedPreferences.getString("user_weight", "60"));
        mHeight_et.setText(mySharedPreferences.getString("user_height", "165"));
        mAge_et.setText(mySharedPreferences.getString("user_age", "25"));

        mWeight_et.setEnabled(isEdit);
        mHeight_et.setEnabled(isEdit);
        mAge_et.setEnabled(isEdit);
        mNewPassword_et.setEnabled(isEdit);


        if (mySharedPreferences.getBoolean("user_famle", true)) {
            mFamle.setChecked(true);
            mMale.setChecked(false);
        } else {
            mMale.setChecked(true);
            mFamle.setChecked(false);
        }
        String Store_password = mySharedPreferences.getString("password", "654321");
        mNewPassword_et.setText(Store_password);
        projectWheel_down = (WheelView) findViewById(R.id.projectWheel_down);
        projectWheel02 = (WheelView) findViewById(R.id.projectWheel02);

        mEnable_btn = (Button) findViewById(R.id.enabel_Edit);
        mEnable_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isEdit = !isEdit;
                mWeight_et.setEnabled(isEdit);
                mHeight_et.setEnabled(isEdit);
                mAge_et.setEnabled(isEdit);
                mNewPassword_et.setEnabled(isEdit);

            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return true;
    }

    public void refresh() {
        onCreate(null);
    }

}
