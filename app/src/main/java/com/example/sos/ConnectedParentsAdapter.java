package com.example.sos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sos.R;
import java.util.List;
import java.util.Map;

public class ConnectedParentsAdapter extends RecyclerView.Adapter<ConnectedParentsAdapter.ViewHolder> {

    private Context context;
    private List<Map<String, Object>> parentsList;
    private OnParentLogoutListener listener;

    public interface OnParentLogoutListener {
        void onLogout(String parentUid);
    }

    public ConnectedParentsAdapter(Context context, List<Map<String, Object>> parentsList,
            OnParentLogoutListener listener) {
        this.context = context;
        this.parentsList = parentsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_connected_parent, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> parentData = parentsList.get(position);

        String displayName = "Unknown Parent";
        if (parentData.containsKey("name")) {
            displayName = (String) parentData.get("name");
        } else if (parentData.containsKey("email")) {
            displayName = (String) parentData.get("email");
        }

        String uid = (String) parentData.get("uid");

        holder.tvParentEmail.setText(displayName);
        if (uid != null) {
            String shortId = uid.length() > 8 ? uid.substring(0, 8) + "..." : uid;
            holder.tvParentId.setText("ID: " + shortId);
        } else {
            holder.tvParentId.setText("ID: Unknown");
        }

        final String finalDisplayName = displayName;
        holder.btnRemoveParent.setOnClickListener(v -> {
            if (listener != null && uid != null) {
                // Show confirmation before removing
                new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Unlink Parent")
                        .setMessage("Are you sure you want to unlink " + finalDisplayName + "?")
                        .setPositiveButton("Unlink", (dialog, which) -> listener.onLogout(uid))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return parentsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvParentEmail;
        TextView tvParentId;
        android.widget.ImageView btnRemoveParent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvParentEmail = itemView.findViewById(R.id.tvParentEmail);
            tvParentId = itemView.findViewById(R.id.tvParentId);
            btnRemoveParent = itemView.findViewById(R.id.btnRemoveParent);
        }
    }
}
