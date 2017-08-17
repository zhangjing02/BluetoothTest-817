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

package com.senssun.bluetooth.tools.aliconfigweight;

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
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

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
public class ALConfigDeviceControlActivity extends Activity {
	private final static String TAG = ALConfigDeviceControlActivity.class.getSimpleName();

	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

	private CheckedTextView mCheckField;
	private TextView mConnectionState;
	private TextView mDataField;
	private TextView mRssiField;
	private String mDeviceName;
	private String mDeviceAddress;
	private ALConfigBluetoothLeService mBluetoothLeService;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
			new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private boolean mConnected = false;
	private BluetoothGattCharacteristic mNotifyCharacteristic;
	private BluetoothGattCharacteristic mWriteCharacteristic;
	private boolean mChceked=false;

	private EditText etMaxValue,etMinValue,etTemp,etCloseTime;
	private RadioButton rb1,rb2,rb3,rb4;
	private RadioGroup rgUnit;
	private Button btnEdit;
	private boolean isEdit = false;
	private boolean writeResult = false;

//	public final static byte[] sendBuffer = new byte[]{(byte) Integer.parseInt("AA", 16),
//		(byte) Integer.parseInt("55", 16),(byte) Integer.parseInt("08", 16),0,(byte) Integer.parseInt("5A", 16)
//			,(byte) Integer.parseInt("5A", 16),(byte) Integer.parseInt("5A", 16), 0};//


	public final static byte[] sendBuffer = new byte[]{(byte) Integer.parseInt("AB", 16),(byte) Integer.parseInt("AB", 16),
			(byte) Integer.parseInt("3B", 16),(byte) Integer.parseInt("C4", 16),
			(byte) Integer.parseInt("C8", 16),(byte) Integer.parseInt("1E", 16),
			(byte) Integer.parseInt("0A", 16),(byte) Integer.parseInt("07", 16),
			0,0,0,0,0,0,0,(byte) Integer.parseInt("09", 16)};

	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((ALConfigBluetoothLeService.LocalBinder) service).getService();
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
			if (ALConfigBluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				updateConnectionState(R.string.connected);
				invalidateOptionsMenu();
			} else if (ALConfigBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				mConnected = false;
				mCheckField.setChecked(false);
				mChceked = false;
				updateConnectionState(R.string.disconnected);
				invalidateOptionsMenu();
				clearUI();
				SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
				if(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
					onBackPressed();
				}

			} else if (ALConfigBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				displayGattServices(mBluetoothLeService.getSupportedGattServices());
			} else if (ALConfigBluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
			}else if (ALConfigBluetoothLeService.ACTION_DATA_RSSI.equals(action)) {
				displayRssi(intent.getStringExtra("Rssi"));
			}
		}
	};


	private void clearUI() {
//		mDataField.setText(R.string.no_data);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ali_config_device_control);

		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

		// Sets up UI references.
		((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
		mConnectionState = (TextView) findViewById(R.id.connection_state);
//		mDataField = (TextView) findViewById(R.id.data_value);
		mRssiField= (TextView) findViewById(R.id.rssi_value);
		mCheckField= (CheckedTextView) findViewById(R.id.check_value);

		etMaxValue = (EditText) findViewById(R.id.etMaxValue);
		etMinValue = (EditText) findViewById(R.id.etMinValue);
		etCloseTime = (EditText) findViewById(R.id.etCloseTime);
		etTemp = (EditText) findViewById(R.id.etTemp);
		rgUnit = (RadioGroup) findViewById(R.id.rgUnit);
		btnEdit = (Button) findViewById(R.id.btnEdit);
		rb1 = (RadioButton) findViewById(R.id.rb1);
		rb2 = (RadioButton) findViewById(R.id.rb2);
		rb3 = (RadioButton) findViewById(R.id.rb3);
		rb4 = (RadioButton) findViewById(R.id.rb4);
		rgUnit.check(rb4.getId());
		changeStatus(false);
		btnEdit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isEdit){
					isEdit = false;
					changeValue();
				}else{
					isEdit = true;
				}
				changeStatus(isEdit);
			}
		});


		getActionBar().setTitle(R.string.menu_back);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		findViewById(R.id.button1).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				onBackPressed();
			}
		});

		Intent gattServiceIntent = new Intent(this, ALConfigBluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


	}

	private void changeStatus(boolean enable){
		if(enable){
			etMaxValue.setEnabled(true);
			etMinValue.setEnabled(true);
			etTemp.setEnabled(true);
			etCloseTime.setEnabled(true);
			rgUnit.setEnabled(true);
			btnEdit.setText("写入");
			rb1.setEnabled(true);
			rb2.setEnabled(true);
			rb3.setEnabled(true);
			rb4.setEnabled(true);
		}else{
			etMaxValue.setEnabled(false);
			etMinValue.setEnabled(false);
			etTemp.setEnabled(false);
			etCloseTime.setEnabled(false);
			rgUnit.setEnabled(false);
			btnEdit.setText("编辑");
			rb1.setEnabled(false);
			rb2.setEnabled(false);
			rb3.setEnabled(false);
			rb4.setEnabled(false);
		}
	}

	private void changeValue(){
		String data = etMaxValue.getText().toString();
		if(data.contains(".")){
			data = data.replace(".","");
		}else{
			data = data + "00";
		}
		String strHex = Integer.toHexString(Integer.valueOf(data));
		String str1 = "00",str2 = "00";
		switch (strHex.length()){
			case 1:
				str1 = "00";
				str2 = "0"+strHex;
				break;
			case 2:
				str1 = "00";
				str2 = strHex;
				break;
			case 3:
				str1 = "0"+strHex.substring(0,1);
				str2 = strHex.substring(1,3);
				break;
			case 4:
				str1 = strHex.substring(0,2);
				str2 = strHex.substring(2,4);
				break;
		}
		sendBuffer[2] = (byte)Integer.parseInt(str1,16);
		sendBuffer[3] = (byte)Integer.parseInt(str2,16);
		data = etMinValue.getText().toString();
		if(data.contains(".")){
			data = data.replace(".", "");
		}else{
			data = data + "00";
		}
		sendBuffer[4] = (byte)Integer.parseInt(Integer.toHexString(Integer.valueOf(data)),16);
		data = etCloseTime.getText().toString();
		if(Integer.valueOf(data) < 15){
			Toast.makeText(this,"自动关闭时间必须大于15秒！",Toast.LENGTH_SHORT).show();
			isEdit = true;
			return;
		}
		sendBuffer[5] = (byte)Integer.parseInt(Integer.toHexString(Integer.valueOf(data)),16);
		data = etTemp.getText().toString();
		if(data.contains("-")){
			data = data.replace("-","");
			sendBuffer[6] = (byte)Integer.parseInt(Integer.toHexString(128+Integer.valueOf(data)),16);
		}else{
			sendBuffer[6] = (byte)Integer.parseInt(Integer.toHexString(Integer.valueOf(data)),16);
		}
		data = "00";
		switch (rgUnit.getCheckedRadioButtonId()){
			case R.id.rb1:
				data = "00";
				break;
			case R.id.rb2:
				data = "01";
				break;
			case R.id.rb3:
				data = "03";
				break;
			case R.id.rb4:
				data = "07";
				break;
		}
		sendBuffer[7] = (byte)Integer.parseInt(Integer.toHexString(Integer.valueOf(data)),16);
		String strSum = Integer.toHexString(~((sendBuffer[2] & 0xff)
				+ (sendBuffer[3] & 0xff) + (sendBuffer[4] & 0xff)
				+ (sendBuffer[5] & 0xff) + (sendBuffer[6] & 0xff) + (sendBuffer[7] & 0xff)));
		if(strSum.length() > 2){
			strSum = strSum.substring(strSum.length()-2,strSum.length());
		}else{
			strSum = "0"+strSum;
		}
		sendBuffer[15] = (byte)Integer.parseInt(strSum,16);
		mHandler = new Handler();
		mRunnable = new Runnable() {
			@Override
			public void run() {
				if(mWriteCharacteristic != null){
					mWriteCharacteristic.setValue(sendBuffer);
					writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if(writeResult){
								mChceked = true;
								mCheckField.setChecked(true);
							}else{
								mChceked = false;
								mCheckField.setChecked(false);
							}
						}
					});
				}
				if(!writeResult) {
					mHandler.postDelayed(this, 500);
				}
			}
		};
		mHandler.postDelayed(mRunnable,500);
	}

	private Handler mHandler;
	private Runnable mRunnable;

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


	// Demonstrates how to iterate through the supported GATT Services/Characteristics.
	// In this sample, we populate the data structure that is bound to the ExpandableListView
	// on the UI.
	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null) return;
		for (BluetoothGattService gattService : gattServices) {
			if (gattService.getUuid().toString().equals("0000feb3-0000-1000-8000-00805f9b34fb")) {
				List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
				for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
					if (characteristic.getUuid().toString().trim().equals("0000fed6-0000-1000-8000-00805f9b34fb")){
						mBluetoothLeService.setCharacteristicNotification(characteristic,true);
						mNotifyCharacteristic = characteristic;
					}
					if (characteristic.getUuid().toString().trim().equals("0000fed5-0000-1000-8000-00805f9b34fb")){
						mWriteCharacteristic=characteristic;
					}
				}
			}

		}

	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ALConfigBluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(ALConfigBluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(ALConfigBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(ALConfigBluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(ALConfigBluetoothLeService.ACTION_DATA_RSSI);
		return intentFilter;
	}










}
