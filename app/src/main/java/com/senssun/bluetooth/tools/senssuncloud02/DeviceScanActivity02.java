package com.senssun.bluetooth.tools.senssuncloud02;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.relative.Information;
import com.senssun.bluetooth.tools.relative.WheelViewSelect;

import java.util.ArrayList;
import java.util.List;

import cn.senssun.ble.sdk.BleDevice;
import cn.senssun.ble.sdk.BleScan;
import cn.senssun.ble.sdk.BleScan.OnScanListening;
import widget.TosAdapterView;
import widget.WheelView;

public class DeviceScanActivity02 extends Activity {

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mAdapter;
    private boolean mScanning;
    private BleScan bleScan;
    private RadioButton mAuto_check;
    private ListView mScan_lv;
    private SharedPreferences mySharedPreferences;
    private int limitRssi;
    private int linshiRssi;
    private CheckBox checkBox;

    // 5秒后停止查找搜索.
    private static final int SCAN_PERIOD = 90000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_senssunfat_scan_layout02);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        bleScan=new BleScan();
        mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);


        checkBox = (CheckBox) findViewById(R.id.autoCheck);
        checkBox.setChecked(mySharedPreferences.getBoolean(Information.DB.AutoCheck02, true));
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = mySharedPreferences.edit();
                editor.putBoolean(Information.DB.AutoCheck02, ((CheckBox) v).isChecked());
                editor.commit();
                onResume();
            }
        });


        mScan_lv = (ListView) findViewById(R.id.scan_list02);
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        WheelView projectWheel = (WheelView) findViewById(R.id.projectWheel222);
        WheelViewSelect.viewProjectNum(projectWheel, this);
        projectWheel.setOnItemSelectedListener(new TosAdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(TosAdapterView<?> parent, View view, int position, long id) {
                SharedPreferences.Editor editor = mySharedPreferences.edit();
                editor.putInt(Information.DB.LimitRssi, position + 40);
                editor.commit();

                limitRssi = position + 40;
            }

            public void onNothingSelected(TosAdapterView<?> parent) {
            }
        });
        linshiRssi = mySharedPreferences.getInt(Information.DB.LimitRssi, 40);
        if (linshiRssi < 130) {
            Log.i("zhangjing", "正常");
            projectWheel.setSelection(linshiRssi - 40);
        } else {
            Log.i("zhangjing", "其他");
            projectWheel.setSelection(limitRssi);
        }

        if (!mAdapter.isEnabled()) {
            Intent intentOpen = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intentOpen, 2);
        } else {
            bleScan.Create(this);
        }

        bleScan.setOnScanListening(new OnScanListening() {

            @Override
            public void OnListening(BleDevice bleDevice) {
                // TODO Auto-generated method stub
                mLeDeviceListAdapter.addDevice(bleDevice);
                mLeDeviceListAdapter.notifyDataSetChanged();



                if (mySharedPreferences.getBoolean(Information.DB.AutoCheck02, true)) {

                    if (limitRssi >= 0) {
                        if (("SENSSUN CLOUD").equals(bleDevice.getBluetoothDevice().getName()) && Math.abs(bleDevice.getRssi()) < limitRssi) {

                            final Intent intent = new Intent(DeviceScanActivity02.this, DeviceControlCloudActivity02.class);
                            Bundle bundle = new Bundle();
                            bundle.putParcelable(BleScan.EXTRAS_DEVICE, bleDevice);
                            //bundle.putExtra(BleScan.EXTRAS_DEVICE, device);
                            intent.putExtras(bundle);
                            bleScan.scanLeStopDevice();
                            startActivity(intent);

                        }
                    }
                }
            }
        });

        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mScan_lv.setAdapter(mLeDeviceListAdapter);

        mScan_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BleDevice device = mLeDeviceListAdapter.getDevice(position);
                switch (device.getDeviceType()) {
                    case BleDevice.CloudScale: {
                        final Intent intent = new Intent(DeviceScanActivity02.this, DeviceControlCloudActivity02.class);
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(BleScan.EXTRAS_DEVICE, device);
                        intent.putExtras(bundle);
                        bleScan.scanLeStopDevice();
                        startActivity(intent);
                        break;
                    }
                }
                mScanning = false;
                invalidateOptionsMenu();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_cloud_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();

                bleScan.ScanLeStartDevice(90000);
                mScanning = true;
                invalidateOptionsMenu();
                break;
            case R.id.menu_stop:
                bleScan.scanLeStopDevice();
                mScanning = false;
                invalidateOptionsMenu();
                break;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 2:
                if (resultCode == -1) {
                    bleScan.Create(DeviceScanActivity02.this);
                } else {
                    finish();
                }
                break;
        }
    }

    private final BroadcastReceiver mGattUpdateReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //处理已发现设备
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (BluetoothAdapter.STATE_ON == mAdapter.getState()) {
                    bleScan.Create(DeviceScanActivity02.this);
                }
            }
        }
    };

    private static IntentFilter makeUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return intentFilter;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceive, makeUpdateIntentFilter());

        mLeDeviceListAdapter.clear();
        bleScan.ScanLeStartDevice(530000);
        mScanning = true;
        invalidateOptionsMenu();


    }

    @Override
    protected void onPause() {
        super.onPause();
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mGattUpdateReceive);
        bleScan.scanLeStopDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private List<BleDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BleDevice>();
            mInflator = DeviceScanActivity02.this.getLayoutInflater();
        }

        public void addDevice(BleDevice device) {
            if (!mLeDevices.contains(device)) {
                if (Math.abs(device.getRssi()) < limitRssi) {
                    mLeDevices.add(device);
                }
            }
        }

        public void setmLeDevices(List<BleDevice> mLeDevices) {
            this.mLeDevices = mLeDevices;
        }

        public BleDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_cloud_device02, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceRssi = (TextView) view.findViewById(R.id.device_rssi);
                viewHolder.broastCast = (TextView) view.findViewById(R.id.broastCast);
                viewHolder.manuData = (TextView) view.findViewById(R.id.manuData);

                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            BleDevice device = mLeDevices.get(i);
            if (device != null) {
               // viewHolder.deviceAddress.setText(device.getBluetoothDevice().getAddress());
                viewHolder.deviceAddress.setText(device.getManuData());
                viewHolder.deviceName.setText(device.getBluetoothDevice().getName());
                viewHolder.deviceRssi.setText("Rssi:" + device.getRssi());

                byte[] broadCast = device.getBroadCast();
                StringBuffer stringBuffer = new StringBuffer();
                if (broadCast != null) {
                    for (byte byteChar : broadCast) {
                        String ms = String.format("%02X ", byteChar).trim();
                        stringBuffer.append(ms);
                    }
                }
                viewHolder.broastCast.setText(stringBuffer.toString());

                viewHolder.manuData.setText(String.valueOf(device.getManuData()));
            }
            return view;
        }
    }
    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
        TextView broastCast;
        TextView manuData;
    }
}