package com.example.phsensor;

import static android.nfc.NfcAdapter.EXTRA_DATA;
import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceControlActivity extends AppCompatActivity implements LocationListener {
    private BluetoothLEService mBluetoothService;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean connected;
    private Button button_connect;
    private Button button_send;
    public static String SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    public static String CHARACTERISTIC_RX_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static String CHARACTERISTIC_TX_UUID = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    BluetoothGattCharacteristic characteristicTx;
    private Handler mHandler = new Handler();
    private TextView mpHValue;
    private TextView mtempValue;
    private TextView msalinityValue;
    private TextView dateTimeTextView;
    private TextView locationTextView;
    private String formattedDate;
    private LocationManager locationManager;
    private double latitude;
    private double longitude;
    private static String url = "https://example.com/api";
    private static String postData = "key1=value1&key2=value2";
    private DecimalFormat decimalFormat;
    private EditText mAddInfo;
    //private BluetoothGattCharacteristic target_chara;

    //private BluetoothGattCharacteristic readCharacteristic;
    //private BluetoothGattService readMnotyGattService;
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        requestPermission();
        // Initialize location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

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

        decimalFormat = new DecimalFormat("0.00000");
        locationTextView = findViewById(R.id.location);
        mAddInfo = findViewById(R.id.Information);

        button_connect = (Button) findViewById(R.id.button_connect);
        button_connect.setOnClickListener(this::onClick);

        button_send = (Button) findViewById(R.id.button_send);
        button_send.setOnClickListener(this::onClick);

        CheckBox checkboxOption1 = findViewById(R.id.checkbox_option1);
        CheckBox checkboxOption2 = findViewById(R.id.checkbox_option2);

       // 监听CheckBox状态变化
        checkboxOption1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 处理选项1的选择状态
                if (isChecked) {
                    // 选项1被选中
                } else {
                    // 选项1被取消选中
                }
            }
        });

        checkboxOption2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 处理选项2的选择状态
                if (isChecked) {
                    // 选项2被选中
                } else {
                    // 选项2被取消选中
                }
            }
        });


       // Request location updates
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
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
        String addInfo = mAddInfo.getText().toString();

        DataType data = new DataType();
        GetRequest_Interface apiService = RetrofitClient.getApiService();

        Call<DataType> call = apiService.createUser(data);
        call.enqueue(new Callback<DataType>() {
            @Override
            public void onResponse(Call<DataType> call, Response<DataType> response) {
                if (response.isSuccessful()) {
                    // Handle success
                    Log.d("POST SUCCESS", "Successfully posted data");
                } else {
                    // Handle the error
                    Log.d("POST ERROR", "Failed to post data");
                }
            }

            @Override
            public void onFailure(Call<DataType> call, Throwable t) {
                // Handle failure
                Log.d("POST FAILURE", "Network or conversion error");
            }
        });



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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions if not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);

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
                //replyData(intent);
                mBluetoothService.writeCharacteristic(characteristicTx);
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
        Log.e(TAG, "!!!!!display:"+ rev_string);

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
                String formattedLocation = "Latitude: " + decimalFormat.format(latitude) + "\n"
                        + "Longitude: " + decimalFormat.format(longitude);
                locationTextView.setText(formattedLocation);

            }
        });
    }


    @Override
    public void onLocationChanged(@NonNull Location location) {
        // Handle location updates here
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        // Do something with the latitude and longitude values
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Handle permission request result
        if (requestCode == 1 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            // Start requesting location updates if permissions granted
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop location updates when activity is destroyed
        locationManager.removeUpdates(this);
    }

    // Implement other methods of the LocationListener interface as needed
    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

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
                if (characteristic.getUuid().toString().equals(CHARACTERISTIC_RX_UUID)) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBluetoothService.readCharacteristic(characteristic);
                        }
                    }, 200);
                    mBluetoothService.setCharacteristicNotification(characteristic, true);
                    //target_chara = characteristic;
                }
                else if (characteristic.getUuid().toString().equals(CHARACTERISTIC_TX_UUID)) {
                    characteristicTx =  characteristic;

                    mBluetoothService.setCharacteristicNotification(characteristic, true);
                    //target_chara = characteristic;
                }
            }
        }

    }

}