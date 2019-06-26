package com.sark110.sark110_android_template;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.alexzaitsev.meternumberpicker.MeterView;

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
public class MainActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "Sark110PrefsFile";
    private boolean mIsBluetooth = false;
    private DeviceIntf mDevIntf;
    private MeterView mFreqPicker;
    private int mLastFreq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.default_preferences, false);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFreqPicker = (MeterView) findViewById(R.id.freqEntry);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        mLastFreq = prefs.getInt("pref_freq", GblDefs.DEF_FREQ_START);
        mFreqPicker.setValue(mLastFreq);

        /* SARK110 */
        /* Get stored preferences */
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mIsBluetooth = prefs.getBoolean("pref_Bluetooth", false);
        mDevIntf = new BluetoothLEIntf(this);     // Create instance: Bluetooth option (future device with LE support)
        if (mIsBluetooth)
            mDevIntf = new BluetoothLEIntf(this);
        else
            mDevIntf = new USBIntf(this);
		mDevIntf.onCreate();
		// Setup listener for connection events from the device
        mDevIntf.setDeviceIntfListener(new DeviceIntf.DeviceIntfListener() {
            @Override
            public void onConnectionStateChanged(DeviceIntf deviceIntf, final boolean isConnected) {
                TextView textConn = findViewById(R.id.connect_stat);
                TextView textVer = findViewById(R.id.status);
                if (isConnected) {
                    textConn.setText("Connected");
                    mDevIntf.BeepCmd();     // Beeps the SARK-110 buzzer
                    mDevIntf.VersionCmd();  // Gets the SARK-110 version: use getSarkVer() and getProtocolVer()
                    textVer.setText("Version: " + new String(mDevIntf.getSarkVer()) + " Protocol: " + String.valueOf(mDevIntf.getProtocolVer()));
                }
                else {
                    textConn.setText("Disconnected");
                    textVer.setText("");
                }
            }
        });

        // Create the Handler object (on the main thread by default)
        final Handler handler = new Handler();
        // Define the code block to be executed
        Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                if (!mDevIntf.isConnected())
                    mDevIntf.connect();
                int freq = mFreqPicker.getValue();
                if (freq < GblDefs.MIN_FREQ) {
                    freq = mLastFreq;
                    mFreqPicker.setValue(freq);
                }
                else if (freq > GblDefs.MAX_FREQ) {
                    freq = mLastFreq;
                    mFreqPicker.setValue(freq);
                }
                else {
                    if (freq != mLastFreq) {
                        SharedPreferences.Editor edit = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                        edit.putInt("pref_freq", freq);
                        edit.apply();
                    }
                    mLastFreq = freq;
                }
                MeasureDataBin bin = mDevIntf.MeasureCmd(freq);
                TextView textSWR = findViewById(R.id.swr_val);
                TextView textZ = findViewById(R.id.impedance_val);
                if (bin != null) {
                    textSWR.setText("VSWR: " + String.format("%.2f", bin.getVswr()));
                    float xs = bin.getXs();
                    if (xs < 0)
                        textZ.setText("Z: " + String.format("%.2f", bin.getRs()) + " -j" + String.format("%.2f", Math.abs(xs)));
                    else
                        textZ.setText("Z: " + String.format("%.2f", bin.getRs()) + " +j" + String.format("%.2f", Math.abs(xs)));
                }
                else {
                    textSWR.setText("VSWR: ");
                    textZ.setText("Z: ");
                }

                // Repeat this the same runnable code block again another 1 second
                // 'this' is referencing the Runnable object
                handler.postDelayed(this, 1000);
            }
        };
        // Start the initial runnable task by posting through the handler
        handler.post(runnableCode);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /* SARK110 */
        mDevIntf.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingIntent =  new Intent(this, SettingsActivity.class);
            startActivity(settingIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
