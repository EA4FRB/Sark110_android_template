package com.sark110.sark110_android_template;

/**
 ******************************************************************************
 * @author  Melchor Varela - EA4FRB
 * @brief   SARK-110 interface for Android
 ******************************************************************************
 * @copy
 *
 *  This file is a part of the "SARK110 Antenna Vector Impedance Analyzer" software
 *
 *  "SARK110 Antenna Vector Impedance Analyzer software" is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 3 of the License,
 *  or (at your option) any later version.
 *
 *  "SARK110 Antenna Vector Impedance Analyzer" software is distributed in the hope that it will be
 *  useful,  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with "SARK110 Antenna Vector Impedance Analyzer" software.  If not,
 *  see <http://www.gnu.org/licenses/>.
 *
 * <h2><center>&copy; COPYRIGHT 2018 Melchor Varela - EA4FRB </center></h2>
 *  Melchor Varela, Madrid, Spain.
 *  melchor.varela@gmail.com
 */


import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.os.Handler;

import static android.content.Context.BLUETOOTH_SERVICE;

public class BluetoothLEIntf extends DeviceIntf {
    private static final String SERVICE_STRING = "49535343-fe7d-4ae5-8fa9-9fafd205e455";
    private static final String WRITE_STRING = "49535343-1e4d-4bd9-ba61-23c647249616";
    private static final String READ_STRING = "49535343-1e4d-4bd9-ba61-23c647249616";
    private static final String DESCRIPTOR_STRING = "00002902-0000-1000-8000-00805f9b34fb";

    private static final UUID SERVICE_UUID = UUID.fromString(SERVICE_STRING);
    private static final UUID WRITE_UUID = UUID.fromString(WRITE_STRING);
    private static final UUID READ_UUID = UUID.fromString(READ_STRING);
    private static final UUID DESCRIPTOR_UUID = UUID.fromString(DESCRIPTOR_STRING);

    private static final long SCAN_TIMEOUT = 10000;      /* ms */
    private static final long RCV_TIMEOUT = 500;         /* ms */

    private boolean mScanning;
    private Handler mScanHandler;
    private Map<String, BluetoothDevice> mScanResults;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private BluetoothGatt mGatt;

    private byte[] mDataRcv;
    private CountDownLatch mSyncRcv;

    public BluetoothLEIntf(Context context)
    {
        super(context);
    }

    /* Listener handling */
    private void InCaseFireConnectionStateChanged() {
        if (mListener != null) {
            ((Activity)mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mListener.onConnectionStateChanged(BluetoothLEIntf.this, isConnected());
                }
            });
        }
    }

    /* life-cycle */
    public void onCreate()
    {
        mConnected = false;
        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager != null)
            mBluetoothAdapter = bluetoothManager.getAdapter();

        connect();
    }

    public void onResume() {
        // Check low energy support
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            // LE not supported on this device
        }
    }

    /* functions */
    public void close()
    {
        disconnectGattServer();
    }

    public void connect () {
        /*
         * Implemented the comment bond method. This requires that the user manually bonds the device
         * from the Bluetooth setup menu
         */
        connectBond();
        //startScan();
    }

    private void connectBond()
    {
        disconnectGattServer();
        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(BLUETOOTH_SERVICE);
        for (BluetoothDevice device : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            int type = device.getType();

            if (type == BluetoothDevice.DEVICE_TYPE_LE) {
                final String deviceName = device.getName();
                if (deviceName != null) {
                    if (deviceName.matches("SARK110-[0-9a-fA-F]{4}")) {
                        BluetoothLEIntf.GattClientCallback gattClientCallback = new BluetoothLEIntf.GattClientCallback();
                        mGatt = device.connectGatt(mContext, true, gattClientCallback);
                    }
                }
            }
        }
    }
    /* scanning */
    private void startScan() {
        if (mScanning)
            return;
        disconnectGattServer();
        mScanResults = new HashMap<>();
        mScanCallback = new BluetoothLEIntf.BtleScanCallback(mScanResults);

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // Note: Filtering does not work the same (or at all) on most devices. It also is unable to
        // search for a mask or anything less than a full UUID.
        // Unless the full UUID of the server is known, manual filtering may be necessary.
        // For example, when looking for a brand of device that contains a char sequence in the UUID
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        // Commented as it does not work
        // filters.add(scanFilter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

        mScanHandler = new Handler();
        mScanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        }, SCAN_TIMEOUT);

        mScanning = true;
    }

    private void stopScan() {
        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);
            scanComplete();
        }
        mScanCallback = null;
        mScanning = false;
        mScanHandler = null;
        if (mScanResults.isEmpty())
            setConnected(false);
    }

    private void scanComplete() {
        if (mScanResults.isEmpty()) {
            return;
        }

        for (String deviceAddress : mScanResults.keySet()) {
            BluetoothDevice device = mScanResults.get(deviceAddress);
            connectDevice(device);
        }
    }

    /* Gatt connection */
    private void connectDevice(BluetoothDevice device) {
        //log("Connecting to " + device.getAddress());
        BluetoothLEIntf.GattClientCallback gattClientCallback = new BluetoothLEIntf.GattClientCallback();
        mGatt = device.connectGatt(mContext, false, gattClientCallback);
    }

    /* support functions */
    private void disconnectGattServer() {
        mConnected = false;
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }
    }

    /* Callbacks */
    private class BtleScanCallback extends ScanCallback {

        private Map<String, BluetoothDevice> mScanResults;

        BtleScanCallback(Map<String, BluetoothDevice> scanResults) {
            mScanResults = scanResults;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                addScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
        }

        private void addScanResult(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceAddress = device.getAddress();
            final String deviceName = device.getName();
            if (deviceName != null)
            {
                if (deviceName.matches("SARK110-[0-9a-fA-F]{4}")) {
                    mScanResults.put(deviceAddress, device);
                    stopScan();
                }
            }
        }
    }

    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_FAILURE) {
                disconnectGattServer();
                setConnected(false);
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer();
                setConnected(false);
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectGattServer();
                setConnected(false);
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                enableReceiveNotifications();
                setConnected(true);
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            mDataRcv = characteristic.getValue();
            mSyncRcv.countDown();
        }
    }
    private void setConnected(boolean connected) {
        mConnected = connected;
        InCaseFireConnectionStateChanged();
    }

    private void sendData(byte[] data) {
        if (mBluetoothAdapter == null || mGatt == null) {
            return;
        }
        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mGatt.getService(SERVICE_UUID);
        if(mCustomService == null){
            return;
        }
        /*get the write characteristic from the service*/
        BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(WRITE_UUID);

        mWriteCharacteristic.setValue(data);
        mWriteCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        if(!mGatt.writeCharacteristic(mWriteCharacteristic)){

        }
    }

    private void enableReceiveNotifications() {
        if (mBluetoothAdapter == null || mGatt == null) {
            return;
        }
        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mGatt.getService(SERVICE_UUID);
        if(mCustomService == null){
            return;
        }

        /*get the read characteristic from the service*/
        BluetoothGattCharacteristic mReadCharacteristic = mCustomService.getCharacteristic(READ_UUID);
        BluetoothGattDescriptor descriptor = mReadCharacteristic.getDescriptor(DESCRIPTOR_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mGatt.writeDescriptor(descriptor);
        mGatt.setCharacteristicNotification(mReadCharacteristic, true);
        if(!mGatt.readCharacteristic(mReadCharacteristic)){

        }
    }

    protected int SendRcv(byte snd[], byte rcv[])
    {
        if (!mConnected)
            return -1;

        mSyncRcv = new CountDownLatch(1);
        mDataRcv = new byte[COMMAND_LEN];

        sendData(snd);

        try {
            boolean result = mSyncRcv.await(RCV_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return -1;
        }

        System.arraycopy(mDataRcv, 0, rcv, 0, COMMAND_LEN);
        return 1;
    }

}
