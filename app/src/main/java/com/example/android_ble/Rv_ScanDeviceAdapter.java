package com.example.android_ble;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class Rv_ScanDeviceAdapter extends RecyclerView.Adapter<Rv_ScanDeviceAdapter.CustomViewHolder> {
    private ArrayList<BluetoothDevice> arrayList;
    private ScanDevice context;

    public Rv_ScanDeviceAdapter(ScanDevice context) {
        this.arrayList = new ArrayList<>();
        this.context = context;

    }

    @NonNull
    @org.jetbrains.annotations.NotNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull @org.jetbrains.annotations.NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_list_item, parent, false);
        CustomViewHolder holder = new CustomViewHolder(view);
        return holder;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onBindViewHolder(@NonNull @org.jetbrains.annotations.NotNull CustomViewHolder holder, int position) {
        holder.tv_device_name.setText(arrayList.get(position).getName());
        holder.tv_device_address.setText(arrayList.get(position).getAddress());
    }

    public void clear() {
        this.arrayList.clear();
    }

    public void addDevice(BluetoothDevice device) {
        if (!arrayList.contains(device)) {
            arrayList.add(device);
        }
    }

    @Override
    public int getItemCount() {
        return (null != arrayList ? arrayList.size() : 0);
    }

    public class CustomViewHolder extends RecyclerView.ViewHolder {
        protected TextView tv_device_name;
        protected TextView tv_device_address;

        public CustomViewHolder(@NonNull @org.jetbrains.annotations.NotNull View itemView) {
            super(itemView);
            this.tv_device_name = (TextView) itemView.findViewById(R.id.tv_device_name);
            this.tv_device_address = (TextView) itemView.findViewById(R.id.tv_device_address);

            //스캔된 디바이스(itemView)를 클릭하면
            // MainActivty로  "key"와 디바이스의 주소값을 넘겨주고 ScanDevice는 종료
            itemView.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void onClick(View view) {
                    //ScanDevice에서 블루투스 스캔중이면 스캔을 종료
                    if (context.ismScanning()) {
                        context.getMbluetooth_adapter().stopLeScan(context.BLE_Callback);
                        context.setmScanning(false);
                    }
                    Intent intent = new Intent();
                    intent.putExtra("key", tv_device_address.getText());
                    context.setResult(RESULT_OK, intent);
                    context.finish();
                }
            });
        }
    }
}