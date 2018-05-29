# Sark110_android_template
Android template application for the SARK-110 Antenna Analyzer

## About
This template application includes the interface classes for communicating with the SARK-110 via USB or Bluetooth LE (future model) and includes a basic example.

## Pre-requisites
- Android Studio
- Android tablet or smartphone with USB host support 
- USB OTG cable
- SARK-110

## Usage
The template application is ready to go and it implements the basic functionality.
If the application is created from scratch, follow the steps below:

### Manifest and resource files
Add the following permissions to the manifest file:

```XML
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

Add the following intent files to the activity section of the manifest file:
```XML
    <intent-filter>
	<action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
    </intent-filter>
    <intent-filter>
	<action android:name="android.hardware.usb.action.USB_DEVICE_DETACHED" />
    </intent-filter>
    <meta-data  android:name=
	"android.hardware.usb.action.USB_DEVICE_ATTACHED"
	android:resource="@xml/device_filter" />
    <meta-data android:name=
	"android.hardware.usb.action.USB_DEVICE_DETACHED"
	android:resource="@xml/device_filter" />
```
The following resource file should be saved in res/xml/device_filter.xml:
```XML
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- SARK-110 -->
    <usb-device vendor-id="0483" product-id="5750" />
</resources>
```
It specifies the VID and PID for the SARK-110.

### Add the classes for communicating to the SARK-110

- DeviceIntf.java
- USBIntf.java
- BluetoothLEIntf.java
- ComplexNumber.java
- MeasureDataBin.java

### Add the code to the Activity 
Define a member variable of DeviceIntf class
```Java
    private DeviceIntf mDevIntf;
```

In onCreate() method of the activity, create an instance of the USBIntf class (for USB communications) or BluetoothLEIntf class (for Bluetooth LE communications; future device).
Then call method onCreate() and then setup a listener for connection events.
```Java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	...
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
```
Call method onResume() from activity onResume().
```Java
    @Override
    protected void onResume() {
	...
        mDevIntf.onResume();
    }
```

With the connection established, use any of the provided methods for communicating with the SARK-110. See the example below: 
```Java
    private void TestSark () {
        TextView text = findViewById(R.id.terminal);
        mDevIntf.BeepCmd();	// Beeps the SARK-110 buzzer
        mDevIntf.VersionCmd();	// Gets the SARK-110 version: use getSarkVer() and getProtocolVer()
        text.setText(
                "Version: " + new String(mDevIntf.getSarkVer()) + " Protocol: "  + String.valueOf(mDevIntf.getProtocolVer()) + "\n"
        );
        text.append("\n* Measurements: *\n");
        for (int i = 1; i <10; i++)
        {			// Perform measurement at a frequency; obtain the different parameters using MeasureDataBin class methods
            MeasureDataBin bin = mDevIntf.MeasureCmd(10.0f + (float)(i));
            text.append("Frequency: " + (10.0f+i) + " VSWR: " + bin.getVswr() + " Rs:" + bin.getRs() + " Xs: " + bin.getXs() + "\n");
        }
        text.append("* The end *\n");
    }
```

### Considerations for Bluetooth
The analyzer will need to be bonded to the Android device in advance. Use the Bluetooth setup in the Android device to bond the analyzer.

## License
Copyright (c) 2018 Melchor Varela - EA4FRB

Licensed under the MIT License (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

	[MIT License](https://opensource.org/licenses/MIT)
	