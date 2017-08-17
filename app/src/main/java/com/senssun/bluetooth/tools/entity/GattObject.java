package com.senssun.bluetooth.tools.entity;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

public class GattObject {
    private String address;
    private String rssi;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private BluetoothGattCharacteristic mNoticeCharacteristic;
    private boolean allowConnect = true;//允许连接
    private boolean isSend = false;//0：可以发送 1：已经发送
    private boolean isConnect=false;//连接状态

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRssi() {
        return rssi;
    }

    public void setRssi(String rssi) {
        this.rssi = rssi;
    }

    public BluetoothGatt getmBluetoothGatt() {
        return mBluetoothGatt;
    }

    public void setmBluetoothGatt(BluetoothGatt mBluetoothGatt) {
        this.mBluetoothGatt = mBluetoothGatt;
    }

    public BluetoothGattCharacteristic getmWriteCharacteristic() {
        return mWriteCharacteristic;
    }

    public void setmWriteCharacteristic(
            BluetoothGattCharacteristic mWriteCharacteristic) {
        this.mWriteCharacteristic = mWriteCharacteristic;
    }

    public BluetoothGattCharacteristic getmNoticeCharacteristic() {
        return mNoticeCharacteristic;
    }

    public void setmNoticeCharacteristic(
            BluetoothGattCharacteristic mNoticeCharacteristic) {
        this.mNoticeCharacteristic = mNoticeCharacteristic;
    }

    public void readRemoteRssi() {
        if (mBluetoothGatt != null)
            mBluetoothGatt.readRemoteRssi();
    }

    public boolean isSend() {
        return isSend;
    }

    public void setSend(boolean isSend) {
        this.isSend = isSend;
    }

    public boolean isConnect() {
        return isConnect;
    }

    public void setConnect(boolean isConnect) {
        this.isConnect = isConnect;
    }

    public boolean isAllowConnect() {
        return allowConnect;
    }

    public void setAllowConnect(boolean allowConnect) {
        this.allowConnect = allowConnect;
    }
}
