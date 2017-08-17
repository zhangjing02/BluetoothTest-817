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

package com.senssun.bluetooth.tools.food;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckedTextView;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.relative.Information;

import java.util.ArrayList;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class FoodDeviceControlActivity extends Activity {
	private final static String TAG = FoodDeviceControlActivity.class.getSimpleName();

	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

	private CheckedTextView mCheckField;
	private TextView mConnectionState;
	private TextView mDataField;
	private TextView mRssiField;
	private String mDeviceName;
	private String mDeviceAddress;
	private FoodBluetoothLeService mBluetoothLeService;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
			new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private boolean mConnected = false;
	private BluetoothGattCharacteristic mNotifyCharacteristic;
	private BluetoothGattCharacteristic mWriteCharacteristic;
	private boolean mChceked=false;

	public final static byte[] sendBuffer = new byte[]{(byte) Integer.parseInt("A5", 16),
		(byte) Integer.parseInt("5A", 16),0,0,0,0,0,
		(byte) Integer.parseInt("5A", 16)};//


	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((FoodBluetoothLeService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up initialization.
			mBluetoothLeService.connect(mDeviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	// Handles various events fired by the Service.
	// ACTION_GATT_CONNECTED: connected to a GATT server.
	// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	// ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
	//                        or notification operations.
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (FoodBluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				updateConnectionState(R.string.connected);
				invalidateOptionsMenu();
			} else if (FoodBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				mConnected = false;
				updateConnectionState(R.string.disconnected);
				invalidateOptionsMenu();
				clearUI();
				SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
				if(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
					onBackPressed();
				}
			} else if (FoodBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				displayGattServices(mBluetoothLeService.getSupportedGattServices());
			} else if (FoodBluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayData(intent.getStringExtra(FoodBluetoothLeService.EXTRA_DATA));
			}else if (FoodBluetoothLeService.ACTION_DATA_RSSI.equals(action)) {
				displayRssi(intent.getStringExtra("Rssi"));
			}
		}
	};


	private void clearUI() {
		mDataField.setText(R.string.no_data);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_food_device_control);

		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

		// Sets up UI references.
		((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
		mConnectionState = (TextView) findViewById(R.id.connection_state);
		mDataField = (TextView) findViewById(R.id.data_value);
		mRssiField= (TextView) findViewById(R.id.rssi_value);
		mCheckField= (CheckedTextView) findViewById(R.id.check_value);

		getActionBar().setTitle(R.string.menu_back);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		findViewById(R.id.button1).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				onBackPressed();
			}
		});
		
		Intent gattServiceIntent = new Intent(this, FoodBluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		if (mBluetoothLeService != null) {
			final boolean result = mBluetoothLeService.connect(mDeviceAddress);
			Log.d(TAG, "Connect request result=" + result);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mServiceConnection);
		mBluetoothLeService = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.gatt_services, menu);
		if (mConnected) {
			menu.findItem(R.id.menu_connect).setVisible(false);
			menu.findItem(R.id.menu_disconnect).setVisible(true);
		} else {
			menu.findItem(R.id.menu_connect).setVisible(true);
			menu.findItem(R.id.menu_disconnect).setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.menu_connect:
			mBluetoothLeService.connect(mDeviceAddress);
			return true;
		case R.id.menu_disconnect:
			mBluetoothLeService.disconnect();
			return true;
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateConnectionState(final int resourceId) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mConnectionState.setText(resourceId);
			}
		});
	}

	private void displayRssi(String Rssi) {
		mRssiField.setText(Rssi);
	}
	private void displayData(String data) {
		if (data != null) {
			String[] strdata=data.split("-");	
			Log.e("Notification", data);
			if(strdata.length>=8){
				if (strdata[6].equals("AA")||strdata[6].equals("A0")){
					String tmpNum = strdata[2] + strdata[3];
					int weight = Integer.valueOf(tmpNum, 16);
					mDataField.setText(String.valueOf(weight));

					if(!mChceked) {
						mWriteCharacteristic.setValue(sendBuffer);
						mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
					}
				}
				if (strdata[1].equals("A5")&&strdata[2].equals("5A")&&!(strdata[6].equals("AA")||strdata[6].equals("A0"))) {
					mCheckField.setChecked(true);
					mChceked=true;
					SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
					if(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
						Intent intent = new Intent();
						Bundle bundle = new Bundle();
						bundle.putString("BleSuc", mDeviceAddress);
						intent.putExtras(bundle);
						setResult(RESULT_OK, intent);
						finish();
					}
				}
			}
		}

		mBluetoothLeService.getmBluetoothGatt().readRemoteRssi();
	}

	// Demonstrates how to iterate through the supported GATT Services/Characteristics.
	// In this sample, we populate the data structure that is bound to the ExpandableListView
	// on the UI.
	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null) return;
		for (BluetoothGattService gattService : gattServices) {

			if (gattService.getUuid().toString().equals("0000ffb0-0000-1000-8000-00805f9b34fb")) {
				List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
				for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
					if (characteristic.getUuid().toString().trim().equals("0000ffb2-0000-1000-8000-00805f9b34fb")){
						mBluetoothLeService.setCharacteristicNotification(characteristic,true);
						mNotifyCharacteristic = characteristic;
					}
					if (characteristic.getUuid().toString().trim().equals("0000ffb2-0000-1000-8000-00805f9b34fb")){
						mWriteCharacteristic=characteristic;
					}
				}
			}
			if (gattService.getUuid().toString().equals("0000fff0-0000-1000-8000-00805f9b34fb")) {
				List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
				for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
					if (characteristic.getUuid().toString().trim().equals("0000fff1-0000-1000-8000-00805f9b34fb")){
						mBluetoothLeService.setCharacteristicNotification(characteristic,true);
						mNotifyCharacteristic = characteristic;
					}
					if (characteristic.getUuid().toString().trim().equals("0000fff2-0000-1000-8000-00805f9b34fb")){
						mWriteCharacteristic=characteristic;
					}
				}
			}
		}
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(FoodBluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(FoodBluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(FoodBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(FoodBluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(FoodBluetoothLeService.ACTION_DATA_RSSI);
		return intentFilter;
	}
}
