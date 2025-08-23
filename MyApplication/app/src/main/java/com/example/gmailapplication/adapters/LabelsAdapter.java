package com.example.gmailapplication.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gmailapplication.R;
import com.example.gmailapplication.database.entities.LabelEntity;

import java.util.ArrayList;
import java.util.List;

public class LabelsAdapter extends RecyclerView.Adapter<LabelsAdapter.LabelViewHolder> {

    private List<LabelEntity> labels = new ArrayList<>();
    private OnLabelClickListener listener;

    public interface OnLabelClickListener {
        void onLabelClick(LabelEntity label);
        void onLabelLongClick(LabelEntity label);
        void onEditLabel(LabelEntity label);
        void onDeleteLabel(LabelEntity label);
    }

    public LabelsAdapter(List<LabelEntity> labels, OnLabelClickListener listener) {
        this.labels = labels != null ? labels : new ArrayList<>();
        this.listener = listener;
    }

    public void updateLabels(List<LabelEntity> newLabels) {
        if (newLabels == null) {
            newLabels = new ArrayList<>();
        }

        LabelDiffCallback diffCallback = new LabelDiffCallback(this.labels, newLabels);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.labels.clear();
        this.labels.addAll(newLabels);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public LabelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_label, parent, false);
        return new LabelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LabelViewHolder holder, int position) {
        LabelEntity label = labels.get(position);
        holder.bind(label);
    }

    @Override
    public int getItemCount() {
        return labels.size();
    }

    class LabelViewHolder extends RecyclerView.ViewHolder {
        private TextView tvLabelName, tvLabelType;
        private ImageView ivLabelIcon, ivOptions;
        private View colorIndicator;

        public LabelViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLabelName = itemView.findViewById(R.id.tvLabelName);
            tvLabelType = itemView.findViewById(R.id.tvLabelType);
            ivLabelIcon = itemView.findViewById(R.id.ivLabelIcon);
            ivOptions = itemView.findViewById(R.id.ivOptions);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onLabelClick(labels.get(position));
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onLabelLongClick(labels.get(position));
                        return true;
                    }
                }
                return false;
            });

            ivOptions.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onLabelLongClick(labels.get(position));
                    }
                }
            });
        }

        public void bind(LabelEntity label) {
            tvLabelName.setText(label.name);

            // סוג התווית
            if (label.isSystemLabel) {
                tvLabelType.setText("תווית מערכת");
                tvLabelType.setVisibility(View.VISIBLE);
                ivOptions.setVisibility(View.GONE); // תוויות מערכת - אין אופציות
            } else {
                tvLabelType.setText("תווית אישית");
                tvLabelType.setVisibility(View.VISIBLE);
                ivOptions.setVisibility(View.VISIBLE);
            }

            // אייקון לפי סוג התווית
            if (label.isInbox()) {
                ivLabelIcon.setImageResource(android.R.drawable.ic_dialog_email);
            } else if (label.isSent()) {
                ivLabelIcon.setImageResource(android.R.drawable.ic_menu_send);
            } else if (label.isSpam()) {
                ivLabelIcon.setImageResource(android.R.drawable.ic_delete);
            } else if (label.isTrash()) {
                ivLabelIcon.setImageResource(android.R.drawable.ic_menu_delete);
            } else {
                ivLabelIcon.setImageResource(android.R.drawable.ic_menu_manage);
            }

            // צבע התווית
            if (label.color != null && !label.color.isEmpty()) {
                try {
                    int color = Color.parseColor(label.color);
                    colorIndicator.setBackgroundColor(color);
                    colorIndicator.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    colorIndicator.setVisibility(View.GONE);
                }
            } else {
                // צבע ברירת מחדל לפי סוג
                if (label.isSystemLabel) {
                    colorIndicator.setBackgroundColor(Color.BLUE);
                } else {
                    colorIndicator.setBackgroundColor(Color.GREEN);
                }
                colorIndicator.setVisibility(View.VISIBLE);
            }

            // סטטוס sync
            if ("pending".equals(label.syncStatus)) {
                tvLabelName.setAlpha(0.7f);
                tvLabelType.setText("מסנכרן...");
            } else if ("failed".equals(label.syncStatus)) {
                tvLabelName.setAlpha(0.5f);
                tvLabelType.setText("שגיאת סנכרון");
            } else {
                tvLabelName.setAlpha(1.0f);
            }
        }
    }

    // DiffCallback for efficient updates
    private static class LabelDiffCallback extends DiffUtil.Callback {
        private final List<LabelEntity> oldList;
        private final List<LabelEntity> newList;

        LabelDiffCallback(List<LabelEntity> oldList, List<LabelEntity> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            LabelEntity oldLabel = oldList.get(oldItemPosition);
            LabelEntity newLabel = newList.get(newItemPosition);
            return oldLabel.id != null && oldLabel.id.equals(newLabel.id);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            LabelEntity oldLabel = oldList.get(oldItemPosition);
            LabelEntity newLabel = newList.get(newItemPosition);

            return objectsEqual(oldLabel.name, newLabel.name) &&
                    objectsEqual(oldLabel.color, newLabel.color) &&
                    objectsEqual(oldLabel.syncStatus, newLabel.syncStatus) &&
                    oldLabel.isSystemLabel == newLabel.isSystemLabel &&
                    oldLabel.needsSync == newLabel.needsSync;
        }

        private boolean objectsEqual(Object obj1, Object obj2) {
            return (obj1 == null && obj2 == null) ||
                    (obj1 != null && obj1.equals(obj2));
        }
    }
}