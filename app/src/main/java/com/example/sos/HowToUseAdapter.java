package com.example.sos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.res.ColorStateList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import java.util.List;

public class HowToUseAdapter extends RecyclerView.Adapter<HowToUseAdapter.TutorialViewHolder> {

    private Context context;
    private List<HowToUseActivity.TutorialStep> steps;

    public HowToUseAdapter(Context context, List<HowToUseActivity.TutorialStep> steps) {
        this.context = context;
        this.steps = steps;
    }

    @NonNull
    @Override
    public TutorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tutorial_page, parent, false);
        return new TutorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TutorialViewHolder holder, int position) {
        HowToUseActivity.TutorialStep step = steps.get(position);
        holder.bind(step);
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    class TutorialViewHolder extends RecyclerView.ViewHolder {

        ImageView ivIllustration;
        TextView tvTitle, tvSubtitle;
        LinearLayout layoutChecklist;
        GridLayout gridEmergency;

        public TutorialViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIllustration = itemView.findViewById(R.id.ivIllustration);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            layoutChecklist = itemView.findViewById(R.id.layoutChecklist);
            gridEmergency = itemView.findViewById(R.id.gridEmergency);
        }

        @SuppressWarnings("unchecked") // Suppress unchecked cast warnings as types are managed by step.type
        public void bind(HowToUseActivity.TutorialStep step) {
            tvTitle.setText(step.title);
            tvSubtitle.setText(step.subtitle);
            if (step.imageResId != 0) {
                ivIllustration.setImageResource(step.imageResId);

                // Logic to tint only vector icons, not the main logo
                if (step.imageResId == R.drawable.women_sos_logo) {
                    ivIllustration.setImageTintList(null);
                } else {
                    int colorPrimary = MaterialColors.getColor(ivIllustration,
                            com.google.android.material.R.attr.colorPrimary);
                    ivIllustration.setImageTintList(ColorStateList.valueOf(colorPrimary));
                }
            }

            // Reset views
            layoutChecklist.setVisibility(View.GONE);
            gridEmergency.setVisibility(View.GONE);
            layoutChecklist.removeAllViews();
            gridEmergency.removeAllViews();

            if (step.type == HowToUseActivity.TutorialStep.TYPE_CHECKLIST) {
                layoutChecklist.setVisibility(View.VISIBLE);
                if (step.contentData instanceof List) {
                    List<String> items = (List<String>) step.contentData;
                    for (String item : items) {
                        addChecklistItem(item);
                    }
                }
            } else if (step.type == HowToUseActivity.TutorialStep.TYPE_GRID) {
                gridEmergency.setVisibility(View.VISIBLE);
                if (step.contentData instanceof List) {
                    List<HowToUseActivity.TutorialStep.EmergencyType> types = (List<HowToUseActivity.TutorialStep.EmergencyType>) step.contentData;
                    for (HowToUseActivity.TutorialStep.EmergencyType type : types) {
                        addEmergencyCard(type);
                    }
                }
            } else if (step.type == HowToUseActivity.TutorialStep.TYPE_FINAL) {
                // Final screen visual adjustments if needed
                if (step.imageResId != R.drawable.women_sos_logo) {
                    int colorPrimary = MaterialColors.getColor(ivIllustration,
                            com.google.android.material.R.attr.colorPrimary);
                    ivIllustration.setImageTintList(ColorStateList.valueOf(colorPrimary));
                }
            }
        }

        private void addChecklistItem(String text) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_tutorial_checklist_row, layoutChecklist,
                    false);
            TextView tvText = view.findViewById(R.id.tvText);
            tvText.setText(text);
            layoutChecklist.addView(view);
        }

        private void addEmergencyCard(HowToUseActivity.TutorialStep.EmergencyType type) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_tutorial_emergency_card, gridEmergency,
                    false);
            TextView tvLabel = view.findViewById(R.id.tvLabel);
            TextView tvDesc = view.findViewById(R.id.tvDesc);
            ImageView ivIcon = view.findViewById(R.id.ivIcon);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.width = 0; // Important for weight
            params.setMargins(16, 16, 16, 16);
            view.setLayoutParams(params);

            tvLabel.setText(type.name);
            tvDesc.setText(type.desc);
            ivIcon.setImageResource(type.iconResId);

            gridEmergency.addView(view);
        }
    }
}
