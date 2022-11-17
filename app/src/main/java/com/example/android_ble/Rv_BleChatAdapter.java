package com.example.android_ble;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class Rv_BleChatAdapter extends RecyclerView.Adapter<Rv_BleChatAdapter.CustomViewHolder> {
    private ArrayList<String> arrayList;

    public Rv_BleChatAdapter() {
        this.arrayList = new ArrayList<>();
    }

    @NonNull
    @org.jetbrains.annotations.NotNull
    @Override
    public Rv_BleChatAdapter.CustomViewHolder onCreateViewHolder(@NonNull @org.jetbrains.annotations.NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_list_item, parent, false);
        Rv_BleChatAdapter.CustomViewHolder holder = new Rv_BleChatAdapter.CustomViewHolder(view);
        return holder;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onBindViewHolder(@NonNull @org.jetbrains.annotations.NotNull Rv_BleChatAdapter.CustomViewHolder holder, int position) {
        holder.tv_RvChat.setText(arrayList.get(position));
    }

    public void clear() {
        this.arrayList.clear();
    }

    public void addChat(String chat) {
        arrayList.add(chat);
    }

    @Override
    public int getItemCount() {
        return (null != arrayList ? arrayList.size() : 0);
    }

    public class CustomViewHolder extends RecyclerView.ViewHolder {
        protected TextView tv_RvChat;

        public CustomViewHolder(@NonNull @org.jetbrains.annotations.NotNull View itemView) {
            super(itemView);
            this.tv_RvChat = (TextView) itemView.findViewById(R.id.tv_RvChat);
        }
    }
}