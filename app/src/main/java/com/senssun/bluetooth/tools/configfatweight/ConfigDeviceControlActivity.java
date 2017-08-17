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

package com.senssun.bluetooth.tools.configfatweight;

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
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.bleweight.BleWeightBluetoothLeService;
import com.senssun.bluetooth.tools.relative.Information;

import java.util.ArrayList;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class ConfigDeviceControlActivity extends Activity {
	private final static String TAG = ConfigDeviceControlActivity.class.getSimpleName();

	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

	private SharedPreferences mySharedPreferences;
	private SharedPreferences.Editor myEditor;

	private CheckedTextView mCheckField;
	private TextView mConnectionState;
	private TextView mDataField;
	private TextView mRssiField;
	private String mDeviceName;
	private String mDeviceAddress;
	private ConfigBluetoothLeService mBluetoothLeService;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
			new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private boolean mConnected = false;
	private BluetoothGattCharacteristic mNotifyCharacteristic;
	private BluetoothGattCharacteristic mWriteCharacteristic;
	private boolean mChceked=false;

	private EditText etMaxValue,etMinValue,etTemp,etCloseTime,userSize;
	private RadioButton rb1,rb2,rb3,rb4,rbW1,rbW2,rbW3,rbW4,rbP1,rbP2,rbS1,rbS2,rbS3;
	private RadioGroup rgUnit,rgWeight,rgPower,rgStartUnit;
	private Button btnEdit;
	private Button btnWrite;
	private boolean isEdit = false;



	public final static byte[] sendBuffer = new byte[]{(byte) Integer.parseInt("AB", 16),(byte) Integer.parseInt("AB", 16),
			(byte) Integer.parseInt("3B", 16),(byte) Integer.parseInt("C4", 16),
			(byte) Integer.parseInt("C8", 16),(byte) Integer.parseInt("1E", 16),
			(byte) Integer.parseInt("0A", 16),(byte) Integer.parseInt("07", 16),
			0,0,0,0,0,0,0,(byte) Integer.parseInt("09", 16)};

	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((ConfigBluetoothLeService.LocalBinder) service).getService();
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
			if (ConfigBluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				updateConnectionState(R.string.connected);
				invalidateOptionsMenu();
			} else if (ConfigBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				mBluetoothLeService.close();
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

			} else if (ConfigBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				displayGattServices(mBluetoothLeService.getSupportedGattServices());
			} else if (ConfigBluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayData(intent.getStringExtra(BleWeightBluetoothLeService.EXTRA_DATA));
			}else if (ConfigBluetoothLeService.ACTION_DATA_RSSI.equals(action)) {
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
		setContentView(R.layout.activity_config_device_control);
		mySharedPreferences = getSharedPreferences("sp",Activity.MODE_PRIVATE);
		myEditor = mySharedPreferences.edit();

		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

		// Sets up UI references.
		((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
		mConnectionState = (TextView) findViewById(R.id.connection_state);
		mDataField = (TextView) findViewById(R.id.data_value);
		mRssiField= (TextView) findViewById(R.id.rssi_value);
		mCheckField= (CheckedTextView) findViewById(R.id.check_value);

		etMaxValue = (EditText) findViewById(R.id.etMaxValue);
		etMinValue = (EditText) findViewById(R.id.etMinValue);
		etCloseTime = (EditText) findViewById(R.id.etCloseTime);
		etTemp = (EditText) findViewById(R.id.etTemp);
		userSize= (EditText) findViewById(R.id.userSize);
		btnWrite = (Button) findViewById(R.id.btnWrite);
		btnEdit = (Button) findViewById(R.id.btnEdit);
		rb1 = (RadioButton) findViewById(R.id.rb1);
		rb2 = (RadioButton) findViewById(R.id.rb2);
		rb3 = (RadioButton) findViewById(R.id.rb3);
		rb4 = (RadioButton) findViewById(R.id.rb4);
		rbW1= (RadioButton) findViewById(R.id.rbW1);
		rbW2= (RadioButton) findViewById(R.id.rbW2);
		rbW3= (RadioButton) findViewById(R.id.rbW3);
		rbW4= (RadioButton) findViewById(R.id.rbW4);
		rbP1= (RadioButton) findViewById(R.id.rbP1);
		rbP2= (RadioButton) findViewById(R.id.rbP2);
		rbS1= (RadioButton) findViewById(R.id.rbS1);
		rbS2= (RadioButton) findViewById(R.id.rbS2);
		rbS3= (RadioButton) findViewById(R.id.rbS3);


		rgUnit = (RadioGroup) findViewById(R.id.rgUnit);
		rgWeight = (RadioGroup) findViewById(R.id.rgWeight);
		rgPower= (RadioGroup) findViewById(R.id.rgPower);
		rgStartUnit= (RadioGroup) findViewById(R.id.rgStartUnit);

		initWeight();
//		rgUnit.check(rb4.getId());
		changeStatus(false);
		btnEdit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isEdit) {
					isEdit = false;
					saveValue();
				} else {
					isEdit = true;
				}
				changeStatus(isEdit);
			}
		});

		btnWrite.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				writeToDevice();
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

		Intent gattServiceIntent = new Intent(this, ConfigBluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


	}

	private void initWeight(){
		etMaxValue.setText(mySharedPreferences.getString(Information.DB.MaxWeight,"153"));
		etMinValue.setText(mySharedPreferences.getString(Information.DB.MinWeight,"2.0"));
		etCloseTime.setText(mySharedPreferences.getString(Information.DB.CloseTime,"30"));
		etTemp.setText(mySharedPreferences.getString(Information.DB.Temperature,"10"));
		userSize.setText(mySharedPreferences.getString(Information.DB.UserSize,"12"));
		switch (mySharedPreferences.getString(Information.DB.Unit,"7")){
			case "0":
				rgUnit.check(rb1.getId());
				break;
			case "1":
				rgUnit.check(rb2.getId());
				break;
			case "3":
				rgUnit.check(rb3.getId());
				break;
			case "7":
				rgUnit.check(rb4.getId());
				break;
			default:
				rgUnit.check(rb4.getId());
				break;
		}

		switch (mySharedPreferences.getString(Information.DB.WeightMode,"00")){
			case "00":
				rgWeight.check(rbW1.getId());
				break;
			case "01":
				rgWeight.check(rbW2.getId());
				break;
			case "02":
				rgWeight.check(rbW3.getId());
				break;
			case "03":
				rgWeight.check(rbW4.getId());
				break;
			default:
				rgWeight.check(rbW4.getId());
				break;
		}

		switch (mySharedPreferences.getString(Information.DB.PowerMode,"00")){
			case "00":
				rgPower.check(rbP1.getId());
				break;
			case "01":
				rgPower.check(rbP2.getId());
				break;
			default:
				rgPower.check(rbP1.getId());
				break;
		}

		switch (mySharedPreferences.getString(Information.DB.StartUnit,"0")){
			case "1":
				rgStartUnit.check(rbS1.getId());
				break;
			case "2":
				rgStartUnit.check(rbS2.getId());
				break;
			case "4":
				rgStartUnit.check(rbS3.getId());
				break;
			default:
				rgStartUnit.check(rbS1.getId());
				break;
		}
		saveValue();
	};


	private void displayData(String data) {
		if (data != null) {
			String[] strdata=data.split("-");
			Log.e("Notification", data);

			if((strdata.length>=8&&strdata[0].equals("FF")&&strdata[1].equals("A5")&&strdata[2].equals("AB") &&strdata[7].equals("AB"))
					||
					(strdata.length>=15&&(strdata[0]+strdata[1]+strdata[2]+strdata[3]+strdata[4]+strdata[5]+strdata[6]+strdata[7]+strdata[8]+strdata[9]+strdata[10]+strdata[11]+strdata[12]+strdata[13]+strdata[14]).equals("100000C50E8000AB000000003C"))){
				mChceked = true;
				mCheckField.setChecked(true);
				if(mySharedPreferences.getBoolean(Information.DB.AutoCheck,false)){
					Intent intent = new Intent();
					Bundle bundle = new Bundle();
					bundle.putString("BleSuc", mDeviceAddress);
					intent.putExtras(bundle);
					setResult(RESULT_OK, intent);
					finish();
				}

				mDataField.setText(mySharedPreferences.getString(Information.DB.MaxWeight, "153.00"));
			}


//			if(strdata.length>=8){
//				if (strdata[6].equals("AA")||strdata[6].equals("A0")){
//					String tmpNum = strdata[2] + strdata[3];
//					int weight = Integer.valueOf(tmpNum, 16);
//					if(weight>100)mWeight=true;
//					mDataField.setText(String.valueOf(weight));
//
//
//
//				}
//				if(strdata[2].equals("5A")){
//					m5ASendBuffer=true;
//				}
//				if (m5ASendBuffer&&mWeight){
//					mCheckField.setChecked(true);
//					SharedPreferences mySharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE);
//					if(mySharedPreferences.getBoolean(Information.DB.AutoCheck, true)) {
//						Intent intent = new Intent();
//						Bundle bundle = new Bundle();
//						bundle.putString("BleSuc", mDeviceAddress);
//						intent.putExtras(bundle);
//						setResult(RESULT_OK, intent);
//						finish();
//					}
//				}
//
//
//			}
		}


	}

	private void changeStatus(boolean enable){
		if(enable){
//			enableThread = false;
			etMaxValue.setEnabled(true);
			etMinValue.setEnabled(true);
			etTemp.setEnabled(true);
			userSize.setEnabled(true);
			etCloseTime.setEnabled(true);
			rgUnit.setEnabled(true);
			rgStartUnit.setEnabled(true);
			rgWeight.setEnabled(true);
			rgPower.setEnabled(true);
			btnEdit.setText("保存");
			btnWrite.setEnabled(false);
			rb1.setEnabled(true);
			rb2.setEnabled(true);
			rb3.setEnabled(true);
			rb4.setEnabled(true);
		}else{
//			enableThread = true;
			etMaxValue.setEnabled(false);
			etMinValue.setEnabled(false);
			etTemp.setEnabled(false);
			userSize.setEnabled(false);
			etCloseTime.setEnabled(false);
			rgUnit.setEnabled(false);
			rgStartUnit.setEnabled(false);
			rgWeight.setEnabled(false);
			rgPower.setEnabled(false);
			btnEdit.setText("编辑");
			btnWrite.setEnabled(true);
			rb1.setEnabled(false);
			rb2.setEnabled(false);
			rb3.setEnabled(false);
			rb4.setEnabled(false);
		}
	}

	private void saveValue(){
		String data = etMaxValue.getText().toString();
		myEditor.putString(Information.DB.MaxWeight,data);
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
		myEditor.putString(Information.DB.MinWeight,data);
//		if(data.contains(".")){
//			data = data.replace(".", "");
//		}else{
//			data = data + "00";
//		}
		data=String.valueOf((int)(Float.valueOf(data)*100));

		sendBuffer[4] = (byte)Integer.parseInt(Integer.toHexString(Integer.valueOf(data)),16);
		data = etCloseTime.getText().toString();
		myEditor.putString(Information.DB.CloseTime,data);
		if(Integer.valueOf(data) < 15){
			Toast.makeText(this,"自动关闭时间必须大于15秒！",Toast.LENGTH_SHORT).show();
			isEdit = true;
			return;
		}
		sendBuffer[5] = (byte)Integer.parseInt(Integer.toHexString(Integer.valueOf(data)),16);
		data = etTemp.getText().toString();
		myEditor.putString(Information.DB.Temperature,data);
		if(data.contains("-")){
			data = data.replace("-","");
			sendBuffer[6] = (byte)Integer.parseInt(Integer.toHexString(128+Integer.valueOf(data)),16);
		}else{
			sendBuffer[6] = (byte)Integer.parseInt(Integer.toHexString(Integer.valueOf(data)),16);
		}
		data = "00";
		String qian="0";
		String hou="0";
		switch (rgStartUnit.getCheckedRadioButtonId()){
			case R.id.rbS1:
				qian = "1";
				break;
			case R.id.rbS2:
				qian = "2";
				break;
			case R.id.rbS3:
				qian = "4";
				break;
		}
		myEditor.putString(Information.DB.StartUnit,qian);
		switch (rgUnit.getCheckedRadioButtonId()){
			case R.id.rb1:
				hou = "0";
				break;
			case R.id.rb2:
				hou = "1";
				break;
			case R.id.rb3:
				hou = "3";
				break;
			case R.id.rb4:
				hou = "7";
				break;
		}
		myEditor.putString(Information.DB.Unit,hou);
		data=qian+hou;

		sendBuffer[7] = (byte)Integer.parseInt(data,16);

		data = "00";
		switch (rgWeight.getCheckedRadioButtonId()){
			case R.id.rbW1:
				data = "00";
				break;
			case R.id.rbW2:
				data = "01";
				break;
			case R.id.rbW3:
				data = "02";
				break;
			case R.id.rbW4:
				data = "03";
				break;
		}
		myEditor.putString(Information.DB.WeightMode,data);
		sendBuffer[8] = (byte)Integer.parseInt(Integer.toHexString(Integer.valueOf(data)),16);

		data = userSize.getText().toString();
		myEditor.putString(Information.DB.UserSize,data);
		sendBuffer[9] = (byte)Integer.parseInt(Integer.toHexString(Integer.valueOf(data)),16);

		data = "00";
		switch (rgPower.getCheckedRadioButtonId()){
			case R.id.rbP1:
				data = "00";
				break;
			case R.id.rbP2:
				data = "01";
				break;
		}
		myEditor.putString(Information.DB.PowerMode,data);
		sendBuffer[10] = (byte)Integer.parseInt(Integer.toHexString(Integer.valueOf(data)),16);

		String strSum=null;
		if(("SENSSUN CLOUD").equals(mDeviceName)){
			strSum = Integer.toHexString(((sendBuffer[2] & 0xff)
					+ (sendBuffer[3] & 0xff) + (sendBuffer[4] & 0xff)
					+ (sendBuffer[5] & 0xff) + (sendBuffer[6] & 0xff)
					+ (sendBuffer[7] & 0xff) + (sendBuffer[8] & 0xff)
					+ (sendBuffer[9] & 0xff) + (sendBuffer[10] & 0xff)
			));
		}else{
			strSum = Integer.toHexString(~((sendBuffer[2] & 0xff)
					+ (sendBuffer[3] & 0xff) + (sendBuffer[4] & 0xff)
					+ (sendBuffer[5] & 0xff) + (sendBuffer[6] & 0xff)
					+ (sendBuffer[7] & 0xff) + (sendBuffer[8] & 0xff)
					+ (sendBuffer[9] & 0xff) + (sendBuffer[10] & 0xff)
			));
		}
		if(strSum.length() > 2){
			strSum = strSum.substring(strSum.length()-2,strSum.length());
		}else{
			strSum = "0"+strSum;
		}
		sendBuffer[15] = (byte)Integer.parseInt(strSum,16);

		myEditor.commit();
		mCheckField.setChecked(false);
	}

	private void writeToDevice(){
//		//写入
//		if(!isThreadRunning){
//			new Thread(new TimeThread()).start();
//		}

		if(mWriteCharacteristic != null){
			mWriteCharacteristic.setValue(sendBuffer);
			StringBuilder stringBuilder = new StringBuilder(sendBuffer.length);
			for(byte byteChar : sendBuffer){
					String ms=String.format("%02X ", byteChar).trim();
					stringBuilder.append(ms+"-");
				}
				Log.d(TAG, "发送2："+stringBuilder.toString());

			boolean writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
			Log.e(TAG,"----------writeResult:"+writeResult);
			if(writeResult){
				mDataField.setText("- -");
			}
//			if(writeResult){
//				mChceked = true;
//				mCheckField.setChecked(true);
//				if(mySharedPreferences.getBoolean(Information.DB.AutoCheck,false)){
//					Intent intent = new Intent();
//					Bundle bundle = new Bundle();
//					bundle.putString("BleSuc", mDeviceAddress);
//					intent.putExtras(bundle);
//					setResult(RESULT_OK, intent);
//					finish();
//				}
//
//			}else{
//				mChceked = false;
//				mCheckField.setChecked(false);
//			}
		}
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
//		new Thread(new TimeThread()).start();
	}
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConfigBluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(ConfigBluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(ConfigBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(ConfigBluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(ConfigBluetoothLeService.ACTION_DATA_RSSI);
		return intentFilter;
	}

//	boolean writeResult = false;
//	boolean isThreadRunning = false;
//	boolean enableThread = true;
//	private class TimeThread implements Runnable{
//
//		private TimeThread(){}
//		@Override
//		public void run() {
//			while(mConnected && enableThread){
//				isThreadRunning = true;
//				try{
//					Thread.sleep(250);
//					if(mWriteCharacteristic != null){
//						mWriteCharacteristic.setValue(sendBuffer);
//						writeResult = mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
//						runOnUiThread(new Runnable() {
//							@Override
//							public void run() {
//								if(writeResult){
//									mChceked = true;
//									mCheckField.setChecked(true);
//								}else{
//									mChceked = false;
//									mCheckField.setChecked(false);
//								}
//							}
//						});
//					}
//				}catch (Exception e){
//					Log.e(TAG,e.toString());
//				}
//
//			}
//			if(!mConnected || !enableThread) {
//				runOnUiThread(new Runnable() {
//					@Override
//					public void run() {
//						mChceked = false;
//						mCheckField.setChecked(false);
//					}
//				});
//				isThreadRunning = false;
//			}
//		}
//	}


}
