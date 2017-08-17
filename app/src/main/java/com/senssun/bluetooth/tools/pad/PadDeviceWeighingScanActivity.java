package com.senssun.bluetooth.tools.pad;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.entity.BluetoothDeviceObject;
import com.senssun.bluetooth.tools.relative.Information;
import com.senssun.bluetooth.tools.relative.WheelViewSelect;

import java.util.ArrayList;

import widget.TosAdapterView;
import widget.WheelView;

public class PadDeviceWeighingScanActivity extends Activity {
    private SharedPreferences mySharedPreferences;
    private int limitRssi;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_CONNECT_BT = 2;
    private static final long SCAN_PERIOD = 1000;
    private String mConnName="SENSSUN Pad";
    private ListView scan_list;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pad_device_weighing_scan);
        getActionBar().setTitle(R.string.menu_back_pad_weighing);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mHandler = new Handler();

        mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);

        scan_list=(ListView)findViewById(R.id.scan_list);


        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();


        limitRssi=mySharedPreferences.getInt(Information.DB.LimitRssi,0);

        WheelView projectWheel =(WheelView) findViewById(R.id.projectWheel);
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
        projectWheel.setSelection(limitRssi - 40);

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
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        scan_list.setAdapter(mLeDeviceListAdapter);

        scanLeDevice(true);
        scan_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent,  View v, int position, long id) {
                final BluetoothDeviceObject device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;
                final Intent intent = new Intent(PadDeviceWeighingScanActivity.this, PadDeviceWeighingScanActivity.class);
                intent.putExtra(PadDeviceControlActivity.EXTRAS_DEVICE_NAME, device.getDevice().getName());
                intent.putExtra(PadDeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getDevice().getAddress());
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
                startActivityForResult(intent,REQUEST_CONNECT_BT);
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(mScanning){
                        mScanning = false;
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        scanLeDevice(true);
                    }else{
                        mScanning = false;
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        invalidateOptionsMenu();
                    }
                }
            }, SCAN_PERIOD);
            if(!mScanning){
                mScanning = true;
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDeviceObject> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDeviceObject>();
            mInflator = PadDeviceWeighingScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDeviceObject device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(0,device);
            }else{
                mLeDevices.remove(device);
                mLeDevices.add(0,device);
            }
        }

        public ArrayList<BluetoothDeviceObject> getmLeDevices() {
            return mLeDevices;
        }

        public BluetoothDeviceObject getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
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
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.item_address_list, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDeviceObject device = mLeDevices.get(i);
            viewHolder.deviceAddress.setText(device.getDevice().getAddress()+"    Rssi:"+device.getRssi());

            return view;
        }
    }


    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(limitRssi<Math.abs(rssi))return;
                            if(device.getName()==null)return;
                            if(!device.getName().equals(mConnName))return;
                            BluetoothDeviceObject bleDevice=new BluetoothDeviceObject();
                            bleDevice.setDevice(device);
                            bleDevice.setRssi(rssi);
                            mLeDeviceListAdapter.addDevice(bleDevice);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    static class ViewHolder {
        //		TextView deviceName;
        TextView deviceAddress;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanLeDevice(false);
    }

}