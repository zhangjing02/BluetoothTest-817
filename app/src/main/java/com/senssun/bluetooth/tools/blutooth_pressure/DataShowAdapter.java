package com.senssun.bluetooth.tools.blutooth_pressure;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by zhangj on 2017/8/17.
 */

public class DataShowAdapter extends BaseAdapter {

    private ArrayList<Map<String,String>> list = new ArrayList<>();
    private ViewHolder mHolder;
    private LayoutInflater mInflater;
    private Context mContext;

    public DataShowAdapter(ArrayList<Map<String,String>> list, Context mContext) {
        this.list = list;
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        mHolder = null;
        if (convertView == null) {
            convertView = mInflater.from(mContext).inflate(R.layout.item_bluepressure_data_show, parent, false);
            mHolder = new ViewHolder();
            mHolder.date = (TextView) convertView.findViewById(R.id.date_item);
            mHolder.Num = (TextView) convertView.findViewById(R.id.data_item);
            convertView.setTag(mHolder);
        } else {
            mHolder= (ViewHolder) convertView.getTag();
        }

        mHolder.date.setText(list.get(position).get("date")+"--"+position+"--");
        mHolder.Num.setText(list.get(position).get("data"));
        return convertView;
    }


    public static class ViewHolder {
        private TextView date;
        private TextView Num;
    }

}
