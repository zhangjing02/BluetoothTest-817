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

package com.senssun.bluetooth.tools.xinhai.ali;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
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
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.entity.BluetoothDeviceObject;
import com.senssun.bluetooth.tools.relative.Information;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class ALDeviceControlActivity extends Activity {
	private final static String TAG = ALDeviceControlActivity.class.getSimpleName();

	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	public static final String EXTRAS_DEVICE_MODEL = "DEVICE_MODEL";

	public static final int TEST_TIME = 20;//进行20次测试
	public static final int TEST_WRITE_TIME = 20;//每次测试写入20次

	public static final int REPELY_TIME = 1000;//重新写入时间1s


	private TextView tvRightCount;
	private TextView tvWriteCount;
	private TextView tvReplyCount;
	private TextView tvReplyWrongCount;
	private TextView tvReplySuccessCount;



	private int connectingCount = 0;
	private int connectedCount = 0;
	private int connectErrorCount = 0;
	private int disConnectErrorCount = 0;

	private int writeCount = 0;
	private int replyCount = 0;
	private int rightReplyCount = 0;
	private int replyWrongCount = 0;
	private int replySuccessCount = 0;

	private boolean isTestEnable = true;

	private static final long SCAN_PERIOD = 1000;


	private String mDeviceName;
	private String mDeviceAddress;
	private int mDeviceModel;
	private com.senssun.bluetooth.tools.xinhai.ali.ALBluetoothLeService mBluetoothLeService;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
			new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private boolean mConnected = false;
	private boolean mDess=true;
	private BluetoothGattCharacteristic mNotifyCharacteristic;
	private BluetoothGattCharacteristic mWriteCharacteristic;

	private boolean mScanning;
	private Handler mHandler;

	private BluetoothAdapter mBluetoothAdapter;


	public final static byte[] sendBuffer = new byte[]{(byte) Integer.parseInt("A5", 16),
		(byte) Integer.parseInt("5A", 16),0, 0, 0, 0, 0,  (byte) Integer.parseInt("5A", 16)};//


	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((com.senssun.bluetooth.tools.xinhai.ali.ALBluetoothLeService.LocalBinder) service).getService();
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
			if (ALBluetoothLeService.ACTION_GATT_CONNECTING.equals(action)){
				if(connectingCount == connectedCount)
					connectingCount++;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						((TextView)findViewById(R.id.tvConCount)).setText(String.valueOf(connectingCount));
					}
				});
			}else if (ALBluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				scanLeDevice(false);
				invalidateOptionsMenu();
				if(connectedCount < connectingCount)
					connectedCount++;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						((TextView) findViewById(R.id.tvConSucCount)).setText(String.valueOf(connectedCount));
					}
				});
			} else if (ALBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				mConnected = false;
				invalidateOptionsMenu();
//				if(connectingCount > TEST_TIME){
//					((TextView)findViewById(R.id.tvTestFinish)).setVisibility(View.VISIBLE);
//				}else{
					scanLeDevice(true);
//				}
			} else if (ALBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

				displayGattServices(mBluetoothLeService.getSupportedGattServices());
			} else if (ALBluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayData(intent.getStringExtra(ALBluetoothLeService.EXTRA_DATA));
			}
		}
	};



	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_xinhai_ali_device_control);
		mHandler = new Handler();
		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
		mDeviceModel= intent.getIntExtra(EXTRAS_DEVICE_MODEL, 0);

		// Sets up UI references.
		((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);

		tvWriteCount = (TextView) findViewById(R.id.tvWriteCount);
		tvReplyCount = (TextView) findViewById(R.id.tvReplyCount);
		tvRightCount = (TextView) findViewById(R.id.tvRightCount);
		tvReplyWrongCount = (TextView) findViewById(R.id.tvReplyWrongCount);
		tvReplySuccessCount= (TextView) findViewById(R.id.tvReplySuccessCount);

		getActionBar().setTitle(R.string.menu_back);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		findViewById(R.id.button1).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				onBackPressed();
			}
		});
		final BluetoothManager bluetoothManager =
				(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		Intent gattServiceIntent = new Intent(this, ALBluetoothLeService.class);
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






	private void displayData(String data) {
		Log.e("Notification", data);
		if (data != null) {
			String[] strdata=data.split("-");
			if(strdata.length>=10){
				if(strdata[4].equals("A5")&&strdata[5].equals("5A")&&strdata[10].equals("5A")){
					rightReplyCount++;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							tvRightCount.setText(String.valueOf(rightReplyCount));
						}
					});

				}else{
					replyCount++;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							tvReplyCount.setText(String.valueOf(replyCount));
						}
					});
				}


			}else{
				if(strdata[1].equals("AA")&&strdata[2].equals("5A")&&strdata[7].equals("5A")){
					rightReplyCount++;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							tvRightCount.setText(String.valueOf(rightReplyCount));
						}
					});

				}else{
					replyCount++;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							tvReplyCount.setText(String.valueOf(replyCount));
						}
					});
				}

				if(((byte)Integer.parseInt(strdata[2], 16)+(byte)Integer.parseInt(strdata[3], 16)+(byte)Integer.parseInt(strdata[4], 16)
						+(byte)Integer.parseInt(strdata[5], 16)+(byte)Integer.parseInt(strdata[6], 16)
				)!=(byte)Integer.parseInt(strdata[7], 16)) {
					replyWrongCount++;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							tvReplyWrongCount.setText(String.valueOf(replyWrongCount));
						}
					});
				}else{
					replySuccessCount++;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							tvReplySuccessCount.setText(String.valueOf(replySuccessCount));
						}
					});


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
						mBluetoothLeService.setCharacteristicINDICATION(characteristic,true);
						mNotifyCharacteristic = characteristic;
					}
					if (characteristic.getUuid().toString().trim().equals("0000fed5-0000-1000-8000-00805f9b34fb")) {
						mWriteCharacteristic=characteristic;
					}
				}
			}

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
		mWriteCharacteristic.setValue(sendBuffer);

		new Thread(new Runnable() {
			@Override
			public void run() {

				while(isTestEnable && writeCount < TEST_WRITE_TIME * connectedCount){
					try {
						Thread.sleep(300);
						boolean writeResult = false;
						writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
						if (writeResult) {
							writeCount++;
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									tvWriteCount.setText(String.valueOf(writeCount).toString());
								}
							});
						}
					} catch (Exception e) {
						Log.e(TAG, e.toString());
					}

				}
				mBluetoothLeService.close();

			}
		}).start();
	}



	private void scanLeDevice(final boolean enable) {
//		if(mBluetoothLeService != null) {
//			final BluetoothAdapter mBluetoothAdapter = mBluetoothLeService.getBluetoothAdapter();
			if (enable) {
				// Stops scanning after a pre-defined scan period.
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (mScanning) {
							mScanning = false;
							mBluetoothAdapter.stopLeScan(mLeScanCallback);
							scanLeDevice(true);
						} else {
							mScanning = false;
							mBluetoothAdapter.stopLeScan(mLeScanCallback);
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
//		}
	}

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback =
			new BluetoothAdapter.LeScanCallback() {
				@Override
				public void onLeScan(final BluetoothDevice device, final int rssi,final byte[] scanRecord) {
					if(device.getAddress().equalsIgnoreCase(mDeviceAddress)){
						mBluetoothLeService.connect(mDeviceAddress);
					}
				}
			};

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ALBluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(ALBluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(ALBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(ALBluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(ALBluetoothLeService.ACTION_GATT_CONNECTING);
		intentFilter.addAction(ALBluetoothLeService.ACTION_DATA_RSSI);
		return intentFilter;
	}




}
