package com.example.sos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private Context context;
    private ArrayList<HashMap<String, String>> historyList;

    public HistoryAdapter(Context context, ArrayList<HashMap<String, String>> historyList) {
        this.context = context;
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HashMap<String, String> map = historyList.get(position);

        // Set title with type
        String type = map.get("type");
        if (type != null) {
            holder.tvSosTitle.setText("SOS Triggered (" + type + ")");
        } else {
            holder.tvSosTitle.setText("SOS Triggered");
        }

        // Set date
        String time = map.get("time");
        if (time != null) {
            // Extract just the date part (YYYY-MM-DD)
            if (time.contains(" ")) {
                holder.tvSosDate.setText(time.split(" ")[0]);
            } else {
                holder.tvSosDate.setText(time);
            }
        }

        // Set location
        String location = map.get("location");
        if (location != null) {
            holder.tvSosLocation.setText("Location: " + location);
        } else {
            holder.tvSosLocation.setText("Location: Not available");
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSosTitle, tvSosDate, tvSosLocation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSosTitle = itemView.findViewById(R.id.tvSosTitle);
            tvSosDate = itemView.findViewById(R.id.tvSosDate);
            tvSosLocation = itemView.findViewById(R.id.tvSosLocation);
        }
    }
}
