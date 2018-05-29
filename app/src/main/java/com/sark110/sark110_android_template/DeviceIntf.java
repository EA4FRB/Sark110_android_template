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

import android.content.Context;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class DeviceIntf {
    protected static final int COMMAND_LEN = 18;

    protected Context mContext;

    protected boolean mConnected;

    private int mProtocolVer = 0;
    private byte[] mSarkVer = null;

    public int getProtocolVer() {
        return mProtocolVer;
    }

    public byte[] getSarkVer() {
        return mSarkVer;
    }

    public DeviceIntf() {
    }
    public DeviceIntf(Context context){
        this.mContext = context;
    }
    public boolean isConnected () {
        return mConnected;
    }
    abstract void onCreate ();
    abstract void onResume ();
    abstract void connect();
    abstract int SendRcv(byte snd[], byte rcv[]);
    abstract void close();

    /* Listener handling */
    protected DeviceIntfListener mListener;

    public void setDeviceIntfListener(DeviceIntfListener listener) {
        this.mListener = listener;
    }

    public interface DeviceIntfListener {
        /**
         * Event fired when the connection status changes.
         * The event is also fired when the Connect() method ends, returning the result of the Connect() request.
         */
        public void onConnectionStateChanged(DeviceIntf helper, boolean isConnected);
    }

    public int VersionCmd ()
    {
        int status = -1;

        byte snd[] = new byte[COMMAND_LEN];
        byte rcv[] = new byte[COMMAND_LEN];
        snd[0] = 1;
        status = SendRcv(snd, rcv);
        if (rcv[0] != 'O')
            status = -1;
        if (status > 0) {
            mProtocolVer = Buf2Short(rcv, 1);
            mSarkVer = new byte[COMMAND_LEN];
            System.arraycopy(rcv, 3, mSarkVer, 0, COMMAND_LEN - 3);
        }
        return status;
    }

    public int BeepCmd()
    {
        int status = -1;

        byte snd[] = new byte[COMMAND_LEN];
        byte rcv[] = new byte[COMMAND_LEN];
        snd[0] = 20;
        status = SendRcv(snd, rcv);
        if (rcv[0] != 'O')
            status = -1;
        return status;
    }

    public MeasureDataBin MeasureCmd(float freq)
    {
        int status = -1;

        byte snd[] = new byte[COMMAND_LEN];
        byte rcv[] = new byte[COMMAND_LEN];

        snd[0] = 2;
        System.arraycopy( Int2Buf ((int)(freq*1000000)), 0, snd, 1, 4 );
        snd[5] = 1;
        snd[6] = 0;
        status = SendRcv(snd, rcv);
        if (rcv[0] != 'O')
            status = -1;
        if (status >= 0) {
            float Rs = Buf2Float(rcv, 1);
            float Xs = Buf2Float(rcv, 5);
            float S21R = Buf2Float(rcv, 9);
            float S21X = Buf2Float(rcv, 13);

            return new MeasureDataBin(0, freq, Rs, Xs);
        }
        else
            return null;
    }

    private ComplexNumber Z2Rho(ComplexNumber cxZ, ComplexNumber cxZ0)
    {
        return ComplexNumber.divide(ComplexNumber.subtract(cxZ, cxZ0), ComplexNumber.add(cxZ, cxZ0));
    }

    private float Z2Vswr (ComplexNumber cxZ, ComplexNumber cxZ0)
    {
        ComplexNumber cxRho = Z2Rho(cxZ, cxZ0);
        if (cxRho.mod() > 0.980197824)
            return 99.999f;
        return (1.0f + (float)cxRho.mod()) / (1.0f - (float)cxRho.mod());
    }

    public byte[] Int2Buf (int val)
    {
        byte[] buf = new byte[4];

        buf[3] = (byte)((val&0xff000000)>>24);
        buf[2] = (byte)((val&0x00ff0000)>>16);
        buf[1] = (byte)((val&0x0000ff00)>>8);
        buf[0] = (byte)((val&0x000000ff)>>0);

        return buf;
    }

    private int Buf2Short (byte buf[], int n)
    {
        int val;
        byte[] bufShort = new byte[2];

        System.arraycopy(buf, n, bufShort, 0, 2);
        val = bufShort[1] << 8;
        val += bufShort[0] << 0;

        return val;
    }

    private float Buf2Float (byte buf[], int n)
    {
        byte[] bufFloat = new byte[4];
        System.arraycopy(buf, n, bufFloat, 0, 4);
        float val = ByteBuffer.wrap(bufFloat).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        return val;
    }
}
