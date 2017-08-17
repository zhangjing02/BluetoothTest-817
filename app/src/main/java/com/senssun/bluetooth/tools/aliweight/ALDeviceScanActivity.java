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

package com.senssun.bluetooth.tools.aliweight;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.entity.BluetoothDeviceObject;
import com.senssun.bluetooth.tools.relative.Information;
import com.senssun.bluetooth.tools.relative.WheelViewSelect;

import widget.TosAdapterView;
import widget.WheelView;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class ALDeviceScanActivity extends Activity {
	private String TAG = ALDeviceScanActivity.class.getSimpleName();
	private SharedPreferences mySharedPreferences;
	private int limitRssi;
	private LeDeviceListAdapter mLeDeviceListAdapter;
	private LeSucMacListAdapter mLeSucMacListAdapter;
	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning;
	private Handler mHandler;

	private static final int REQUEST_CONNECT_BT = 2;
	private static final long SCAN_PERIOD = 1000;
	private String mConnName="SENSSUN FAT";
	private ListView scan_list;
	private ListView suc_list;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ali_device_scan);
		getActionBar().setTitle(R.string.menu_back_ali);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		mHandler = new Handler();

		mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);

		scan_list=(ListView)findViewById(R.id.scan_list);
		suc_list=(ListView)findViewById(R.id.suc_list);


		final BluetoothManager bluetoothManager =
				(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		mLeSucMacListAdapter=new LeSucMacListAdapter();
		suc_list.setAdapter(mLeSucMacListAdapter);

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

		CheckBox checkBox=(CheckBox)findViewById(R.id.autoCheck);
		checkBox.setChecked(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true));
		checkBox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences.Editor editor = mySharedPreferences.edit();
				editor.putBoolean(Information.DB.AutoCheck, ((CheckBox) v).isChecked());
				editor.commit();
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
			public void onItemClick(AdapterView<?> parent,  View v, int position, long id) {
				final BluetoothDeviceObject device = mLeDeviceListAdapter.getDevice(position);
				if (device == null) return;
				final Intent intent = new Intent(ALDeviceScanActivity.this, ALDeviceControlActivity.class);
				intent.putExtra(ALDeviceControlActivity.EXTRAS_DEVICE_NAME, device.getDevice().getName());
				intent.putExtra(ALDeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getDevice().getAddress());
				if (mScanning) {
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					mScanning = false;
				}
				startActivityForResult(intent,REQUEST_CONNECT_BT);
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
					if(mScanning){
						mScanning = false;
						mBluetoothAdapter.stopLeScan(mLeScanCallback);
						if(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)){
							if(mLeDeviceListAdapter.getmLeDevices().size()>0){
								final BluetoothDeviceObject device = mLeDeviceListAdapter.getDevice(0);
								if (device == null) return;
								final Intent intent = new Intent(ALDeviceScanActivity.this, ALDeviceControlActivity.class);
								intent.putExtra(ALDeviceControlActivity.EXTRAS_DEVICE_NAME, device.getDevice().getName());
								intent.putExtra(ALDeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getDevice().getAddress());
								intent.putExtra(ALDeviceControlActivity.EXTRAS_DEVICE_MODEL, device.getDeviceMode());
								if (mScanning) {
									mBluetoothAdapter.stopLeScan(mLeScanCallback);
									mScanning = false;
								}
								startActivityForResult(intent,REQUEST_CONNECT_BT);
							}
						}
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
			mInflator = ALDeviceScanActivity.this.getLayoutInflater();
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

	// Adapter for holding devices found through scanning.
	private class LeSucMacListAdapter extends BaseAdapter {
		private ArrayList<String> mLeDevices;
		private LayoutInflater mInflator;

		public LeSucMacListAdapter() {
			super();
			mLeDevices = new ArrayList<String>();
			mInflator = ALDeviceScanActivity.this.getLayoutInflater();
		}

		public void addDevice(String device) {
			if(!mLeDevices.contains(device)) {
				mLeDevices.add(0,device);
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
				public void onLeScan(final BluetoothDevice device, final int rssi,final byte[] scanRecord) {

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if(limitRssi<Math.abs(rssi))return;
							if(device.getName()==null)return;
							if(!device.getName().equals(mConnName))return;
							String pair=String.format("%02X ", scanRecord[9]).trim()+String.format("%02X ", scanRecord[10]).trim()+String.format("%02X ", scanRecord[11]).trim()+String.format("%02X ", scanRecord[12]).trim()+String.format("%02X ", scanRecord[13]).trim();
							if(!(pair.equals("A8010101A3")||pair.equals("A8010101A2")))return;
							if(mLeSucMacListAdapter.getmLeDevices().contains(device.getAddress()))return;
							BluetoothDeviceObject bleDevice=new BluetoothDeviceObject();
							bleDevice.setDevice(device);
							bleDevice.setRssi(rssi);
							if(pair.equals("A8010101A3")) bleDevice.setDeviceMode(2);
							if(pair.equals("A8010101A2")) bleDevice.setDeviceMode(1);
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