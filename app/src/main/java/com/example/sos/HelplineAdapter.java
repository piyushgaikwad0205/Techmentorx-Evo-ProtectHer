package com.example.sos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class HelplineAdapter extends RecyclerView.Adapter<HelplineAdapter.ViewHolder> {

    private Context context;
    private ArrayList<HelplineModel> list;
    private OnHelplineActionListener listener;

    public interface OnHelplineActionListener {
        void onCall(String number);

        void onEdit(HelplineModel model);
    }

    public HelplineAdapter(Context context, ArrayList<HelplineModel> list, OnHelplineActionListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_helpline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HelplineModel model = list.get(position);
        holder.name.setText(model.getName());
        holder.number.setText(model.getNumber());
        holder.btnKeyword.setText("Keyword : " + model.getKeyword());

        holder.btnCall.setOnClickListener(v -> {
            if (listener != null)
                listener.onCall(model.getNumber());
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null)
                listener.onEdit(model);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, number;
        MaterialButton btnCall, btnKeyword;
        View btnEdit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.helplineName);
            number = itemView.findViewById(R.id.helplineNumber);
            btnKeyword = itemView.findViewById(R.id.btnKeyword);
            btnCall = itemView.findViewById(R.id.btnCallHelpline);
            btnEdit = itemView.findViewById(R.id.btnEditHelpline);
        }
    }
}
