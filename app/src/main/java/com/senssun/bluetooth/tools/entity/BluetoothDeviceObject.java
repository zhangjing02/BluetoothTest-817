package com.senssun.bluetooth.tools.entity;

import android.bluetooth.BluetoothDevice;

public class BluetoothDeviceObject {
	private BluetoothDevice device;
	private int rssi;
	private int deviceMode=0;
	private String fullDevice;

	public String getFullDevice() {
		return fullDevice;
	}

	public void setFullDevice(String fullDevice) {
		this.fullDevice = fullDevice;
	}

	public int getDeviceMode() {
		return deviceMode;
	}
	public void setDeviceMode(int deviceMode) {
		this.deviceMode = deviceMode;
	}
	public BluetoothDevice getDevice() {
		return device;
	}
	public void setDevice(BluetoothDevice device) {
		this.device = device;
	}
	public int getRssi() {
		return rssi;
	}
	public void setRssi(int rssi) {
		this.rssi = rssi;
	}

	@Override
	public String toString() {
		return device.getAddress();
	}
	
	public boolean equals(Object obj) {   
        if (obj instanceof BluetoothDeviceObject) {   
        	BluetoothDeviceObject u = (BluetoothDeviceObject) obj;   
            return this.device.getAddress().equals(u.getDevice().getAddress());   
        }   
        return super.equals(obj); 
	}
}
