package com.sark110.sark110_android_template;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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
    private boolean mIsBluetooth = false;
    private DeviceIntf mDevIntf;
    private float mFreq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.default_preferences, false);
        setContentView(R.layout.activity_main);
        /* Get stored preferences */
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /* SARK110 */
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

        final EditText etFreq = findViewById(R.id.frequency_entry);
        mFreq = etFreq.getText().toString().isEmpty()?GblDefs.DEF_FREQ_START:Float.valueOf(etFreq.getText().toString());
        /* Validation */
        etFreq.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {
                if( etFreq.getText().toString().length() == 0 )
                    etFreq.setError("Empty value");
                else if (Float.valueOf(etFreq.getText().toString()) < GblDefs.MIN_FREQ)
                    etFreq.setError("Below min");
                else if (Float.valueOf(etFreq.getText().toString()) > GblDefs.MAX_FREQ)
                    etFreq.setError("Above max");
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
                MeasureDataBin bin = mDevIntf.MeasureCmd(mFreq/1000000);
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
