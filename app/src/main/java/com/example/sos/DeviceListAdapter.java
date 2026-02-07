package com.example.sos;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {

    private Context context;
    private List<BluetoothDevice> deviceList;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onConnectClick(BluetoothDevice device);

        void onItemClick(BluetoothDevice device);
    }

    public DeviceListAdapter(Context context, OnDeviceClickListener listener) {
        this.context = context;
        this.deviceList = new ArrayList<>();
        this.listener = listener;
    }

    public void addDevice(BluetoothDevice device) {
        if (!deviceList.contains(device)) {
            deviceList.add(device);
            notifyDataSetChanged();
        }
    }

    public void clear() {
        deviceList.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_device_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BluetoothDevice device = deviceList.get(position);
        try {
            String name = device.getName();
            holder.tvName.setText(name != null && !name.isEmpty() ? name : "Unknown Device");
            holder.tvAddress.setText(device.getAddress());

            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                holder.btnConnect.setText("Paired"); // User asked: click "Paired" to unpair.
                // Optionally change text color or style if needed
            } else {
                holder.btnConnect.setText("Pair");
            }
        } catch (SecurityException e) {
            holder.tvName.setText("Permission Error");
        }

        holder.btnConnect.setOnClickListener(v -> listener.onConnectClick(device));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(device));
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress;
        com.google.android.material.button.MaterialButton btnConnect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDeviceName);
            tvAddress = itemView.findViewById(R.id.tvDeviceAddress);
            btnConnect = itemView.findViewById(R.id.btnConnect);
        }
    }
}
