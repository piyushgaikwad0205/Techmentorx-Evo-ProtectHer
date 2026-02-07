package com.example.sos;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class RespondersAdapter extends RecyclerView.Adapter<RespondersAdapter.ViewHolder> {

    private List<ResponderModel> respondersList;
    private OnChatClickListener chatClickListener;

    public interface OnChatClickListener {
        void onChatClick(ResponderModel responder);
    }

    public RespondersAdapter(List<ResponderModel> respondersList, OnChatClickListener listener) {
        this.respondersList = respondersList;
        this.chatClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_responder, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ResponderModel responder = respondersList.get(position);

        // Set name (use userId if name not available)
        String name = responder.getUserName() != null ? responder.getUserName()
                : "User " + responder.getUserId().substring(0, 6);
        holder.tvResponderName.setText(name);

        // Set status with color
        String status = responder.getStatus();
        if ("coming".equals(status)) {
            holder.tvResponderStatus.setText("🏃 Coming to help");
            holder.tvResponderStatus.setTextColor(
                    holder.itemView.getContext().getResources().getColor(R.color.colorWarning));
        } else if ("arrived".equals(status)) {
            holder.tvResponderStatus.setText("✅ Arrived");
            holder.tvResponderStatus.setTextColor(
                    holder.itemView.getContext().getResources().getColor(R.color.colorSuccess));
        } else {
            holder.tvResponderStatus.setText("📍 " + status);
        }

        // Set distance
        if (responder.getDistance() > 0) {
            holder.tvResponderDistance.setText(
                    String.format("%.1f meters away", responder.getDistance()));
        } else {
            holder.tvResponderDistance.setText("Distance unknown");
        }

        // Chat button click
        holder.btnChat.setOnClickListener(v -> {
            if (chatClickListener != null) {
                chatClickListener.onChatClick(responder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return respondersList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvResponderName;
        TextView tvResponderStatus;
        TextView tvResponderDistance;
        MaterialButton btnChat;

        ViewHolder(View itemView) {
            super(itemView);
            tvResponderName = itemView.findViewById(R.id.tvResponderName);
            tvResponderStatus = itemView.findViewById(R.id.tvResponderStatus);
            tvResponderDistance = itemView.findViewById(R.id.tvResponderDistance);
            btnChat = itemView.findViewById(R.id.btnChat);
        }
    }
}
