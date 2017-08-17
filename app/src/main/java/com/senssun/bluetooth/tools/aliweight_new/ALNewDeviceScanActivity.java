/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.senssun.bluetooth.tools.aliweight_new;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.aliconfigweight.ALConfigDeviceControlActivity;
import com.senssun.bluetooth.tools.entity.BluetoothDeviceObject;
import com.senssun.bluetooth.tools.relative.Information;
import com.senssun.bluetooth.tools.relative.WheelViewSelect;

import java.util.ArrayList;

import widget.TosAdapterView;
import widget.WheelView;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class ALNewDeviceScanActivity extends Activity {
    private String TAG = ALNewDeviceScanActivity.class.getSimpleName();
    private SharedPreferences mySharedPreferences;
    private int limitRssi;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private LeSucMacListAdapter mLeSucMacListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_CONNECT_BT = 2;
    private static final long SCAN_PERIOD = 1000;
    private String mConnName = "SENSSUN FAT";
    private ListView scan_list;
    private ListView suc_list;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ali_test_device_scan);
        getActionBar().setTitle(R.string.menu_back_ali_new);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mHandler = new Handler();

        mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);

        scan_list = (ListView) findViewById(R.id.scan_list);
        suc_list = (ListView) findViewById(R.id.suc_list);


        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mLeSucMacListAdapter = new LeSucMacListAdapter();
        suc_list.setAdapter(mLeSucMacListAdapter);

        limitRssi = mySharedPreferences.getInt(Information.DB.LimitRssi, 0);

        WheelView projectWheel = (WheelView) findViewById(R.id.projectWheel);
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

        RadioGroup aliCheck = (RadioGroup) findViewById(R.id.aliCheck);
        switch (mySharedPreferences.getInt(Information.DB.AliCheck, 0)) {
            case 0:
                aliCheck.check(R.id.noAutoCheck);
                break;
            case 1:
                aliCheck.check(R.id.autoCheckSend1);
                break;
            case 2:
                aliCheck.check(R.id.autoDissHisSend1);
                break;
            case 3:
                aliCheck.check(R.id.autoCheck);
                break;
        }

        aliCheck.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                SharedPreferences.Editor editor = mySharedPreferences.edit();
                switch (i) {
                    case R.id.noAutoCheck:
                        editor.putInt(Information.DB.AliCheck, 0);
                        break;
                    case R.id.autoCheckSend1:
                        editor.putInt(Information.DB.AliCheck, 1);
                        break;
                    case R.id.autoDissHisSend1:
                        editor.putInt(Information.DB.AliCheck, 2);
                        break;
                    case R.id.autoCheck:
                        editor.putInt(Information.DB.AliCheck, 3);
                        break;
                }
                editor.commit();
            }
        });

//		checkBox.setChecked(mySharedPreferences.getInt(Information.DB.AliCheck, 0));
//		checkBox.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				SharedPreferences.Editor editor = mySharedPreferences.edit();
//				editor.putBoolean(Information.DB.AutoCheck, ((CheckBox) v).isChecked());
//				editor.commit();
//			}
//		});
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
        scan_list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                final BluetoothDeviceObject device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;
                final Intent intent = new Intent(ALNewDeviceScanActivity.this, ALNewDeviceControlActivity.class);
                intent.putExtra(ALNewDeviceControlActivity.EXTRAS_DEVICE_NAME, device.getDevice().getName());
                intent.putExtra(ALNewDeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getDevice().getAddress());
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
                startActivityForResult(intent, REQUEST_CONNECT_BT);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_CONNECT_BT && resultCode == RESULT_OK) {
            String bleSucAddress = data.getStringExtra("BleSuc");
            //			noConnectAddress.add(bleSucAddress);
            mLeSucMacListAdapter.addDevice(bleSucAddress);
            mLeSucMacListAdapter.notifyDataSetChanged();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
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
                    if (mScanning) {
                        mScanning = false;
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        if (mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 3 || mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 2 || mySharedPreferences.getInt(Information.DB.AliCheck, 0) == 1) {
                            if (mLeDeviceListAdapter.getmLeDevices().size() > 0) {
                                final BluetoothDeviceObject device = mLeDeviceListAdapter.getDevice(0);
                                if (device == null) return;
                                final Intent intent = new Intent(ALNewDeviceScanActivity.this, ALNewDeviceControlActivity.class);
                                intent.putExtra(ALConfigDeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getDevice().getAddress());
                                if (mScanning) {
                                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                    mScanning = false;
                                }
                                startActivityForResult(intent, REQUEST_CONNECT_BT);
                            }
                        }
                        scanLeDevice(true);
                    } else {
                        mScanning = false;
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        invalidateOptionsMenu();
                    }
                }
            }, SCAN_PERIOD);
            if (!mScanning) {
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
            mInflator = ALNewDeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDeviceObject device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            } else {
                int postion = mLeDevices.indexOf(device);
                mLeDevices.remove(device);
                mLeDevices.add(postion, device);
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
            viewHolder.deviceAddress.setText(device.getDevice().getAddress() + "    Rssi:" + device.getRssi());

            return view;
        }
    }

    // Adapter for holding devices found through scanning.
    private class LeSucMacListAdapter extends BaseAdapter {
        private ArrayList<String> mLeDevices;
        private LayoutInflater mInflator;

        public LeSucMacListAdapter() {
            super();
            mLeDevices = new ArrayList<String>();
            mInflator = ALNewDeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(String device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(0, device);
            }
        }

        public ArrayList<String> getmLeDevices() {
            return mLeDevices;
        }

        public String getDevice(int position) {
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

            String device = mLeDevices.get(i);
            viewHolder.deviceAddress.setText(device);

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (limitRssi < Math.abs(rssi)) return;
                            if (device.getName() == null) return;
                            if (!mConnName.contains(device.getName())) return;
//							String pair=String.format("%02X ", scanRecord[9]).trim()+String.format("%02X ", scanRecord[10]).trim()+String.format("%02X ", scanRecord[11]).trim()+String.format("%02X ", scanRecord[12]).trim()+String.format("%02X ", scanRecord[13]).trim();
//							if(!(pair.equals("A8010101A3")||pair.equals("A8010101A2")))return;
                            if (mLeSucMacListAdapter.getmLeDevices().contains(device.getAddress()))
                                return;
                            BluetoothDeviceObject bleDevice = new BluetoothDeviceObject();
                            bleDevice.setDevice(device);
                            bleDevice.setRssi(rssi);
//							if(pair.equals("A8010101A3")) bleDevice.setDeviceMode(2);
//							if(pair.equals("A8010101A2")) bleDevice.setDeviceMode(1);
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