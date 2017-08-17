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

package com.senssun.bluetooth.tools.aliweight.old;

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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
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
public class OldALDeviceControlActivity extends Activity {
	private final static String TAG = com.senssun.bluetooth.tools.aliweight.ALDeviceControlActivity.class.getSimpleName();

	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	public static final String EXTRAS_DEVICE_MODEL = "DEVICE_MODEL";

	private CheckedTextView mCheckField;
	private TextView mConnectionState;
	private TextView mDataField;
	private TextView mRssiField;

	private String mDeviceName;
	private String mDeviceAddress;
	private int mDeviceModel;
	private OldALBluetoothLeService mBluetoothLeService;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
			new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private boolean mConnected = false;
	private boolean mDess=true;
	private BluetoothGattCharacteristic mNotifyCharacteristic;
	private BluetoothGattCharacteristic mWriteCharacteristic;
	private Button btnSend;
	private boolean autoCheck;



	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((OldALBluetoothLeService.LocalBinder) service).getService();
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
			if (OldALBluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				updateConnectionState(R.string.connected);
				invalidateOptionsMenu();
			} else if (OldALBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				mConnected = false;
				updateConnectionState(R.string.disconnected);
				invalidateOptionsMenu();
				clearUI();
				SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
				if(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
					onBackPressed();
				}
			} else if (OldALBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				displayGattServices(mBluetoothLeService.getSupportedGattServices());
			} else if (OldALBluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayData(intent.getStringExtra(OldALBluetoothLeService.EXTRA_DATA));
			}else if (OldALBluetoothLeService.ACTION_DATA_RSSI.equals(action)) {
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
		setContentView(R.layout.activity_ali_old_device_control);

		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
		mDeviceModel= intent.getIntExtra(EXTRAS_DEVICE_MODEL, 0);

		// Sets up UI references.
		((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
		mConnectionState = (TextView) findViewById(R.id.connection_state);
		mDataField = (TextView) findViewById(R.id.data_value);
		mRssiField= (TextView) findViewById(R.id.rssi_value);
		mCheckField= (CheckedTextView) findViewById(R.id.check_value);
		btnSend = (Button)findViewById(R.id.btnSend);
		SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
		autoCheck = mySharedPreferences.getBoolean(Information.DB.AutoCheck, true);
		if(autoCheck){
			btnSend.setClickable(true);
		}else{
			btnSend.setClickable(false);
		}
		if(mDeviceModel==1){
			findViewById(R.id.mTv1Layout).setVisibility(View.GONE);
			findViewById(R.id.mTv2Layout).setVisibility(View.GONE);
			findViewById(R.id.mTv3Layout).setVisibility(View.GONE);
			findViewById(R.id.mTv4Layout).setVisibility(View.GONE);
			findViewById(R.id.mTv5Layout).setVisibility(View.GONE);
		}



		getActionBar().setTitle(R.string.menu_back);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		findViewById(R.id.button1).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				onBackPressed();
			}
		});

		btnSend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendUserInfo(true);
			}
		});

		Intent gattServiceIntent = new Intent(this, OldALBluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

		DessRssi();

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
		sendUserInfo(false);
		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDess=false;
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
	boolean writeResult = false;
	private void displayData(String data) {
		if (data != null) {
			String[] strdata=data.split("-");
			Log.e("Notification", data);
			if(strdata.length>=10){
				if(data.equalsIgnoreCase("02-00-08-FF-A5-10-00-00-00-00-D5-")){
					sendUserInfo(false);
				}
				if (strdata[9].equals("AA")||strdata[9].equals("A0")){
					String tmpNum = strdata[5] + strdata[6];
					int weight = Integer.valueOf(tmpNum, 16);
					mDataField.setText(String.valueOf(weight));

					if(mDeviceModel==1){
						mCheckField.setChecked(true);
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
				}else if(strdata[9].equals("D0")){
					String tmpNum= strdata[5]+strdata[6];
					((TextView)findViewById(R.id.mTv5)).setText(String.valueOf(Integer.valueOf(tmpNum,16)));
				}else if(strdata[9].equals("C0")){
					String tmpNum= strdata[5]+strdata[6];
					((TextView)findViewById(R.id.mTv3)).setText(String.valueOf(Integer.valueOf(tmpNum,16)));

					tmpNum= strdata[8]+strdata[7];
					((TextView)findViewById(R.id.mTv4)).setText(String.valueOf(Integer.valueOf(tmpNum,16)));
				}else if(strdata[9].equals("B0")){
					String tmpNum= strdata[5]+strdata[6];
					((TextView)findViewById(R.id.mtv2)).setText(String.valueOf(Integer.valueOf(tmpNum,16)));

					 tmpNum= strdata[7]+strdata[8];
					((TextView)findViewById(R.id.mTv1)).setText(String.valueOf(Integer.valueOf(tmpNum,16)));

					if(mDeviceModel==2){
						mCheckField.setChecked(true);
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
		}


	}

	// Demonstrates how to iterate through the supported GATT Services/Characteristics.
	// In this sample, we populate the data structure that is bound to the ExpandableListView
	// on the UI.
	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null) return;

		for (BluetoothGattService gattService : gattServices) {
			String tmp=gattService.getUuid().toString();
			if (gattService.getUuid().toString().equals("0000feb3-0000-1000-8000-00805f9b34fb")) {
				List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
				for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
					if (characteristic.getUuid().toString().trim().equals("0000fed6-0000-1000-8000-00805f9b34fb")) {

						mBluetoothLeService.setCharacteristicNotification(characteristic,true);
						mNotifyCharacteristic = characteristic;
					}
					if (characteristic.getUuid().toString().trim().equals("0000fed5-0000-1000-8000-00805f9b34fb")) {
						mWriteCharacteristic=characteristic;
						writeResult = false;
					}
				}
			}
		}
		if(autoCheck){
			sendUserInfo(true);
		}
	}

	Handler mHandler;
	Runnable mRunnable;

	private void sendUserInfo(boolean sendAble){
		if(sendAble){
			if(mHandler == null){
				mHandler = new Handler();
			}
			if(mRunnable == null){
				mRunnable = new Runnable() {
					@Override
					public void run() {
						if(mWriteCharacteristic != null){
							byte[] sendBuffer = new byte[]{(byte) Integer.parseInt("02", 16),
									(byte) Integer.parseInt("00", 16),(byte) Integer.parseInt("08", 16),(byte) Integer.parseInt("A5", 16),(byte) Integer.parseInt("10", 16),(byte) Integer.parseInt("01", 16),
									(byte) Integer.parseInt("12", 16),(byte) Integer.parseInt("A7", 16),(byte) Integer.parseInt("02", 16),(byte) Integer.parseInt("8F", 16),(byte) Integer.parseInt("5B", 16)};

							mWriteCharacteristic.setValue(sendBuffer);
							writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
							Log.e(TAG,"-------writeResult:"+writeResult);
						}
						mHandler.postDelayed(this,1000);
					}
				};
			}

			mHandler.postDelayed(mRunnable,100);
		}else{
			if(mHandler != null && mRunnable != null){
				mHandler.removeCallbacks(mRunnable);
				mRunnable = null;
				mHandler = null;
			}

		}
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(OldALBluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(OldALBluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(OldALBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(OldALBluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(OldALBluetoothLeService.ACTION_DATA_RSSI);
		return intentFilter;
	}


	private void DessRssi() {
		new Thread() {
			public void run() {
				while (mDess) {

					if(mConnected) {
						mBluetoothLeService.getmBluetoothGatt().readRemoteRssi();
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}.start();
	}


}
