package com.example.android_ble;

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
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.UUID;

public class BluetoothLeService extends Service {

    //Binder
    private final IBinder mBinder = new LocalBinder();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    //Socket
    private String mBluetoothDeviceAddress;
    private BluetoothGatt BLE_Socket;

    //기기연결 상태
    private int mConnectionState = STATE_DISCONNECTED;

    //상태 코드
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;


    // Service Action
    //기기연결이 성공했을때
    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    //기기가 연결되어있지 않을때
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    //서비스를 찾았을때 들어옴
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    //값을 찾았을때 들어옴
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";

    //변경이 필요한 부분!!!
    //UUID 지정(기기 UUID)
    public final static UUID BLE_UUID = UUID.fromString("19b10000-e8f2-537e-4f6c-d104768a1214");
    //UUID BLE_CLIENT_CHARACTERISTIC(통신 UUID)
    public final static UUID BLE_CLIENT_CHARACTERISTIC_READ = UUID.fromString("19b10000-e8f2-537e-4f6c-d104768a1214");

    public final static UUID BLE_CLIENT_CHARACTERISTIC_WRITE = UUID.fromString("19b10001-e8f2-537e-4f6c-d104768a1214");


    //BluetoothLeService 초기화
    public boolean initialize() {
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }


    //Binder : 서비스의 기능을 이용할 수 있도록 제공하는 것
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @SuppressLint("MissingPermission")
    public void close() {
        if (BLE_Socket == null) {
            return;
        }
        BLE_Socket.close();
        BLE_Socket = null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        MainActivity.BTstate = false;
        close();
        return super.onUnbind(intent);
    }

    // 연결(변경, 검색된 서비스) GATT 이벤트에 관한 callBack
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String bluetooth_success;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bluetooth_success = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED; //의미없음
                broadcastUpdate(bluetooth_success); //broadcastUpdate함수가 잇음
                Log.i("BluetoothGattCallback", "Attempting to start service discovery:" + BLE_Socket.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                bluetooth_success = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED; //의미없음 왜 카운터 하는지 모름
                Log.i("BluetoothGattCallback", "Disconnected from GATT server.");
                broadcastUpdate(bluetooth_success);
            }
        }

        //새로운 서비스가 발견되었을때 호출되는 콜백
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.w("onServicesDiscovered", "onServicesDiscovered received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("onServicesDiscovered", "SUCCESS");
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w("onServicesDiscovered", "Not SUCCESS");
            }
        }

        //최초 연결시 1번만 empty값 가지고 실행됨
        //특성 읽기 작업의 결과를 보고하는 콜백
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.w("onCharacteristicRead", String.valueOf(TextUtil.getInstance().getDecimal(TextUtil.getInstance().bytesToHex(characteristic.getValue()))));
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        //원격 특성 알림의 결과로 트리거된 콜백입니다.
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    //블루투스 연결할때 여기로 들어옴
    private void broadcastUpdate(String _bluetooth_success) {
        final Intent intent = new Intent(_bluetooth_success);
        sendBroadcast(intent);
    }

    //데이터를 읽을때 여기로 들어옴
    private void broadcastUpdate(String action, BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
//        Log.w("characteristic.getUuid()", characteristic.getUuid().toString());
        if (BLE_UUID.equals(characteristic.getUuid())) {
            final int heartRate = TextUtil.getInstance().getDecimal(TextUtil.getInstance().bytesToHex(characteristic.getValue()));
//            Log.w("BLE_Service_if", String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else  //거의 else만 실행됨 if 왜 있는지 ..? 일단 여기로 들어옴
        {
            final int heartRate = TextUtil.getInstance().getDecimal(TextUtil.getInstance().bytesToHex(characteristic.getValue()));
//            Log.w("BLE_Service_else", String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        }
        sendBroadcast(intent);
    }


    // 기기 연결
    @SuppressLint("MissingPermission")
    public boolean connect(final String address) {
        //블루투스 어뎁터, MAC주소 없을때
        if (mBluetoothAdapter == null || address == null) {
            return false;
        }

        // 재연결 : 연결이 잠시 끊어져서 MAC주소를 아직 가지고 있을때
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && BLE_Socket != null) {
            if (BLE_Socket.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        //블루투스 어뎁터에서 소켓 얻기(처음으로 소켓 생성함)
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            return false;
        }

        //블루투스 어뎁터에서 진화형소켓 얻기(Gatt Socket)
        //상태 변경
        BLE_Socket = device.connectGatt(this, false, mGattCallback); //소켓(device)에서 bluetoohGatt
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    //연결 끊겼을때
    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (mBluetoothAdapter == null || BLE_Socket == null) {
            return;
        }
        BLE_Socket.disconnect();
    }

    //////////////////////블루투스 통신 (읽고, 쓰기)  Read Write  /////////////////////////////
    protected static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || BLE_Socket == null) {
            Log.w("BLE_Service", "BluetoothAdapter not initialized");
            return;
        }

        BLE_Socket.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID); //characteristic을 찾기, 보여주기 등을 담당

        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        //연결된 원격 장치에 주어진 설명자의 값을 씁니다.
        BLE_Socket.writeDescriptor(descriptor);
    }

    //////////////////////////////// Read 데이터 수신 //////////////////////////////////
    public BluetoothGattCharacteristic getSupportedGattServices_read() //변경
    {
        if (BLE_Socket == null) {
            return null;
        }
        BluetoothGattService BTservice = BLE_Socket.getService(BLE_UUID);
        if (BTservice == null) {
            return null;
        }

        return BTservice.getCharacteristic(BLE_CLIENT_CHARACTERISTIC_READ);
    }

    @SuppressLint("MissingPermission")
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || BLE_Socket == null) {
            Log.w("BLE_Service", "BluetoothAdapter not initialized");
            return;
        }
        //여기로 들어옴 읽을때(여기서 characteristic 값이 들어옴)
        BLE_Socket.readCharacteristic(characteristic);
    }

    //////////////////////////////// Write 데이터 송신 //////////////////////////////////
    public BluetoothGattCharacteristic getSupportedGattServices_write() {
        if (BLE_Socket == null) {
            return null;
        }
        BluetoothGattService BTservice = BLE_Socket.getService(BLE_UUID);

        if (BTservice == null) {
            return null;
        }

        BluetoothGattCharacteristic BTcharateristics = BTservice.getCharacteristic(BLE_CLIENT_CHARACTERISTIC_WRITE);
        return BTcharateristics;
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
        for (byte b : data)
            Log.w("writeCharacteristic", String.valueOf(b));
        if (mBluetoothAdapter == null || BLE_Socket == null) {
            return;
        }
        if (characteristic == null) {
            return;
        }

        characteristic.setValue(data);
        @SuppressLint("MissingPermission") boolean a = BLE_Socket.writeCharacteristic(characteristic);
    }
}