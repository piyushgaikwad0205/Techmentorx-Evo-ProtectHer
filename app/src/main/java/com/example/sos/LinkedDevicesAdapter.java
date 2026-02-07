package com.example.sos;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Map;

public class LinkedDevicesAdapter extends RecyclerView.Adapter<LinkedDevicesAdapter.ViewHolder> {

    private Context context;
    private ArrayList<String> deviceIds;
    private Map<String, String> deviceNames;
    private OnDeviceDeleteListener deleteListener;

    public interface OnDeviceDeleteListener {
        void onDelete(String deviceId);
    }

    public LinkedDevicesAdapter(Context context, ArrayList<String> deviceIds, Map<String, String> deviceNames,
            OnDeviceDeleteListener deleteListener) {
        this.context = context;
        this.deviceIds = deviceIds;
        this.deviceNames = deviceNames;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_linked_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String deviceId = deviceIds.get(position);
        String name = deviceNames != null ? deviceNames.get(deviceId) : "Child Device";

        holder.tvDeviceName.setText(name);

        TextView tvDeviceID = holder.itemView.findViewById(R.id.tvDeviceID);
        if (tvDeviceID != null) {
            tvDeviceID.setText("ID: " + deviceId);
        }

        // Click to open dashboard
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ParentDashboardActivity.class);
            intent.putExtra("TARGET_CHILD_ID", deviceId);
            context.startActivity(intent);
        });

        // Delete button with confirmation
        holder.btnDelete.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(context)
                    .setTitle("Remove Device")
                    .setMessage("Are you sure you want to remove \"" + name + "\" from linked devices?")
                    .setPositiveButton("Remove", (dialog, which) -> {
                        if (deleteListener != null) {
                            deleteListener.onDelete(deviceId);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return deviceIds.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName;
        ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            btnDelete = itemView.findViewById(R.id.btnDeleteDevice);
        }
    }
}
