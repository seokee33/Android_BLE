package com.example.android_ble;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

public class ScanDevice extends AppCompatActivity {

    //RecyclerView
    private RecyclerView rv_Scan;
    private Rv_ScanDeviceAdapter adapter;
    private LinearLayoutManager linearLayoutManager;

    //Bluetooth
    private BluetoothAdapter mbluetooth_adapter;
    private boolean mScanning = false;  //현재 스캔 상태
    private Handler mHandler;   //(블루투스 스캔시 -> 스캔 시간 조절할때 사용)
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 1000;    // 스캔이 지속되는 시간


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_device);

        ///RecyclerView 초기화
        rv_Scan = (RecyclerView) findViewById(R.id.rv_Scan);
        linearLayoutManager = new LinearLayoutManager(this);
        rv_Scan.setLayoutManager(linearLayoutManager);
        adapter = new Rv_ScanDeviceAdapter(this);
        rv_Scan.setAdapter(adapter);

        mHandler = new Handler();//헨들러 생성(블루투스 스캔시 -> 스캔 시간 조절할때 사용)

        //안드로이드 ver4.3이상 ? 아님? //지원되는 기기인지 체크
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "ble_not_supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        //BLE Adapter 얻어오기 (getSystemService, bluetoothManager)
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mbluetooth_adapter = bluetoothManager.getAdapter();

        // bluetooth 지원 기기 확인
        if (mbluetooth_adapter == null) {
            Toast.makeText(this, "ble_not_supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    //블루투스 스캔시작
    @SuppressLint("MissingPermission")
    private void bleScanDevice(final boolean enable) {
        if (enable) //true 이면 스캔시작
        {
            //10초 대기후 블루투스 스캔 종료
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false; //블루투스 스캔 상태 : false
                    mbluetooth_adapter.stopLeScan(BLE_Callback); //ble_scan_stop : 스캔 종료
                }
            }, SCAN_PERIOD); //BLE 스캔 : 10초(SCAN_PERIOD 변경하면 시간 조절 가능!!)

            mScanning = true;   //블루투스 스캔 상태 : true
            mbluetooth_adapter.startLeScan(BLE_Callback);   //ble_scan_start: 스캔 시작
        } else    //false 이면 스캔 종료
        {
            mScanning = false; //스캔 상태 : false
            mbluetooth_adapter.stopLeScan(BLE_Callback); // ble_scan_stop : 스캔 종료
        }
    }

    //블루투스 디바이스를 찾으면 실제 화면에 갱신시켜주는곳 (runOnUiThread <===> Handler.PostDelay <===> Main Thread)
    public BluetoothAdapter.LeScanCallback BLE_Callback = new BluetoothAdapter.LeScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            // 디바이스가 NotNull
            // 디바이스 이름이 NotNull
            if (device != null && device.getName() != null) {
                //찾으면 화면에 갱신- RecyclerView
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //RecyclerView Adapter에 아이템(Device) 추가
                        adapter.addDevice(device);
                        adapter.notifyDataSetChanged();  //RecyclerView 갱신
                    }
                });
            }
        }

    };


    //화면 구성이 끝나면
    //블루투스가 켜져있는지 확인하고 켜져있으면 스캔 시작
    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();
        //블루투스가 켜져있는 상태인지
        if (!mbluetooth_adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        bleScanDevice(true);
    }

    //화면이 꺼지는 등 화면전환이 생겼을때
    @Override
    protected void onPause() {
        super.onPause();
        //MainActivity.BTstate = false;   //MainActivity의 블루투스 연결 상태를 해제
        bleScanDevice(false);   //ble_scan_stop : 블루투스 스캔 종료
        adapter.clear();    //RecyclerView의 데이터 지우기
        adapter.notifyDataSetChanged();
    }


    //블루투스 꺼져있으면 끔
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish(); //아예 앱 끝내버림
        }

    }

    //RecyclerView Adapter에서 ScanDevice의 변수에 접근하기 위해 getter/setter
    public BluetoothAdapter getMbluetooth_adapter() {
        return mbluetooth_adapter;
    }

    public boolean ismScanning() {
        return mScanning;
    }

    public void setmScanning(boolean mScanning) {
        this.mScanning = mScanning;
    }
}