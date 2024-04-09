package com.example.phsensor;

import static android.nfc.NfcAdapter.EXTRA_DATA;
import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.util.List;
import java.util.UUID;

public class BluetoothLEService extends Service {
    public static final String TAG = "BluetoothLeService";
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private String mBluetoothAddress;
    private BluetoothGatt mBluetoothGatt;
    private Binder mBinder = new LocalBinder();


    public int mConnectionState = STATE_DISCONNECTED;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";

    class pHValue {
        float ph;
        float temp;
        float light;
    }

    class LocalBinder extends Binder {
        public BluetoothLEService getService() {
            return BluetoothLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return mBinder;
    }

    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }


    public boolean initialize() {
        mBluetoothManager = getSystemService(BluetoothManager.class);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "!!!!!Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public boolean connect(String deviceAddress) {

        if (mBluetoothAdapter == null || deviceAddress == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            // return false;
        }
        if (mBluetoothAddress != null && mBluetoothGatt != null && mBluetoothAddress.equals(deviceAddress)) {
            Log.i(TAG, "connect: Trying to use an existing mBluetoothGatt for connection");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        if (device == null) {
            Log.i(TAG, "Device not found with provided address.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, false, gattCallback);
        Log.i(TAG, "connect: Trying to create a connection");
        mBluetoothAddress = deviceAddress;
        mConnectionState = STATE_CONNECTING;
        return true;

        /*try {
            final BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
            // connect to the GATT server on the device
            mBluetoothGatt = device.connectGatt(this, false, gattCallback);
            // return true;
        } catch (IllegalArgumentException exception) {
            Log.w(TAG, "Device not found with provided address.  Unable to connect.");
            // return false;
        }*/
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "disconnect: BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    @SuppressLint("MissingPermission")
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            String intentAction;
            //连接状态回调方法
            if (newState == BluetoothGatt.STATE_CONNECTED) {//已连接
                // gatt.discoverServices();//发现远程设备提供的服务及其特征和描述符，成功后会回调 onServicesDiscovered 方法
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "!!!!Connected to GATT server.");
                Log.i(TAG, "!!!!Attempting to start service discovery:" + gatt.discoverServices());
                //                if (this.bleConnectOrSyncCallback != null)
                //                    this.bleConnectOrSyncCallback.connectOrSyncStatus(gatt.STATE_CONNECTED, gatt);
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {//已断开连接
                //            gatt.close();//关闭gatt （这里不关闭，否则重连调用bluetoothGatt.connect()无效，当手动解绑(断连)再gatt.close()）
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "!!!!Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                Log.d(TAG, "!!!!service Discovered");
                /*for (BluetoothGattService service : gatt.getServices()) {
                    Log.d(TAG, "!!!!service" + service.getUuid());
                    //遍历Characteristic
                    for (BluetoothGattCharacteristic charac : service.getCharacteristics()) {
                        Log.d(TAG, "!!!!charac " + charac.getUuid());
                        for (BluetoothGattDescriptor descr : charac.getDescriptors()) {
                            //将获取到的characteristic存在列表里面
                            Log.d(TAG, "!!!!descr " + descr.getUuid());
                        }
                        //listcharac.add(charac);
                    }
                }*/
            } else {
                Log.w(TAG, "!!!!onServicesDiscovered received: " + status);
                System.out.println("onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                Log.w(TAG, "!!!!Successful read");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                Log.w(TAG, "!!!!Successful write");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            Log.w(TAG, "!!!!Characteristic changed");
        }

    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
        Log.w(TAG, "!!!!broadcast Update");
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        byte[] data = characteristic.getValue();

        Log.w(TAG, "!!!!broadcast Update characteristic");
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data) {
                int unsignedVal = byteChar & 0xFF;
                stringBuilder.append(String.format("%02X", unsignedVal));
                Log.i(TAG, "!!!!broadcastUpdate: byteChar is:" + unsignedVal);
            }
            Log.i(TAG, "!!!!broadcastUpdate: string is:" + stringBuilder);
            intent.putExtra("BLE_BYTE_DATA", data);
            intent.putExtra("BLE_BYTE_String", new String(data));
            writeCharacteristic(characteristic);
        }
        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is carried out as per profile specifications.
        /*if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
                        stringBuilder.toString());
            }
        }*/
        sendBroadcast(intent);
    }

    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }

        boolean isEnableNotification = mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        Log.w(TAG, "!!!!set Characteristic Notification");
        if(isEnableNotification) {
            List<BluetoothGattDescriptor> descriptorList = characteristic.getDescriptors();
            if(descriptorList != null && descriptorList.size() > 0) {
                for(BluetoothGattDescriptor descriptor : descriptorList) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                }
            }
        }
        /*BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("6e402902-b5a3-f393-e0a9-e50e24dcca9e"));
        if (enabled) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        mBluetoothGatt.writeDescriptor(descriptor);*/
        /*// This is specific to Heart Rate Measurement.
        if (characteristic.getUuid().equals("6e400003-b5a3-f393-e0a9-e50e24dcca9e")) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("6e402092-b5a3-f393-e0a9-e50e24dcca9e"));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }*/
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }

    /*public BluetoothGattService getSupportedGattServices(UUID uuid) {
        BluetoothGattService mBluetoothGattService;
        if (mBluetoothGatt == null) return null;
        mBluetoothGattService = mBluetoothGatt.getService(uuid);
        Log.w(TAG, "!!!!get Supported GattServices");
        return mBluetoothGattService;
    }*/

    @SuppressLint("MissingPermission")
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }
        Log.w(TAG, "!!!!read Characteristic");
        mBluetoothGatt.readCharacteristic(characteristic);
    }
    @SuppressLint("MissingPermission")
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        byte[] value = new byte[] {0x01, 0x02};
        characteristic.setValue(value);
        mBluetoothGatt.writeCharacteristic(characteristic);
        Log.w(TAG, "!!!!write Characteristic");
    }
}