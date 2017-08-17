
package com.senssun.bluetooth.tools.adapter;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;


import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.entity.MessObject;


public class DeviceAdapter extends BaseAdapter {
    private class buttonViewHolder {
        TextView deviceName;//设备名字
        TextView deviceRssi; //设备信号
        TextView deviceMess;//设备内容
        TextView deviceMac;//Mac地址

        CheckedTextView sendProductSuc;//接收产品信息成功
    }

    private List<MessObject> mAppList;
    private LayoutInflater mInflater;
    private Context mContext;
    private buttonViewHolder holder;
    private Activity activity;
    private DateFormat format = new SimpleDateFormat("yyyy-M-d");
    //	private GridView gridView;

    public DeviceAdapter(Activity c, List<MessObject> appList) {
        this.activity = c;
        mAppList = appList;
        mContext = c;
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setBlueList(List<MessObject> AppList) {
        this.mAppList = AppList;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return mAppList.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView != null) {
            holder = (buttonViewHolder) convertView.getTag();
        } else {
            convertView = mInflater.inflate(R.layout.item_list_device, null);
            holder = new buttonViewHolder();
            holder.deviceName = (TextView) convertView.findViewById(R.id.deviceName);
            holder.deviceRssi = (TextView) convertView.findViewById(R.id.deviceRssi);
            holder.deviceMess = (TextView) convertView.findViewById(R.id.deviceMess);
            holder.deviceMac = (TextView) convertView.findViewById(R.id.deviceMac);

            holder.sendProductSuc = (CheckedTextView) convertView.findViewById(R.id.sendSuc);
            convertView.setTag(holder);
        }

        MessObject appInfo = mAppList.get(mAppList.size() - 1 - position);
        if (appInfo != null) {
            holder.deviceName.setText(appInfo.getName());
            holder.deviceRssi.setText(String.valueOf(appInfo.getRssi()));
            holder.deviceMess.setText(String.valueOf(appInfo.getMess()));
            holder.deviceMac.setText(String.valueOf(appInfo.getAddress()));
            holder.sendProductSuc.setChecked(appInfo.isSendProductSuc());
        }
        return convertView;
    }
}
