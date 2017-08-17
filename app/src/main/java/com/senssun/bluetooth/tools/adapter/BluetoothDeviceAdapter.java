
package com.senssun.bluetooth.tools.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.entity.BleDevice;

import java.util.ArrayList;


public class BluetoothDeviceAdapter extends BaseAdapter {
    private class ViewHolder {
        TextView deviceMac;
        TextView deviceRssi;
        TextView deviceMess;
        CheckedTextView sendSuc;
    }

    private ArrayList<BleDevice> mLeDevices;
    private LayoutInflater mInflator;

    public BluetoothDeviceAdapter(Context mContext) {
        super();
        mLeDevices = new ArrayList<BleDevice>();
        mInflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    public void addDevice(BleDevice device) {
        if(!mLeDevices.contains(device)) {
            mLeDevices.add(0,device);
        }else{
            for(int i=mLeDevices.size()-1;i>-1;i--){
                if (mLeDevices.get(i).getmDevice().getAddress().equals(device.getmDevice().getAddress())){
                    mLeDevices.get(i).setmNotiStr(device.getmNotiStr());
                    mLeDevices.get(i).setmSendSuc(device.ismSendSuc());
                    mLeDevices.get(i).setmRssi(device.getmRssi());
                }
            }
        }

    }

    public BleDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    public void clear() {
        mLeDevices.clear();
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return mLeDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder viewHolder;
        if (view == null) {
            view = mInflator.inflate(R.layout.item_list_device, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceMac = (TextView) view.findViewById(R.id.deviceMac);
            viewHolder.deviceRssi = (TextView) view.findViewById(R.id.deviceRssi);
            viewHolder.deviceMess = (TextView) view.findViewById(R.id.deviceMess);
            viewHolder.sendSuc = (CheckedTextView) view.findViewById(R.id.sendSuc);


            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        BleDevice device = mLeDevices.get(position);
        viewHolder.deviceMac.setText(device.getmDevice().getAddress());
        if (device.getmRssi()!=0){
            viewHolder.deviceRssi.setText(String.valueOf(device.getmRssi()));
        }
        if(viewHolder.sendSuc.isChecked()){

        }else{
            viewHolder.sendSuc.setChecked(device.ismSendSuc());
        }
        if(device.getmNotiStr()!=null){
            viewHolder.deviceMess.setText(device.getmNotiStr());
        }



        return view;
    }
}
