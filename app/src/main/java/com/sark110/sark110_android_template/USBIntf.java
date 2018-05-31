package com.sark110.sark110_android_template;

/*
 * This file is a part of the "SARK110 Antenna Vector Impedance Analyzer" software
 *
 * MIT License
 *
 * Copyright (c) 2018 Melchor Varela - EA4FRB
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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import java.util.Objects;

class USBIntf extends DeviceIntf {
    private static final String ACTION_USB_PERMISSION = "com.google.android.HID.action.USB_PERMISSION";

    private UsbDevice mUsbDevice;
    private UsbDeviceConnection mConnection;
    private UsbManager mUsbManager;
    private UsbEndpoint mEndPointRead;
    private UsbEndpoint mEndPointWrite;
    private PendingIntent mPermissionIntent;

    public USBIntf(Context context)
    {
        super(context);
    }

    public void connect () {
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mContext.registerReceiver(mUsbReceiver, filter);				//Register broadcast receiver

        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        if (mUsbManager != null && mUsbManager.getDeviceList().isEmpty())
            return;
        if (mUsbManager != null) {
            mUsbDevice = (UsbDevice) mUsbManager.getDeviceList().values().toArray()[0];
        }
        mConnection = Objects.requireNonNull(mUsbManager).openDevice(mUsbDevice);
        mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);	//Get USB permission intent for broadcast
        mUsbManager.requestPermission(mUsbDevice, mPermissionIntent);
    }

    public void close()
    {
        mConnected = false;
        mContext.unregisterReceiver(mUsbReceiver);				        //Unregister broadcast receiver
    }

    public void onCreate () {
        connect();
    }

    public void onResume() {

    }

    private boolean setDevice(Intent intent) {
        boolean rc = true;

        mUsbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
        {
            mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);	//Get USB permission intent for broadcast
            mUsbManager.requestPermission(mUsbDevice, mPermissionIntent);
        }
        if (mUsbDevice != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            mConnection = mUsbManager.openDevice(mUsbDevice);		//Connect to device
            UsbInterface usbIntf = mUsbDevice.getInterface(1);
            if (null == mConnection) {
                rc = false;
            } else {
                mConnection.claimInterface(usbIntf, true);			//Device connected - claim ownership over the interface
            }
            try {
                //Direction of end point 1 - OUT - from host to device
                if (UsbConstants.USB_DIR_OUT == usbIntf.getEndpoint(1).getDirection()) {
                    mEndPointWrite = usbIntf.getEndpoint(1);
                }
            } catch (Exception e) {
                rc = false;
            }
            try {
                //Direction of end point 0 - IN - from device to host
                if (UsbConstants.USB_DIR_IN == usbIntf.getEndpoint(0).getDirection()) {
                    mEndPointRead = usbIntf.getEndpoint(0);
                }
            } catch (Exception e) {
                rc = false;
            }
        }
        return rc;
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    if (setDevice(intent))
                        setConnected(true);
                    else
                        setConnected(false);
                }
            }
            //device attached
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                synchronized (this) {
                    if (setDevice(intent))
                        setConnected(true);
                    else
                        setConnected(false);
                }
            }
            //device detached
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                if (mUsbDevice != null) {
                    mUsbDevice = null;
                    setConnected(false);
                }
            }
        }
    };

    protected int SendRcv(byte snd[], byte rcv[]) {
        int status = -1;
        if (!mConnected)
            return status;
        if (mUsbDevice != null && mEndPointWrite != null && mUsbManager.hasPermission(mUsbDevice)) {
            status = mConnection.bulkTransfer(mEndPointWrite, snd, COMMAND_LEN, 255); 	//Send data to device
            if (status > 0)
                status = mConnection.bulkTransfer(mEndPointRead, rcv, COMMAND_LEN, 255);	//Read data from device
        }
        return status;
    }

    private void setConnected(boolean connected) {
        mConnected = connected;
        InCaseFireConnectionStateChanged();
    }

    /* Listener handling */
    private void InCaseFireConnectionStateChanged() {
        if (mListener != null) {
            ((Activity)mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mListener.onConnectionStateChanged(USBIntf.this, isConnected());
                }
            });
        }
    }
}
