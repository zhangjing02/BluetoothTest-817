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

package com.senssun.bluetooth.tools.xinhai.fat;

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
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckedTextView;
import android.widget.TextView;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.relative.Information;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class FatDeviceControlActivity extends Activity {
	private final static String TAG = FatDeviceControlActivity.class.getSimpleName();

	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

	public static final int TEST_TIME = 20;//进行20次测试

	private CheckedTextView mCheckField;
	private TextView mDataField;
	private TextView mRssiField;
	private TextView tvLog;
	private String mDeviceName;
	private String mDeviceAddress;
	private FatBluetoothLeService mBluetoothLeService;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
			new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private boolean mConnected = false;
	private BluetoothGattCharacteristic mNotifyCharacteristic;
	private BluetoothGattCharacteristic mWriteCharacteristic;
	private boolean mChceked=false;

	private String strLog = "";
	private int testTime = 1;
	private List<Boolean> testResultList;

//	public final static byte[] sendBuffer = new byte[]{(byte) Integer.parseInt("A5", 16),
//			(byte) Integer.parseInt("10", 16), (byte) Integer.parseInt("01", 16),
//			(byte) Integer.parseInt("12", 16), (byte) Integer.parseInt("A7", 16),
//			(byte) Integer.parseInt("2", 16), (byte) Integer.parseInt("8F", 16),
//			(byte) Integer.parseInt("5B", 16)};//

	//同步时间命令
	public final static byte[] sendBuffer = new byte[]{(byte) Integer.parseInt("A5", 16),
			(byte) Integer.parseInt("30", 16), 0, 0, 0, 0, 0, 0};//


	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((FatBluetoothLeService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up initialization.
			strLog += "<B>测试"+testTime+"</B><br/>";
			updateLog();
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
			if(FatBluetoothLeService.ACTION_GATT_CONNECTING.equals(action)){
				strLog += "---正在连接...<br/>";
				updateLog();
			}
			if (FatBluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				invalidateOptionsMenu();
				strLog += "---连接成功<br/>";
				updateLog();
			} else if (FatBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				mConnected = false;
				invalidateOptionsMenu();
//				clearUI();
				SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
				if(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
					onBackPressed();
				}
				strLog += "---连接已断开<br/><br/>";
				updateLog();
				if(testResultList.size()<(testTime+1)){
					testResultList.add(false);
				}
				if(testTime <= TEST_TIME){
					testTime++;
					strLog += "<B>测试"+testTime+"</B><br/>";
					updateLog();
					mBluetoothLeService.connect(mDeviceAddress);
				}else{
					strLog += "<B>测试完成</B><br/>";
					updateLog();
					testFinish();
				}
			} else if (FatBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				displayGattServices(mBluetoothLeService.getSupportedGattServices());
			} else if (FatBluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayData(intent.getStringExtra(FatBluetoothLeService.EXTRA_DATA));
			}
		}
	};


//	private void clearUI() {
//		mDataField.setText(R.string.no_data);
//	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_xinhai_fat_device_control);

		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

		// Sets up UI references.
		((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
		mDataField = (TextView) findViewById(R.id.data_value);
		mRssiField= (TextView) findViewById(R.id.rssi_value);
		mCheckField= (CheckedTextView) findViewById(R.id.check_value);

		tvLog = (TextView) findViewById(R.id.tvLog);
		tvLog.setMovementMethod(ScrollingMovementMethod.getInstance());
		getActionBar().setTitle(R.string.menu_back);
		getActionBar().setDisplayHomeAsUpEnabled(true);

//		findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				// TODO Auto-generated method stub
//				onBackPressed();
//			}
//		});

		initSendBuffer();
		testResultList = new ArrayList<>();

		Intent gattServiceIntent = new Intent(this, FatBluetoothLeService.class);
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

	private void initSendBuffer(){
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int day = calendar.get(Calendar.DAY_OF_YEAR);
		int weekDay = calendar.get(Calendar.DAY_OF_WEEK);
		weekDay--;

		String code;
		code = Integer.toHexString((year%100));
		sendBuffer[2] = (byte)Integer.parseInt(code,16);
		code = Integer.toHexString((day/100));
		sendBuffer[3] = (byte)Integer.parseInt(code,16);
		code = Integer.toHexString((day%100));
		sendBuffer[4] = (byte)Integer.parseInt(code,16);
		code = Integer.toHexString((weekDay));
		sendBuffer[5] = (byte)Integer.parseInt(code,16);

		String strSum = Integer.toHexString((sendBuffer[1] & 0xff)
				+ (sendBuffer[2] & 0xff) + (sendBuffer[3] & 0xff)
				+ (sendBuffer[4] & 0xff) + (sendBuffer[5] & 0xff) + (sendBuffer[6] & 0xff));
		if(strSum.length() >= 2){
			strSum = strSum.substring(strSum.length()-2,strSum.length());
		}else{
			strSum = "0"+strSum;
		}
		Log.e(TAG,"strSum:"+strSum);
		sendBuffer[7] = (byte)Integer.parseInt(strSum, 16);
	}

	private void testFinish(){
		if(testResultList.size() < TEST_TIME){
			mCheckField.setChecked(false);
		}else{
			for(int i=0;i<testResultList.size();i++){
				if(!testResultList.get(i)){
					mCheckField.setChecked(false);
					return;
				}
			}
			mCheckField.setChecked(true);
		}
	}

	private void updateLog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tvLog.setText(Html.fromHtml(strLog));
			}
		});
	}


	private void displayData(String data) {
		if (data != null) {
			String[] strdata=data.split("-");
			Log.e("Notification", data);
			if(strdata.length>=10){
				if(strdata[0].equals("FF")&&strdata[1].equals("A5")&&strdata[2].equals("30")){
					if(strdata[6].equals("00")){
						strLog += "---写入成功<br/>";
						testResultList.add(true);
					}else{
						strLog += "---写入失败<br/>";
						testResultList.add(false);
					}
					updateLog();
					mBluetoothLeService.disconnect();
				}
			}
		}

//		mBluetoothLeService.getmBluetoothGatt().readRemoteRssi();
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

		boolean writeResult = false;
		mWriteCharacteristic.setValue(sendBuffer);
		while(!writeResult){
			writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
			Log.e(TAG,"writeResult:"+writeResult);
		}
		strLog += "---正在写入数据...<br/>";
		updateLog();

	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(FatBluetoothLeService.ACTION_GATT_CONNECTING);
		intentFilter.addAction(FatBluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(FatBluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(FatBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(FatBluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(FatBluetoothLeService.ACTION_DATA_RSSI);
		return intentFilter;
	}


}
