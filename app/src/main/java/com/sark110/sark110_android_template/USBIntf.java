package com.sark110.sark110_android_template;

/**
 ******************************************************************************
 * @author  Melchor Varela - EA4FRB
 *
 * SARK-110 interface for Android
 ******************************************************************************
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

public class USBIntf extends DeviceIntf {
    private static final String ACTION_USB_PERMISSION = "com.google.android.HID.action.USB_PERMISSION";

    private UsbDevice mUsbDevice;
    private UsbDeviceConnection mConnection;
    private UsbManager mUsbManager;
    private UsbEndpoint mEndPointRead;
    private UsbEndpoint mEndPointWrite;
    private UsbInterface mUsbIntf;
    private PendingIntent mPermissionIntent;

    public USBIntf(Context context)
    {
        super(context);
    }

    public void connect () {
    }

    public void close()
    {
        mConnected = false;
        mContext.unregisterReceiver(mUsbReceiver);				        //Unregister broadcast receiver
    }

    public void onCreate () {
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
        mConnection = mUsbManager.openDevice(mUsbDevice);
        mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);	//Get USB permission intent for broadcast
        mUsbManager.requestPermission(mUsbDevice, mPermissionIntent);
    }

    public void onResume() {

    }

    private void setDevice(Intent intent) {
        mUsbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
        {
            mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);	//Get USB permission intent for broadcast
            mUsbManager.requestPermission(mUsbDevice, mPermissionIntent);
        }
        if (mUsbDevice != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            mConnection = mUsbManager.openDevice(mUsbDevice);		//Connect to device
            mUsbIntf = mUsbDevice.getInterface(1);
            if (null == mConnection) {

            } else {
                mConnection.claimInterface(mUsbIntf, true);			//Device connected - claim ownership over the interface
            }
            try {
                //Direction of end point 1 - OUT - from host to device
                if (UsbConstants.USB_DIR_OUT == mUsbIntf.getEndpoint(1).getDirection()) {
                    mEndPointWrite = mUsbIntf.getEndpoint(1);
                }
            } catch (Exception e) {

            }
            try {
                //Direction of end point 0 - IN - from device to host
                if (UsbConstants.USB_DIR_IN == mUsbIntf.getEndpoint(0).getDirection()) {
                    mEndPointRead = mUsbIntf.getEndpoint(0);
                }
            } catch (Exception e) {

            }
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    setDevice(intent);
                    setConnected(true);
                }
            }
            //device attached
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                synchronized (this) {
                    setDevice(intent);        //Connect to the selected device
                    setConnected(true);
                }
                if (mUsbDevice == null) {

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
