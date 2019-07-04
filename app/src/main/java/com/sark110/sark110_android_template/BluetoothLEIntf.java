package com.sark110.sark110_android_template;

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

/**
 * This file is a part of the "SARK110 Antenna Vector Impedance Analyzer" software
 *
 * MIT License
 *
 * @author Copyright (c) 2018 Melchor Varela - EA4FRB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
class BluetoothLEIntf extends DeviceIntf {
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

    private boolean mScanning = false;
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
        mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /* functions */
    public void close()
    {
        stopScan();
        disconnectGattServer();
    }

    void connect() {
        /*
         * Implements the comment bond method. This requires that the user manually bonds the device
         * from the Bluetooth setup menu.
         */
        //connectBond();

        /*
         * Implements the scan method.
         */
        startScan();
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
                    if (deviceName.matches("SARK110.[0-9a-fA-F]{4}")) {
                        BluetoothLEIntf.GattClientCallback gattClientCallback = new BluetoothLEIntf.GattClientCallback();
                        mGatt = device.connectGatt(mContext, true, gattClientCallback);
                        break;
                    }
                }
            }
        }
    }

    boolean IsAvailable()
    {
        if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)== false)
            return false;

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter.isEnabled();
    }

    /* scanning */
    private void startScan() {
        if (!IsAvailable()) {
            return;
        }
        if (mScanning)      // Scanning already in progress
            return;
        disconnectGattServer();
        mScanResults = new HashMap<>();
        mScanCallback = new BluetoothLEIntf.BtleScanCallback(mScanResults);

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mBluetoothLeScanner == null)
            return;

        // Note: Filtering does not work the same (or at all) on most devices. It also is unable to
        // search for a mask or anything less than a full UUID.
        // Unless the full UUID of the server is known, manual filtering may be necessary.
        // For example, when looking for a brand of device that contains a char sequence in the UUID
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();

        ScanSettings settings = new ScanSettings.Builder()
//                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)    // Faster
                .build();

        List<ScanFilter> filters = new ArrayList<>();
        // Commented as it does not work
        //filters.add(scanFilter);

        mScanning = true;
        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

        mScanHandler = new Handler();
        mScanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        }, SCAN_TIMEOUT);

    }

    private void stopScan() {
        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
            if (mBluetoothLeScanner != null)
                mBluetoothLeScanner.stopScan(mScanCallback);
            mBluetoothLeScanner = null;
        }
        mScanCallback = null;
        mScanHandler = null;
        if (mScanResults.isEmpty()) {
            mScanning = false;
            setConnected(false);
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

        private final Map<String, BluetoothDevice> mScanResults;

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
            if (!mScanResults.isEmpty())        /* Already detected */
                return;
            BluetoothDevice device = result.getDevice();
            String deviceAddress = device.getAddress();
            final String deviceName = device.getName();
            if (deviceName != null)
            {
                if (deviceName.matches("SARK110.[0-9a-fA-F]{4}")) {
                    mScanResults.put(deviceAddress, device);
                    BluetoothDevice sarkDevice = mScanResults.get(deviceAddress);
                    connectDevice(sarkDevice);
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
        if (connected == false)
            mScanning = false;  // A new scan could be started
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
        mGatt.writeCharacteristic(mWriteCharacteristic);
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
        mGatt.readCharacteristic(mReadCharacteristic);
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
