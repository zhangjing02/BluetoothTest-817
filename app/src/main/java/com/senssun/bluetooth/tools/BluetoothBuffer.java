package com.senssun.bluetooth.tools;


public class BluetoothBuffer {
	public final static String ACTION_GATT_CONNECTED ="com.example.bluetooth.le.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED ="com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED ="com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE ="com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
	public final static String ACTION_DATA_RSSI ="com.example.bluetooth.le.ACTION_DATA_RSSI";
	public final static String EXTRA_DATA ="com.example.bluetooth.le.EXTRA_DATA";

	//测试闹铃
	public final static byte[] SendAlarmBuffer = new byte[]{ (byte)0xa5,(byte)0x32,0,0,0,0,(byte)0x55,(byte)0x87};//
	//发送个人信息 默认165cm 25year female
	public final static byte[] SendBodyBuffer = new byte[]{ (byte)0xa5,(byte)0x10,(byte)0x01,(byte)0x19,(byte)0xa5,0,0,(byte)0x74};//
	//清除历史
	public final static byte[] ClearDataBuffer=new byte[]{(byte)0xA5,0x50,0,0,0,0,0,0,0};

}
