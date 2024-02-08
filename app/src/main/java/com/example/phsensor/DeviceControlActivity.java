package com.example.phsensor;

import static android.nfc.NfcAdapter.EXTRA_DATA;
import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DeviceControlActivity extends AppCompatActivity {
    private BluetoothLEService mBluetoothService;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean connected;
    private Button button_connect;
    public static String SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    public static String CHARACTERISTIC_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    private Handler mHandler = new Handler();
    private TextView mpHValue;
    private TextView mtempValue;
    private TextView msalinityValue;
    private TextView dateTimeTextView;
    private String formattedDate;
    //private BluetoothGattCharacteristic target_chara;

    //private BluetoothGattCharacteristic readCharacteristic;
    //private BluetoothGattService readMnotyGattService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        requestPermission();


        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);


        // Sets up UI references.
        ((TextView) findViewById(R.id.device_name)).setText(mDeviceName);

        //TextView mConnectionState = (TextView) findViewById(R.id.connection_state);
        mpHValue = (TextView) findViewById(R.id.pHValue);
        mtempValue = (TextView) findViewById(R.id.tempValue);
        msalinityValue = (TextView) findViewById(R.id.salinityValue);


        // Display the formatted date and time in a TextView
        dateTimeTextView = findViewById(R.id.time);


        button_connect = (Button) findViewById(R.id.button_connect);
        button_connect.setOnClickListener(this::onClick);
        //edittext_input_value = (EditText) findViewById(R.id.edittext_input_value);
        Intent gattServiceIntent = new Intent(this, BluetoothLEService.class);
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    private void onClick(View v){

        if(!connected) {
            boolean status = mBluetoothService.connect(mDeviceAddress);
            Log.e(TAG, "!!!!!Click on connect:" + status);
        }
        else {
            mBluetoothService.disconnect();
            Log.e(TAG, "!!!!!Click on disconnect");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        /*if (mBluetoothService != null) {
            final boolean result = mBluetoothService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }
    public void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
       if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
               != PackageManager.PERMISSION_GRANTED) {
           ActivityCompat.requestPermissions(this,
                   new String[]{android.Manifest.permission.BLUETOOTH_ADVERTISE}, 1);
       }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothService = ((BluetoothLEService.LocalBinder) service).getService();
            if (mBluetoothService == null || !mBluetoothService.initialize()) {
                Log.e(TAG, "!!!!!Unable to initialize Bluetooth");
                finish();
            }
            // call functions on service to check connection and connect to devices
            //mBluetoothService.connect(mDeviceAddress);
            Log.e(TAG, "!!!!!Connecting Bluetooth Service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothService = null;
            Log.e(TAG, "!!!!!Service Disconnected");
        }
    };

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLEService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                button_connect.setText("disconnect");
               // updateConnectionState(R.string.connected);
            } else if (BluetoothLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                button_connect.setText("connect");
                //updateConnectionState(R.string.disconnected);
            } else if (BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothService.getSupportedGattServices());
                //读数据的服务和characteristic
                //readMnotyGattService = mBluetoothService.getSupportedGattServices(UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"));
                //readCharacteristic = readMnotyGattService.getCharacteristic(UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"));
                Log.e(TAG, "!!!!!Service Discovered and read data");
                /*Log.e(TAG, "!!!!!char "+readCharacteristic.getUuid());
                for (BluetoothGattDescriptor descr : readCharacteristic.getDescriptors()) {
                    Log.d(TAG, "!!!!Desctr " + descr.getUuid());
                }*/
            } else if (BluetoothLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.i(TAG, "!!!!onReceive: data available");
                displayData(intent.getExtras().getString("BLE_BYTE_String"), intent);
           }
        }
    };

    public void displayData(String rev_string, Intent intent) {
        byte[] data = intent.getByteArrayExtra("BLE_BYTE_DATA");
        if (data == null) {
            Log.i(TAG, "displayData: data is empty");
            return;
        }
        Log.e(TAG, "!!!!!display:"+rev_string);

        double ph = (data[0] & 0xFF) + (data[1] & 0xFF) * 0.01;
        double temp = (data[2] & 0xFF) + (data[3] & 0xFF) * 0.01;
        double salinity = (data[4] & 0xFF) + (data[5] & 0xFF) * 0.01;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mpHValue.setText(String.valueOf(ph));
                mtempValue.setText(String.valueOf(temp));
                msalinityValue.setText(String.valueOf(salinity));
                Calendar calendar = Calendar.getInstance();
                Date currentDate = calendar.getTime();

                // Format the date and time
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                formattedDate = dateFormat.format(currentDate);
                dateTimeTextView.setText(formattedDate);
            }
        });
    }



    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLEService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    // Demonstrates how to iterate through the supported GATT
    // Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the
    // ExpandableListView on the UI.


    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        Log.e(TAG, "!!!!!display Gatt Services");
        for (BluetoothGattService service : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = service.getCharacteristics();
            for (final BluetoothGattCharacteristic characteristic : gattCharacteristics) {
                if (characteristic.getUuid().toString().equals(CHARACTERISTIC_UUID)) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBluetoothService.readCharacteristic(characteristic);
                        }
                    }, 200);
                    mBluetoothService.setCharacteristicNotification(characteristic, true);
                    //target_chara = characteristic;
                }
            }
        }

    }

}