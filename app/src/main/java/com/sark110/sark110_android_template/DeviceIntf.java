package com.sark110.sark110_android_template;

import android.content.Context;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
public abstract class DeviceIntf {
    static final int COMMAND_LEN = 18;

    private static final int CMD_SARK_VERSION =		1;	/* Returns version of the protocol */
    private static final int CMD_SARK_MEAS_RX =		2;	/* Measures R and X */
    private static final int CMD_SARK_MEAS_VECTOR =	3;	/* Measures raw vector data */
    private static final int CMD_SARK_SIGNAL_GEN =	4;	/* Signal generator */
    private static final int CMD_SARK_MEAS_RF =		5;	/* Measures RF */
    private static final int CMD_SARK_MEAS_VEC_THRU =	6;	/* Measures raw vector thru data */
    private static final int CMD_BATT_STAT =		7;	/* Battery charger status */
    private static final int CMD_DISK_INFO =		8;	/* Get disk information */
    private static final int CMD_DISK_VOLUME =		9;	/* Get disk volume name */
    private static final int CMD_SARK_MEAS_RX_EXT =	12;	/* Measures R and X; efficient (x4 samples) */
    private static final int CMD_BUZZER	=			20;	/* Sounds buzzer */
    private static final int CMD_GET_KEY =			21;	/* Get key */
    private static final int CMD_DEV_RST =			50;	/* Reset */

    /* Position 0 in responses */
    private static final int ANS_SARK_OK =			'O';
    private static final int ANS_SARK_ERR =			'E';

    /* Position 5 in CMD_SARK_MEAS_RX */
    private static final int PAR_SARK_CAL =			1;	/* OSL calibrated val */
    private static final int PAR_SARK_UNCAL =		2;	/* Raw val */

    /**
     * Raw detectors measurement output
     */
    public class MeasureDetector {
        private float mFreq;
        private float mMagV;
        private float mPhV;
        private float mMagI;
        private float mPhI;

        public float getFreq() {
            return mFreq;
        }
        public float getMagV() {
            return mMagV;
        }
        public float getPhV() {
            return mPhV;
        }
        public float getMagI() {
            return mMagI;
        }
        public float getPhI() {
            return mPhI;
        }
        MeasureDetector(float freq, float magV, float phV, float magI, float phI) {
            this.mFreq = freq;
            this.mMagV = magV;
            this.mPhV = phV;
            this.mMagI = magI;
            this.mPhI = phI;
        }
    }

    Context mContext;
    boolean mConnected;
    private int mProtocolVer = 0;
    private byte[] mSarkVer = null;

    /**
     * Constructors
     */
    DeviceIntf() {
    }
    DeviceIntf(Context context){
        this.mContext = context;
    }

    /**
     * Gets connection status
     *
     * @return true if connected
     */
    boolean isConnected() {
        return mConnected;
    }
    abstract void onCreate ();
    abstract void onResume ();
    abstract void connect();
    abstract int SendRcv(byte snd[], byte rcv[]);
    abstract void close();

    /* Listener handling */
    DeviceIntfListener mListener;

    public void setDeviceIntfListener(DeviceIntfListener listener) {
        this.mListener = listener;
    }

    public interface DeviceIntfListener {
        /**
         * Event fired when the connection status changes.
         * The event is also fired when the Connect() method ends, returning the result of the Connect() request.
         */
        void onConnectionStateChanged(DeviceIntf helper, boolean isConnected);
    }

    /**
     * Sends the sark110 get version command.
     * After that, use getSarkVer() getProtocolVer() methods to retrieve the version.
     *
     * @return <0 error; otherwise ok
     */
    public int VersionCmd () {
        int status;

        byte snd[] = new byte[COMMAND_LEN];
        byte rcv[] = new byte[COMMAND_LEN];
        snd[0] = CMD_SARK_VERSION;
        status = SendRcv(snd, rcv);
        if (rcv[0] != ANS_SARK_OK)
            status = -1;
        if (status > 0) {
            mProtocolVer = Buf2Short(rcv, 1);
            mSarkVer = new byte[COMMAND_LEN];
            System.arraycopy(rcv, 3, mSarkVer, 0, COMMAND_LEN - 3);
        }
        return status;
    }

    /**
     * Gets sark110 firmware version; call VersionCmd() first
     *
     * @return firmware version
     */
    public byte[] getSarkVer() {
        return mSarkVer;
    }
    /**
     * Gets sark110 protocol version; call VersionCmd() first
     *
     * @return protocol version
     */
    public int getProtocolVer() {
        return mProtocolVer;
    }

    /**
     * Sounds the sark110 buzzer.
     *
     * @param freq      frequency in hertz
     * @param duration  duration in ms
     * @return <0 error; otherwise ok
     */
    public int BeepCmd(int freq, int duration) {
        int status;

        byte snd[] = new byte[COMMAND_LEN];
        byte rcv[] = new byte[COMMAND_LEN];
        snd[0] = CMD_BUZZER;
        System.arraycopy(Short2Buf(freq), 0, snd,1, 2);
        System.arraycopy(Short2Buf(duration), 0, snd,3, 2);
        status = SendRcv(snd, rcv);
        if (rcv[0] != ANS_SARK_OK)
            status = -1;
        return status;
    }

    /**
     * Sounds the sark110 buzzer.
     *
     * @return <0 error; otherwise ok
     */
    public int BeepCmd() {
        return BeepCmd(0, 0);
    }

    /**
     * Takes one measurement sample at the specified frequency
     *
     * @param freq      frequency in MHz; use 0 to turn-off the generator
     * @param samples   number of samples for averaging
     * @return  data; or null if error
     */
    public MeasureDataBin MeasureCmd(float freq, byte samples) {
        int status;

        byte snd[] = new byte[COMMAND_LEN];
        byte rcv[] = new byte[COMMAND_LEN];

        snd[0] = CMD_SARK_MEAS_RX;
        System.arraycopy( Int2Buf ((int)(freq*1000000)), 0, snd, 1, 4 );
        snd[5] = PAR_SARK_CAL;
        snd[6] = samples;
        status = SendRcv(snd, rcv);
        if (rcv[0] != ANS_SARK_OK)
            status = -1;
        if (status >= 0) {
            float Rs = Buf2Float(rcv, 1);
            float Xs = Buf2Float(rcv, 5);
            // Future 2-port model:
            // float S21R = Buf2Float(rcv, 9);
            // float S21X = Buf2Float(rcv, 13);

            return new MeasureDataBin(0, freq, Rs, Xs);
        }
        else
            return null;
    }
    /**
     * Takes one measurement sample at the specified frequency
     *
     * @param freq      frequency in MHz; use 0 to turn-off the generator
     * @return  data; or null if error
     */
    public MeasureDataBin MeasureCmd(float freq) {
        return MeasureCmd(freq, (byte)0);
    }

    /**
     * Takes measurement samples at four frequencies for fastest sweep speed.
     * Precision is a bit compromised because the use of half-float
     *
     * @param freq      initial frequency in MHz; use 0 to turn-off the generator
     * @param step      step frequency in MHz
     * @param samples   number of samples for averaging
     * @return data (four); or null if error
     */
    public MeasureDataBin[] MeasureCmdExt(float freq, float step, byte samples) {
        int status;
        byte snd[] = new byte[COMMAND_LEN];
        byte rcv[] = new byte[COMMAND_LEN];

        snd[0] = CMD_SARK_MEAS_RX_EXT;
        System.arraycopy( Int2Buf ((int)(freq*1000000)), 0, snd, 1, 4 );
        snd[5] = PAR_SARK_CAL;
        snd[6] = samples;
        System.arraycopy( Int2Buf ((int)(step*1000000)), 0, snd, 7, 4 );
        status = SendRcv(snd, rcv);
        if (rcv[0] != ANS_SARK_OK)
            status = -1;
        if (status >= 0) {
            int offset = 1;
            int iRs, iXs;
            float fRs, fXs;
            MeasureDataBin[] data = new MeasureDataBin[4];

            iRs = Buf2Short(rcv, offset);
            fRs = toFloat(iRs);
            iXs = Buf2Short(rcv, offset+2);
            fXs = toFloat(iXs);
            data[0] = new MeasureDataBin(0, freq, fRs, fXs);
            offset += 4;

            iRs = Buf2Short(rcv, offset);
            fRs = toFloat(iRs);
            iXs = Buf2Short(rcv, offset+2);
            fXs = toFloat(iXs);
            data[1] = new MeasureDataBin(0, freq+(1.0f*step), fRs, fXs);
            offset += 4;

            iRs = Buf2Short(rcv, offset);
            fRs = toFloat(iRs);
            iXs = Buf2Short(rcv, offset+2);
            fXs = toFloat(iXs);
            data[2] = new MeasureDataBin(0, freq+(2.0f*step), fRs, fXs);
            offset += 4;

            iRs = Buf2Short(rcv, offset);
            fRs = toFloat(iRs);
            iXs = Buf2Short(rcv, offset+2);
            fXs = toFloat(iXs);
            data[3] = new MeasureDataBin(0, freq+(3.0f*step), fRs, fXs);

            return data;
        }
        else
            return null;
    }
    /**
     * Takes measurement samples at four frequencies for fastest sweep speed.
     * Precision is a bit compromised because the use of half-float
     *
     * @param freq      initial frequency in MHz; use 0 to turn-off the generator
     * @param step      step frequency in MHz
     * @return data (four); or null if error
     */
    public MeasureDataBin[] MeasureCmdExt(float freq, float step) {
        return MeasureCmdExt(freq, step, (byte) 0);
    }

    /**
     * Enables signal generator.
     *
     * @param freq  frequency in MHz; use 0 to turn-off the generator
     * @param level output level
     * @param gain  gain multiplier
     * @return <0 error; otherwise ok
     */
    public int SignalGenCmd(float freq, int level, byte gain) {
        int status;

        byte snd[] = new byte[COMMAND_LEN];
        byte rcv[] = new byte[COMMAND_LEN];

        snd[0] = CMD_SARK_SIGNAL_GEN;
        System.arraycopy( Int2Buf ((int)(freq*1000000)), 0, snd, 1, 4 );
        System.arraycopy(Short2Buf(level), 0, snd,5, 2);
        snd[7] = gain;
        status = SendRcv(snd, rcv);
        if (rcv[0] != ANS_SARK_OK)
            status = -1;
        return status;
    }

    /**
     * Resets the sark110
     *
     * @return <0 error; otherwise ok
     */
    public int ResetCmd() {
        int status;

        byte snd[] = new byte[COMMAND_LEN];
        byte rcv[] = new byte[COMMAND_LEN];
        snd[0] = CMD_DEV_RST;
        status = SendRcv(snd, rcv);
        if (rcv[0] != ANS_SARK_OK)
            status = -1;
        return status;
    }

    /**
     * Raw detectors measurement
     *
     * @param freq frequency in MHz; use 0 to turn-off the generator
     * @return detectors data; or null if error
     */
    public MeasureDetector MeasDetectorCmd(float freq) {
        int status;

        byte snd[] = new byte[COMMAND_LEN];
        byte rcv[] = new byte[COMMAND_LEN];

        snd[0] = CMD_SARK_MEAS_VECTOR;
        System.arraycopy( Int2Buf ((int)(freq*1000000)), 0, snd, 1, 4 );
        status = SendRcv(snd, rcv);
        if (rcv[0] != ANS_SARK_OK)
            status = -1;
        if (status >= 0) {
            float magV = Buf2Float(rcv, 1);
            float phV = Buf2Float(rcv, 5);
            float magI = Buf2Float(rcv, 9);
            float phI = Buf2Float(rcv, 13);
            return new MeasureDetector(freq, magV, phV, magI, phI);
        }
        else
            return null;
    }

    /**
     * External RF signal measurement
     *
     * @param freq frequency in MHz; use 0 to turn-off the generator
     * @return detectors data; or null if error
     */
    public MeasureDetector MeasRfCmd (float freq) {
        int status;

        byte snd[] = new byte[COMMAND_LEN];
        byte rcv[] = new byte[COMMAND_LEN];

        snd[0] = CMD_SARK_MEAS_RF;
        System.arraycopy( Int2Buf ((int)(freq*1000000)), 0, snd, 1, 4 );
        status = SendRcv(snd, rcv);
        if (rcv[0] != ANS_SARK_OK)
            status = -1;
        if (status >= 0) {
            float magV = Buf2Float(rcv, 1);
            float phV = Buf2Float(rcv, 5);
            float magI = Buf2Float(rcv, 9);
            float phI = Buf2Float(rcv, 13);
            return new MeasureDetector(freq, magV, phV, magI, phI);
        }
        else
            return null;
    }

    /*
     * private methods
     */
    private byte[] Int2Buf(int val) {
        byte[] buf = new byte[4];

        buf[3] = (byte)((val&0xff000000)>>24);
        buf[2] = (byte)((val&0x00ff0000)>>16);
        buf[1] = (byte)((val&0x0000ff00)>>8);
        buf[0] = (byte)((val&0x000000ff)>>0);

        return buf;
    }

    private byte[] Short2Buf(int val) {
        byte[] buf = new byte[2];

        buf[1] = (byte)((val&0x0000ff00)>>8);
        buf[0] = (byte)((val&0x000000ff)>>0);

        return buf;
    }

    private int Buf2Short (byte buf[], int n) {
        long val;
        byte[] bufShort = new byte[2];

        System.arraycopy(buf, n, bufShort, 0, 2);
        val = (bufShort[1] << 8) & 0xff00L;
        val += ((bufShort[0] << 0) & 0xffL);

        return (int)val;
    }

    private float Buf2Float (byte buf[], int n) {
        byte[] bufFloat = new byte[4];
        System.arraycopy(buf, n, bufFloat, 0, 4);
        return ByteBuffer.wrap(bufFloat).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    /*
     * "Half-float" conversion function
     */
    private static final int FP16_SIGN_MASK         = 0x8000;
    private static final int FP16_EXPONENT_MASK     = 0x1f;
    private static final int FP16_EXPONENT_SHIFT    = 10;
    private static final int FP16_SIGNIFICAND_MASK  = 0x3ff;
    private static final int FP32_DENORMAL_MAGIC = 126 << 23;
    private static final float FP32_DENORMAL_FLOAT = Float.intBitsToFloat(FP32_DENORMAL_MAGIC);
    private static final int FP16_EXPONENT_BIAS     = 15;
    private static final int FP32_EXPONENT_BIAS     = 127;
    private static final int FP32_EXPONENT_SHIFT    = 23;

    public  float toFloat(int h) {
        int bits = h & 0xffff;
        int s = bits & FP16_SIGN_MASK;
        int e = (bits >>> FP16_EXPONENT_SHIFT) & FP16_EXPONENT_MASK;
        int m = (bits) & FP16_SIGNIFICAND_MASK;
        int outE = 0;
        int outM = 0;
        if (e == 0) { // Denormal or 0
            if (m != 0) {
                // Convert denorm fp16 into normalized fp32
                float o = Float.intBitsToFloat(FP32_DENORMAL_MAGIC + m);
                o -= FP32_DENORMAL_FLOAT;
                return s == 0 ? o : -o;
            }
        } else {
            outM = m << 13;
            if (e == 0x1f) { // Infinite or NaN
                outE = 0xff;
            } else {
                outE = e - FP16_EXPONENT_BIAS + FP32_EXPONENT_BIAS;
            }
        }
        int out = (s << 16) | (outE << FP32_EXPONENT_SHIFT) | outM;
        return Float.intBitsToFloat(out);
    }
}
