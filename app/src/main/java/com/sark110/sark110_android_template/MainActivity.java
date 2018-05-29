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

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private DeviceIntf mDevIntf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "SARK110 Test", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                TestSark();
            }
        });

        /* SARK110 */
        //mDevIntf = new BluetoothLEIntf(this);     // (future device with LE support)
        mDevIntf = new USBIntf(this);
		mDevIntf.onCreate();
		// Setup listener for connection events from the device
		mDevIntf.setDeviceIntfListener(new DeviceIntf.DeviceIntfListener() {
			@Override
			public void onConnectionStateChanged(DeviceIntf helper, final boolean isConnected) {
			TextView text = findViewById(R.id.connect_stat);
			if (isConnected)
				text.setText("Connected");
			else {
				text.setText("Disconnected");
			}
			}
        });
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void TestSark () {
        TextView text = findViewById(R.id.terminal);
        mDevIntf.BeepCmd();     // Beeps the SARK-110 buzzer
        mDevIntf.VersionCmd();  // Gets the SARK-110 version: use getSarkVer() and getProtocolVer()
        text.setText(
                "Version: " + new String(mDevIntf.getSarkVer()) + " Protocol: "  + String.valueOf(mDevIntf.getProtocolVer()) + "\n"
        );
        text.append("\n* Measurements: *\n");
        for (int i = 1; i <10; i++)
        {   // Perform measurement at a frequency; obtain the different parameters using MeasureDataBin class methods
            MeasureDataBin bin = mDevIntf.MeasureCmd(10.0f + (float)(i));
            text.append("Frequency: " + (10.0f+i) + " VSWR: " + bin.getVswr() + " Rs:" + bin.getRs() + " Xs: " + bin.getXs() + "\n");
        }
        text.append("* The end *\n");
    }

}
