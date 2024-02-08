package com.example.phsensor;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import static java.security.AccessController.getContext;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BLEActivity extends AppCompatActivity {
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothLeScanner mBluetoothLeScanner; //non-static;
    private boolean mIsScanning = false;
    private Handler mHandler;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private ListView deviceListView;
    //private ArrayAdapter<BluetoothDevice> deviceArrayAdapter;
    private MyAdapter deviceArrayAdapter;

    private BluetoothDevice mBluetoothDevice;
    private String mdeviceName;



    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleactivity);
        requestPermission();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            //Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "ble_not_supported");
            finish();
            return;
        }

        // mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //Deprecated
        mBluetoothManager = getSystemService(BluetoothManager.class);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Log.d(TAG, "ble_not_supported");
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        List<BluetoothDevice> mDevicesList = new ArrayList<>();
        deviceArrayAdapter = new MyAdapter(this, R.layout.list_item, mDevicesList);
       // deviceArrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1);
        deviceListView = findViewById(R.id.listview);
        deviceListView.setAdapter(deviceArrayAdapter);

        //可以用activityresult来实现
        deviceListView.setOnItemClickListener((parent, view, position, id) -> {

            BluetoothDevice selectedDevice = deviceArrayAdapter.getItem(position);
            if (selectedDevice != null) {
                //String[] parts = selectedDeviceString.split("\n");
                //String selectedDeviceAddress = parts[1];
                Intent itemClickIntent = null;
                itemClickIntent = new Intent(BLEActivity.this, DeviceControlActivity.class);
                itemClickIntent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, selectedDevice.getName());
                itemClickIntent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, selectedDevice.getAddress());

                //start itemclick之前停止扫描？
                startActivity(itemClickIntent);
                // Connect to the selected Bluetooth device
                //connectToDevice(selectedDeviceAddress);
            }
        });
        scanLeDevice();


    }

    @SuppressLint("MissingPermission")
    protected void onResume() {
        super.onResume();
        //有权限请求时会调用两次
    }

    public void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.BLUETOOTH_SCAN}, 1);
        }
       /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
               != PackageManager.PERMISSION_GRANTED) {
           ActivityCompat.requestPermissions(this,
                   new String[]{android.Manifest.permission.BLUETOOTH_ADVERTISE}, 1);
       }*/
       if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
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



    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth open", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Bluetooth cannot open", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }*/

    @SuppressLint("MissingPermission")
    private void scanLeDevice() {
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner(); //non-static;

        mHandler = new Handler();

        if (mBluetoothAdapter == null || mBluetoothLeScanner == null) {
            Log.d(TAG, "ble_not_supported");
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!mIsScanning) {
            // Stops scanning after a predefined scan period.
            mHandler.postDelayed(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    mIsScanning = false;
                    mBluetoothLeScanner.stopScan(mleScanCallback);
                    //mBluetoothAdapter.stopLeScan(mleScanCallback);
                    Log.d(TAG, "!!!!!!stop scaning!!!!!!!");
                }
            }, SCAN_PERIOD);
            mIsScanning = true;
            mBluetoothLeScanner.startScan(mleScanCallback);
            Log.d(TAG, "!!!!!!start scaning!!!!!!!");
            Toast.makeText(this, "scanning", Toast.LENGTH_SHORT).show();
        } else {
            mIsScanning = false;
            mBluetoothLeScanner.stopScan(mleScanCallback);
            Toast.makeText(this, "stopscaning", Toast.LENGTH_SHORT).show();

        }
    }
    // Device scan callback.
    private final ScanCallback mleScanCallback = new ScanCallback() {

        public void onScanResult(int callbackType, final ScanResult result) {//发现 BLE 广播时的回调
            //super.onScanResult(callbackType, result);
            runOnUiThread(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    mBluetoothDevice = result.getDevice();
                    mdeviceName = result.getDevice().getName();
                    //Log.d(TAG, "!!!!!!"+mdeviceName);
                    if(!containsDevice(mBluetoothDevice)) {
                        deviceArrayAdapter.add(mBluetoothDevice);
                        deviceArrayAdapter.notifyDataSetChanged();
                    }
                    //deviceArrayAdapter.add(mBluetoothDevice.getName() + "\n" + mBluetoothDevice.getAddress());
                    //LogUtil.i(TAG, "address: " + bluetoothDevice.getAddress());
                }

            });
        }
    };

    private boolean containsDevice(BluetoothDevice device) {
        for (int i = 0; i < deviceArrayAdapter.getCount(); i++) {
            BluetoothDevice existingDevice = deviceArrayAdapter.getItem(i);
            if (existingDevice != null && existingDevice.getAddress().equals(device.getAddress())) {
                // Device with the same address already exists in the adapter
                return true;
            }
        }
        return false;
    }


    private class MyAdapter extends ArrayAdapter<BluetoothDevice> {

        private Context context;
        private int itemResource;
        private List<BluetoothDevice> devicesList;
        public MyAdapter(Context context, int itemResource, List<BluetoothDevice> devicesList) {
            super(context, R.layout.activity_bleactivity, devicesList);
            this.context = context;
            this.devicesList = devicesList;
            this.itemResource = itemResource;
        }

        @SuppressLint("MissingPermission")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder viewHolder;
            View customView = convertView;
            if (customView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                customView = inflater.inflate(itemResource, parent, false);

                viewHolder = new ViewHolder();
                viewHolder.deviceName = customView.findViewById(R.id.item_name);
                viewHolder.deviceAddress = customView.findViewById(R.id.item_address);
                customView.setTag(viewHolder);

            } else {
                viewHolder = (ViewHolder) customView.getTag();
            }

            BluetoothDevice device = getItem(position);
            if (device.getName() != null) {
                viewHolder.deviceName.setText(device.getName());
            }
            else {
                viewHolder.deviceName.setText("Unknown_device");
            }
            viewHolder.deviceAddress.setText(device.getAddress());

            return customView;

        }

        public class ViewHolder {
            TextView deviceName;
            TextView deviceAddress;
        }
    }
        /*@SuppressLint("MissingPermission")
        private void connectToDevice(String deviceAddress) {
            // Implement your Bluetooth connection logic here
            // Use the deviceAddress to connect to the selected Bluetooth LE device
            // You may want to start a new activity or fragment to handle the connection details
            // Example:
            // Intent intent = new Intent(this, YourConnectionActivity.class);
            // intent.putExtra("DEVICE_ADDRESS", deviceAddress);
            // startActivity(intent);
            if (mBluetoothAdapter == null || deviceAddress == null) {
                Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
                // return false;
            }
            mBluetoothDevice.connect(deviceAddress);
            try {
                final BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                // connect to the GATT server on the device
                mBluetoothGatt = device.connectGatt(this, false, mBluetoothDevice.gattCallback);
                // return true;
            } catch (IllegalArgumentException exception) {
                Log.w(TAG, "Device not found with provided address.  Unable to connect.");
                // return false;
            }
            //BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
            //if (device != null) device.connectGatt(this, false, gattCallback);     //connectGatt
        }*/

    /*private class MyListener implements View.OnClickListener {
        int mPosition;
        public MyListener(int inPosition){
            mPosition= inPosition;
        }
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            Toast.makeText(ListViewActivity.this, title[mPosition], Toast.LENGTH_SHORT).show();
        }

    }*/

    //}

}




