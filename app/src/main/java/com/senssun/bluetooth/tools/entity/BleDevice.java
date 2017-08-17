package com.senssun.bluetooth.tools.entity;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;
import java.io.SerializablePermission;

public class BleDevice implements Serializable {
    private static final long serialVersionUID = 1L;

	private BluetoothDevice mDevice;
    private String mNotiStr;
	private int mRssi;
    private boolean mSendSuc;

    public BluetoothDevice getmDevice() {
        return mDevice;
    }

    public void setmDevice(BluetoothDevice mDevice) {
        this.mDevice = mDevice;
    }

    public String getmNotiStr() {
        return mNotiStr;
    }

    public void setmNotiStr(String mNotiStr) {
        this.mNotiStr = mNotiStr;
    }

    public int getmRssi() {
        return mRssi;
    }

    public void setmRssi(int mRssi) {
        this.mRssi = mRssi;
    }

    public boolean ismSendSuc() {
        return mSendSuc;
    }

    public void setmSendSuc(boolean mSendSuc) {
        this.mSendSuc = mSendSuc;
    }

    public boolean equals(Object obj) {
        if (obj instanceof BleDevice) {
        	BleDevice u = (BleDevice) obj;
            return this.mDevice.getAddress().equals(u.getmDevice().getAddress());
        }   
        return super.equals(obj); 
	}
}
