package com.senssun.bluetooth.tools.pad.test;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ScanService extends Service {

    private final String TAG = ScanService.class.getSimpleName();

    private String mConnName="SENSSUN Pad";
    private String mConnAddress="98:7B:F3:C7:49:B9";


    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    public ScanService() {
    }

    public boolean initialize() {
        Log.e(TAG,"---------here");
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        return true;
    };

    public class LocalBinder extends Binder {
        ScanService getService() {
            return ScanService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        return super.onUnbind(intent);
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    Log.e(TAG, "**********device Name:" + device.getName());
                    if(device.getName()==null)return;
                    if(!device.getName().equals(mConnName))return;
                    if(!device.getAddress().equals(mConnAddress))return;

                }
            };
}
