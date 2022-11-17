package com.example.android_ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {


    //BLE Scan
    private final int BLE_RETURN = 100; //ScanDevice_Activity 로 부터 반환 코드
    private FloatingActionButton btn_Scan; //스캔 화면으로 전환해주는 버튼

    //BLE Connect
    public static String mDeviceAddress = "00:00:00:00:00:00";
    public static boolean BTstate = false; //블루투스 연결상태 : false로 초기화
    private BluetoothLeService mBluetoothLeService;
    private ConstraintLayout layout_pair;
    private ConstraintLayout layout_Chat;
    private TextView tv_State;


    //BLE Data 송수신
    private BluetoothGattCharacteristic BTcharacteristic_read; //연결된 기기로 부터 받은 데이터가 들어가는 변수
    private BluetoothGattCharacteristic click_ArrayList_data;
    private TextView et_chat;
    private Button btn_send;
    private RecyclerView rv_Chat;
    private Rv_BleChatAdapter adapter;
    private LinearLayoutManager linearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //권한 체크( >= marshMellow)
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            showPermissionDialog();
        }

        btn_Scan = (FloatingActionButton) findViewById(R.id.btn_Scan);
        btn_Scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ScanDevice.class);
                intent.putExtra("key", "value");
                startActivityForResult(intent, BLE_RETURN);
            }
        });

        //레이아웃
        layout_pair = (ConstraintLayout) findViewById(R.id.layout_pair);
        layout_Chat = (ConstraintLayout) findViewById(R.id.layout_chat);
        tv_State = (TextView)findViewById(R.id.tv_state);

        //데이터 송수신
        et_chat = (TextView) findViewById(R.id.et_chat);
        btn_send = (Button) findViewById(R.id.btn_send);

        rv_Chat = (RecyclerView)findViewById(R.id.rv_Chat);
        linearLayoutManager = new LinearLayoutManager(this);
        rv_Chat.setLayoutManager(linearLayoutManager);
        adapter = new Rv_BleChatAdapter();
        rv_Chat.setAdapter(adapter);

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BTstate) {
                    if(et_chat.getText().toString().length()>0){
                        sendData(et_chat.getText().toString());
                    }else{
                        Toast.makeText(getApplicationContext(),"입력을 해주세요",Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "BLE연결을 확인하십시오.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    /////////////////////////////////   연결 //////////////////////////////
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        //ScanDevice 클래스로 부터 값이 넘어오면 여기로
        if (requestCode == BLE_RETURN) {
            //ScanDevice에서 선택한 기기의 주소값을 가져옴
            mDeviceAddress = data.getStringExtra("key");
            BLE_connct(); // 선택한 기기와 연결 ==> 다음장에서 주석 풀어야 함

        }
    }

    // BluetoothLeSerivce클래스와 바인드되어 클라이언트 입장에서 실시간으로 상황받아옴
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService(); //BluetoothLeService클래스 인스턴스화
            //BluetoothLeService 클래스에서 가져오기를 실패하였을 때
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress); //첫연결할때 여기서 연결
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // BluetoothLeService에서 sendBroadcast하면 전달받음
    private final BroadcastReceiver BLE_broadCastReciver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) //기기연결이 성공했을때
            {
                connectUI();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) //기기가 연결되어있지 않을때
            {
                disconnectUI();
                BTstate = false;//블루투스 연결상태 : false(연결 해제)

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) //서비스를 찾았을때 들어옴
            {
                BTstate = true; //블루투스 연결상태 : true(연결중)
                readData();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) //값을 찾았을때 들어옴
            {
                if (intent.getStringExtra(BluetoothLeService.EXTRA_DATA) != null) {
                    String getData = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                    adapter.addChat(getData);
                    adapter.notifyDataSetChanged();
                    Log.w("String getData = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);", getData);
                }
            }
        }
    };

    //블루투스 연결
    public void BLE_connct() {
        //두 개의 액티비티가 하나의 서비스의 데이터를 전달받는 것
        Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE); // -> 7

        registerReceiver(BLE_broadCastReciver, makeGattUpdateIntentFilter());
        //폰 화면 꺼졋다가 연결할 시에 (pause()되었다가 연결함) 안들어감
        if (mBluetoothLeService != null) {
            mBluetoothLeService.disconnect(); //point
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
        }
    }

    //블루투스가 연결 실패 됐을때
    private void disconnectUI() {
        mBluetoothLeService.disconnect(); //point
        layout_Chat.setVisibility(View.GONE);
        layout_pair.setVisibility(View.VISIBLE);
        tv_State.setText("Bad");
        adapter.clear();
        adapter.notifyDataSetChanged();
    }

    //블루투스가 연결 됐을때
    private void connectUI() {
        layout_Chat.setVisibility(View.VISIBLE);
        layout_pair.setVisibility(View.GONE);
        tv_State.setText("Good");
    }

    //GATT UUID필터(bluetoothLeService.java에서 이것만 들어올수 있게)
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    ////////////////////////////////  Read Write  ///////////////////////////////
    //데이터 받아오기 클릭
    @Nullable
    private void getCharacteristics() {
        if (BTcharacteristic_read != null) {
            final BluetoothGattCharacteristic characteristic = BTcharacteristic_read;
            final int click_ArrayList_properties = characteristic.getProperties();
            if ((click_ArrayList_properties | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                if (click_ArrayList_data != null) {
                    mBluetoothLeService.setCharacteristicNotification(click_ArrayList_data, false);
                    click_ArrayList_data = null;
                }
                mBluetoothLeService.readCharacteristic(characteristic); //아직 characteristic값 없음
            }
            if ((click_ArrayList_properties | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0)//읽을때 일로 들어옴
            {
                click_ArrayList_data = characteristic;
                //delay를 줘야하는것 같음!!!!!
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                    }
                }, 3000);
            }
        }
    }

    public void sendData(String data) {
        byte[] sendHex = TextUtil.getInstance().hexStringToByteArray(data);
        Log.w("sendHex", String.valueOf(sendHex.length));
        BluetoothGattCharacteristic BTcharacteristic_write = mBluetoothLeService.getSupportedGattServices_write();

        mBluetoothLeService.writeCharacteristic(BTcharacteristic_write, sendHex); //TX
    }

    public void readData() {
        BTcharacteristic_read = mBluetoothLeService.getSupportedGattServices_read(); //변경
        getCharacteristics(); //데이터 찾는 순간, 받아오기 클릭
    }


    //권한 체크( >= marshMellow)
    private void showPermissionDialog() {
        if (!MainActivity.checkPermission(this)) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    100);
        }
    }

    //권한 체크( >= marshMellow)
    public static boolean checkPermission(final Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}