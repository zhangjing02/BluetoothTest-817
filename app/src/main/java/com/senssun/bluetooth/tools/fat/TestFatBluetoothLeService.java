package com.senssun.bluetooth.tools.fat;


/**************************************************************************************************
 Filename:       BluetoothLeService.java
 Revised:        $Date: 2013-09-09 16:23:36 +0200 (ma, 09 sep 2013) $
 Revision:       $Revision: 27674 $

 Copyright 2013 Texas Instruments Incorporated. All rights reserved.

 IMPORTANT: Your use of this Software is limited to those specific rights
 granted under the terms of a software license agreement between the user
 who downloaded the software, his/her employer (which must be your employer)
 and Texas Instruments Incorporated (the "License").  You may not use this
 Software unless you agree to abide by the terms of the License.
 The License limits your use, and you acknowledge, that the Software may not be
 modified, copied or distributed unless used solely and exclusively in conjunction
 with a Texas Instruments Bluetooth device. Other than for the foregoing purpose,
 you may not use, reproduce, copy, prepare derivative works of, modify, distribute,
 perform, display or sell this Software and/or its documentation for any purpose.

 YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 PROVIDED �AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 TEXAS INSTRUMENTS OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT,
 NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER
 LEGAL EQUITABLE THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES
 INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE
 OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT
 OF SUBSTITUTE GOODS, TECHNOLOGY, SERVICES, OR ANY CLAIMS BY THIRD PARTIES
 (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.

 Should you have any questions regarding your right to use this Software,
 contact Texas Instruments Incorporated at www.TI.com

 **************************************************************************************************/

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.senssun.bluetooth.tools.entity.GattObject;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**
 * Service for managing connection and data communication with a GATT server hosted on a given Bluetooth LE device.
 */
public class TestFatBluetoothLeService extends Service {
    static final String TAG = "BluetoothLeService";

    public final static String ACTION_GATT_CONNECTED = "com.senssun.bluetooth.tools.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.senssun.bluetooth.tools.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.senssun.bluetooth.tools.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_READ = "com.senssun.bluetooth.tools.ACTION_DATA_READ";
    public final static String ACTION_DATA_NOTIFY = "com.senssun.bluetooth.tools.ACTION_DATA_NOTIFY";
    public final static String ACTION_DATA_WRITE = "com.senssun.bluetooth.tools.ACTION_DATA_WRITE";
    public final static String ACTION_DATA_RSSI = "com.senssun.bluetooth.tools.ACTION_DATA_RSSI";
    public final static String EXTRA_DATA = "com.senssun.bluetooth.tools.EXTRA_DATA";
    public final static String EXTRA_UUID = "com.senssun.bluetooth.tools.EXTRA_UUID";
    public final static String EXTRA_STATUS = "com.senssun.bluetooth.tools.EXTRA_STATUS";
    public final static String EXTRA_ADDRESS = "com.senssun.bluetooth.tools.EXTRA_ADDRESS";

    public final static String SEND_ADDRESS = "com.senssun.bluetooth.tools.SEND_ADDRESS";
    public final static String SEND_RSSI = "com.senssun.bluetooth.tools.SEND_RSSI";
    public final static String SEND_NAME = "com.senssun.bluetooth.tools.SEND_NAME";


    // BLE
    private Handler mHandler;
    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBtAdapter = null; //蓝牙适配
    private static TestFatBluetoothLeService mThis = null;
    private volatile boolean mBusy = false; // Write/read pending response
//    private BluetoothGattCharacteristic mWriteCharacteristic;
//    private List<GattObject> gattList=new ArrayList<GattObject>();
    private ArrayList<GattObject> gattList;
    private boolean overScan = true;
    private int inputRssi = 55;
    private static final long SCAN_PERIOD = 6000;// 10000;  扫描间隔
//    private BluetoothGatt mBluetoothGatt;
    //  private Dess mDess=new Dess(); //扫描线程

    public ArrayList<GattObject> getGattList() {
        return gattList;
    }

    /**
     * GATT client callbacks
     */
    private BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (gatt == null) {
                Log.e(TAG, "mBluetoothGatt not created!");
                return;
            }

            BluetoothDevice device = gatt.getDevice();
            String address = device.getAddress();
            String name = device.getName();
            Log.d(TAG, "onConnectionStateChange (" + address + ") " + newState + " status: " + status);

            try {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        for(int i=gattList.size()-1;i>=0;i--){
                            GattObject tmp = gattList.get(i);
                            if (tmp.getAddress().equals(address)) {
                                tmp.setConnect(true);
                            }
                        }
                        broadcastUpdate(ACTION_GATT_CONNECTED, address, name, status);
                        gatt.discoverServices();
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        broadcastUpdate(ACTION_GATT_DISCONNECTED, address, name, status);

                        for(int i=gattList.size()-1;i>=0;i--){
                            GattObject tmp = gattList.get(i);
                            if (tmp.getAddress().equals(address)) {
                                tmp.getmBluetoothGatt().close();
                                tmp.setConnect(false);
                            }
                        }

                        break;
                    default:
                        Log.e(TAG, "New state not processed: " + newState);
                        break;
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothDevice device = gatt.getDevice();
            displayGattServices(getSupportedGattServices(gatt), gatt);

            broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, device.getAddress(), device.getName(), status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            BluetoothDevice device = gatt.getDevice();
            broadcastUpdate(ACTION_DATA_NOTIFY, characteristic, device.getAddress(), device.getName(), BluetoothGatt.GATT_SUCCESS);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            BluetoothDevice device = gatt.getDevice();
            broadcastUpdate(ACTION_DATA_READ, characteristic, device.getAddress(), device.getName(), status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            BluetoothDevice device = gatt.getDevice();
            broadcastUpdate(ACTION_DATA_WRITE,characteristic,device.getAddress(),device.getName(),status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            mBusy = false;
            Log.i(TAG, "onDescriptorRead");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            mBusy = false;
            Log.i(TAG, "onDescriptorWrite");
        }

        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            BluetoothDevice device = gatt.getDevice();
            broadcastUpdate(ACTION_DATA_RSSI, rssi, device.getAddress(), device.getName(), BluetoothGatt.GATT_SUCCESS);
        }
    };

    private void broadcastUpdate(final String action, final String address, final String name, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(SEND_NAME, name);
        intent.putExtra(EXTRA_ADDRESS, address);
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
        mBusy = false;
    }

    private void broadcastUpdate(final String action, final int Rssi, final String address, final String name, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(SEND_RSSI, String.valueOf(Rssi));
        intent.putExtra(EXTRA_ADDRESS, address);
        intent.putExtra(SEND_NAME, name);
        sendBroadcast(intent);
        mBusy = false;
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic, final String address, final String name, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(SEND_NAME, name);
        intent.putExtra(EXTRA_ADDRESS, address);

        final byte[] data = characteristic.getValue();
        StringBuilder stringBuilder = new StringBuilder(data.length);
        if (data != null && data.length >= 8) {
            for (byte byteChar : data) {
                String ms = String.format("%02X ", byteChar).trim();
                stringBuilder.append(ms + "-");
            }
        }
        intent.putExtra(EXTRA_DATA, stringBuilder.toString());
        sendBroadcast(intent);
        mBusy = false;
    }

    private boolean checkGatt(BluetoothGatt mBluetoothGatt) {
        if (mBtAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return false;
        }

        if (mBusy) {
            Log.w(TAG, "LeService busy");
            return false;
        }
        return true;

    }

    /**
     * Manage the BLE service
     */
    public class LocalBinder extends Binder {
        public TestFatBluetoothLeService getService() {
            return TestFatBluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBluetoothManager.getAdapter();

        gattList = new ArrayList<GattObject>();
        mHandler = new Handler();
        //		Thread t1=new Thread(mDess,"Window 1");
        //		t1.start();
        scanLeDevice(true);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        return binder;
    }


    public void sendTimeHander() { //每隔多少秒发一次
        Log.d(TAG, "间隔发送");

        Calendar cal = Calendar.getInstance();
        byte[] sendTimeBuffer = new byte[8];
        String byte0=Long.toHexString(165);
        sendTimeBuffer[0]=(byte)Integer.parseInt(byte0, 16);

        int intByte1=48;
        String byte1=Long.toHexString(48); //脂肪发送日期模式 30
        sendTimeBuffer[1]=(byte)Integer.parseInt(byte1, 16);

        int intByte2=Integer.valueOf(String.valueOf(cal.get(Calendar.YEAR)).substring(2));
        String byte2=Long.toHexString(intByte2);
        sendTimeBuffer[2]=(byte)Integer.parseInt(byte2, 16);

        int intByte3and4=cal.get(Calendar.DAY_OF_YEAR);
        String byte3and4=Long.toHexString(intByte3and4);
        byte3and4=byte3and4.length()==1?"000"+byte3and4:
                byte3and4.length()==2?"00"+byte3and4:
                        byte3and4.length()==3?"0"+byte3and4:byte3and4;

        String byte3=byte3and4.substring(0,2);
        sendTimeBuffer[3]=(byte)Integer.parseInt(byte3, 16);
        int intByte3=Integer.valueOf(byte3,16);//Integer.valueOf(byte3.substring(0,1))*16+Integer.valueOf(byte3.substring(1,2));

        String byte4=byte3and4.substring(2,4);
        sendTimeBuffer[4]=(byte)Integer.parseInt(byte4, 16);
        int intByte4=Integer.valueOf(byte4,16);//Integer.valueOf(byte4.substring(0,1))*16+Integer.valueOf(byte4.substring(1,2));

        int intbyte7=intByte1+intByte2+intByte3+intByte4;
        String byte7=Long.toHexString(intbyte7);
        byte7=byte7.substring(byte7.length()-2,byte7.length());
        //				byte7="6E";
        sendTimeBuffer[7]=(byte)Integer.parseInt(byte7, 16);

        for(int i=gattList.size()-1;i>=0;i--){
            GattObject tmp =gattList.get(i);
            BluetoothGattCharacteristic mWriteCharacteristic = tmp.getmWriteCharacteristic();
            if (mWriteCharacteristic != null&& tmp.isConnect()&&tmp.isSend()==false ) {

                mWriteCharacteristic.setValue(sendTimeBuffer);
                boolean istrue = writeCharacteristic(mWriteCharacteristic, tmp.getmBluetoothGatt());

                Log.d(TAG, "对" + tmp.getAddress() + "发出命令" + istrue);
            }
        }

    }

    public void sendTestHander() { //每隔多少秒发一次


        byte[] byBuffer = new byte[8];
        String byte0=Long.toHexString(165).toUpperCase();
        byBuffer[0]=(byte)Integer.parseInt(byte0, 16);

        int intByte1=16; //脂肪测试模式 10
        String byte1=Long.toHexString(intByte1).toUpperCase();
        byBuffer[1]=(byte)Integer.parseInt(byte1, 16);
        int sex;
        if(true){
            sex=1;
        }else{
            sex=0;
        }

        int intByte2=(sex==0?0:8)*16+Integer.valueOf("1");
        //				intByte2=1;
        String byte2=Long.toHexString(intByte2).toUpperCase();
        //				byte2="01";
        byBuffer[2]=(byte)Integer.parseInt(byte2, 16);

        int intByte3=Integer.valueOf("20");
        String byte3=Long.toHexString(intByte3).toUpperCase();
        //				byte3="17";
        byBuffer[3]=(byte)Integer.parseInt(byte3, 16);

        int intByte4=Integer.valueOf("175");
        String byte4=Long.toHexString(intByte4).toUpperCase();
        byBuffer[4]=(byte)Integer.parseInt(byte4, 16);


        String HeightTmp=String.valueOf(new BigDecimal(intByte4/0.254).setScale(0, BigDecimal.ROUND_HALF_UP));
        String Height=HeightTmp.substring(0, HeightTmp.length()-1)+((Integer.valueOf(HeightTmp.substring(HeightTmp.length()-1))>=5&&Integer.valueOf(HeightTmp.substring(HeightTmp.length()-1))<=9)?"5":"0");

        int intbyte5and6=Integer.valueOf(Height);
        String byte5and6=Long.toHexString(intbyte5and6).toUpperCase();
        byte5and6=byte5and6.length()==2?"00"+byte5and6:
                byte5and6.length()==3?"0"+byte5and6:byte5and6;

        String byte5=byte5and6.substring(0,2).toUpperCase();
        byBuffer[5]=(byte)Integer.parseInt(byte5, 16);
        int intByte5=Integer.valueOf(byte5,16);//Integer.valueOf(byte3.substring(0,1))*16+Integer.valueOf(byte3.substring(1,2));

        String byte6=byte5and6.substring(2,4).toUpperCase();
        byBuffer[6]=(byte)Integer.parseInt(byte6, 16);
        int intByte6=Integer.valueOf(byte6,16);//Integer.valueOf(byte4.substring(0,1))*16+Integer.valueOf(byte4.substring(1,2));
        ////////////////////////////////////////////////////////

        int intbyte7=intByte1+intByte2+intByte3+intByte4+intByte5+intByte6;//+yingcun+yingcunDecimal;
        String byte7=Long.toHexString(intbyte7).toUpperCase();
        byte7=byte7.substring(byte7.length()-2,byte7.length());
        //				byte7="6E";
        byBuffer[7]=(byte)Integer.parseInt(byte7, 16);

        for(int i=gattList.size()-1;i>=0;i--){
            GattObject tmp =gattList.get(i);
            BluetoothGattCharacteristic mWriteCharacteristic = tmp.getmWriteCharacteristic();
            if (mWriteCharacteristic != null && tmp.isSend()&&tmp.isConnect()) {

                mWriteCharacteristic.setValue(byBuffer);
                boolean istrue = writeCharacteristic(mWriteCharacteristic, tmp.getmBluetoothGatt());

                Log.d(TAG, "对" + tmp.getAddress() + "发出测试" + istrue);
            }
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {    //广播接收
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TestFatBluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
//				scanLeDevice(true);
            } else if (TestFatBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

            } else if (ACTION_DATA_NOTIFY.equals(action)) {
//                displyData(intent.getStringExtra(EXTRA_DATA), intent.getStringExtra(SEND_NAME), intent.getStringExtra(EXTRA_ADDRESS));
            }
        }
    };



    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TestFatBluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(TestFatBluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(TestFatBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(TestFatBluetoothLeService.ACTION_DATA_NOTIFY);
        return intentFilter;
    }

    /**
     * ************************************自定义搜索连接代码****************************************
     */
    /*   搜索设备    */
    public void scanLeDevice(final boolean enable) { //搜索BLE设备
        if (enable) {
            // Stops scanning after a pre-defined scan period 预定义的扫描周期后停止扫描
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (overScan) {
                        mBtAdapter.stopLeScan(mLeScanCallback);
                        scanLeDevice(true);
                    }
//                    mBtAdapter.stopLeScan(mLeScanCallback);
//                    scanLeDevice(true);
                }
            }, SCAN_PERIOD);

            mBtAdapter.startLeScan(mLeScanCallback);
        } else {
            mBtAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback. 设备扫描回调
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int i = 0;
                            GattObject tmp = null;
                            if (device.getName() == null) return;
                            if (device.getName().trim().equals("BLE to UART_2") || device.getName().trim().equals("SENSSUN FAT") || device.getName().trim().equals("SENSSUN FOOD00")) {
                                if (inputRssi < Math.abs(rssi)) {
                                } else {
//                                    for(int j=gattList.size()-1;j>=0;j--){
//                                        tmp = gattList.get(j);
//                                        if (tmp.getAddress().equals(device.getAddress())) {
//                                            i = 1;
//                                            break;
//                                        }
//                                    }
//                                    if (i == 0 || tmp.isAllowConnect()) {
                                        connect(device.getAddress()); //连接
//                                    }
                                }
                            }
                        }
                    }).start();
                }
            };

    private void displayGattServices(List<BluetoothGattService> gattServices, BluetoothGatt mBluetoothGatt) {
        if (gattServices == null) return;
        //遍历 GATT 服务可用
        for (BluetoothGattService gattService : gattServices) {
            if (gattService.getUuid().toString().equals("0000fff0-0000-1000-8000-00805f9b34fb")) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    if (gattCharacteristic.getUuid().toString().trim().equals("0000fff1-0000-1000-8000-00805f9b34fb")) {
                        setCharacteristicNotification(gattCharacteristic, mBluetoothGatt, true);
                    }
                    if (gattCharacteristic.getUuid().toString().trim().equals("0000fff2-0000-1000-8000-00805f9b34fb")) {
                        for(int i=gattList.size()-1;i>=0;i--){
                            GattObject tmp = gattList.get(i);
                            if (tmp.getAddress().equals(mBluetoothGatt.getDevice().getAddress())) {
                                tmp.setmWriteCharacteristic(gattCharacteristic);
                                }
                        }
                    }
                }
            }

        }
    }

    public void WriteChar(byte[] byBuffer) {
        for(int i=gattList.size()-1;i>=0;i--){
            GattObject tmp=gattList.get(i);
            BluetoothGattCharacteristic mWriteCharacteristic=tmp.getmWriteCharacteristic();

            if (mWriteCharacteristic != null) {
                mWriteCharacteristic.setValue(byBuffer);//连接发送时间
                boolean istrue = writeCharacteristic(mWriteCharacteristic, tmp.getmBluetoothGatt());
                Log.d(TAG, "对" + tmp.getAddress() + "发出命令" + istrue);
            }
        }
    }

    /**
     * ************************************    自定义搜索连接结束代        ******************************************
     */
    @Override
    public boolean onUnbind(Intent intent) {
        scanLeDevice(false);
        overScan = false;
        unregisterReceiver(mGattUpdateReceiver);
        disconnect();
        close();
        return super.onUnbind(intent);
    }

    private final IBinder binder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {

        Log.d(TAG, "initialize");
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        mThis = this;
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBtAdapter = mBluetoothManager.getAdapter();
        if (mBtAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
    }

//
// GATT API
//

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)} callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic, BluetoothGatt mBluetoothGatt) {
        if (!checkGatt(mBluetoothGatt))
            return;
        mBusy = true;
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, BluetoothGatt mBluetoothGatt) {
        if (!checkGatt(mBluetoothGatt))
            return false;

        mBusy = true;
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be invoked only after {@code BluetoothGatt#discoverServices()} completes
     * successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices(BluetoothGatt mBluetoothGatt) {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }


    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, BluetoothGatt mBluetoothGatt, boolean enable) {
        if (!checkGatt(mBluetoothGatt))
            return false;

        if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            Log.w(TAG, "setCharacteristicNotification failed");
            return false;
        }

        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if (clientConfig == null)
            return false;

        if (enable) {
            Log.i(TAG, "enable notification");
            clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            Log.i(TAG, "disable notification");
            clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }

        mBusy = true;
        return mBluetoothGatt.writeDescriptor(clientConfig);
    }

    public boolean isNotificationEnabled(BluetoothGattCharacteristic characteristic, BluetoothGatt mBluetoothGatt) {
        if (!checkGatt(mBluetoothGatt))
            return false;

        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if (clientConfig == null)
            return false;

        return clientConfig.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)} callback.
     */
    public boolean connect(final String address) {
        if (mBtAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        final BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
        int connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

        if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {

            if (device == null) {
                Log.w(TAG, "Device not found.  Unable to connect.");
                return false;
            }
            // We want to directly connect to the device, so we are setting the
            // autoConnect parameter to false.
            int i = 0;
            for(int j=gattList.size()-1;j>=0;j--){
                GattObject tmp = gattList.get(j);
                if (tmp.getAddress().equals(address)) {
                    if (tmp.getmBluetoothGatt() != null) {
                        tmp.getmBluetoothGatt().disconnect();
                        tmp.getmBluetoothGatt().close();

                        BluetoothGatt mBluetoothGatt = device.connectGatt(this, false, mGattCallbacks);
                        tmp.setmBluetoothGatt(mBluetoothGatt);
//                        tmp.setSend(true);
                        i = 1;
                    }
                }
            }
            if (i == 0) {
                BluetoothGatt mBluetoothGatt = device.connectGatt(this, false, mGattCallbacks);
                GattObject gattObject = new GattObject();
                gattObject.setAddress(address);
                gattObject.setmBluetoothGatt(mBluetoothGatt);
//                gattObject.setSend(true);
                gattList.add(gattObject);
            }
        } else {
            Log.w(TAG, "Attempt to connect in state: " + connectionState);
            return false;
        }
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)} callback.
     */
    public void disconnect(BluetoothGatt mBluetoothGatt) {
        if (mBtAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void disconnect() {
        for(int i=gattList.size()-1;i>=0;i--) {
            BluetoothGatt mBluetoothGatt=gattList.get(i).getmBluetoothGatt();
            if (mBtAdapter == null || mBluetoothGatt == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }
            mBluetoothGatt.disconnect();
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are released properly.
     */
    public void close() {
        for(int i=gattList.size()-1;i>=0;i--) {
            BluetoothGatt mBluetoothGatt = gattList.get(i).getmBluetoothGatt();
            if (mBluetoothGatt != null) {
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            }
        }
    }
//
// Utility functions
//
//	public static BluetoothGatt getBtGatt() {
//		return mThis.mBluetoothGatt;
//	}

    public void GetRssi() {
        for(int i=gattList.size()-1;i>=0;i--){
            GattObject tmp = gattList.get(i);
            if (tmp != null && tmp.getmBluetoothGatt() != null) {
                tmp.readRemoteRssi();
            }
        }
    }

    public static BluetoothManager getBtManager() {
        return mThis.mBluetoothManager;
    }

    public static TestFatBluetoothLeService getInstance() {
        return mThis;
    }

    public void setInputRssi(String inputRssi) {
        int rssi = 55;
        try {
            rssi = Integer.valueOf(inputRssi);
        } catch (Exception e) {
        }
        this.inputRssi = rssi;
    }


}
