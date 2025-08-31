package com.example.gmailapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gmailapplication.R;
import com.example.gmailapplication.shared.Email;

import java.util.ArrayList;
import java.util.List;

public class EmailAdapter extends RecyclerView.Adapter<EmailAdapter.EmailViewHolder> {

    private List<Email> emails = new ArrayList<>();
    private OnEmailClickListener listener;
    private OnEmailDeleteListener deleteListener;
    private String currentUserEmail;

    public interface OnEmailClickListener {
        void onEmailClick(Email email);
    }

    public interface OnEmailDeleteListener {
        void onEmailDelete(Email email);
    }

    public EmailAdapter(OnEmailClickListener listener, String currentUserEmail) {
        this.listener = listener;
        this.currentUserEmail = currentUserEmail;
    }

    public void setDeleteListener(OnEmailDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void updateEmails(List<Email> newEmails) {
        System.out.println("=== ADAPTER DEBUG ===");
        System.out.println("updateEmails called with emails: " + (newEmails != null ? newEmails.size() : "null"));

        this.emails.clear();
        if (newEmails != null) {
            this.emails.addAll(newEmails);
        }
        System.out.println("Final adapter size: " + this.emails.size());
        notifyDataSetChanged();
        System.out.println("notifyDataSetChanged() called");
        System.out.println("==================");
    }

    @NonNull
    @Override
    public EmailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_email, parent, false);
        return new EmailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmailViewHolder holder, int position) {
        holder.bind(emails.get(position));
    }

    @Override
    public int getItemCount() {
        return emails.size();
    }

    class EmailViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSender, tvSubject, tvPreview, tvTime;
        private TextView tvLabels;
        private ImageView ivDelete;

        public EmailViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvPreview = itemView.findViewById(R.id.tvPreview);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvLabels = itemView.findViewById(R.id.tvLabels);
            ivDelete = itemView.findViewById(R.id.ivDelete);

            // לחיצה על המייל עצמו
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onEmailClick(emails.get(position));
                    }
                }
            });

            // לחיצה על כפתור המחיקה - פשוט
            ivDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        deleteListener.onEmailDelete(emails.get(position));
                    }
                }
            });
        }

        public void bind(Email email) {
            // Sender
            tvSender.setText(email.sender != null ? email.sender : "לא ידוע");

            // Subject
            tvSubject.setText(email.subject != null ? email.subject : "(ללא נושא)");

            // Preview
            String preview = email.content;
            if (preview != null && preview.length() > 100) {
                preview = preview.substring(0, 100) + "...";
            }
            tvPreview.setText(preview != null ? preview : "(ללא תוכן)");

            // Time - just show timestamp as-is for now
            tvTime.setText(email.timestamp != null ? email.timestamp.substring(11, 16) : "");

            // Labels
            displayLabels(email);
        }

        private void displayLabels(Email email) {
            if (email.labels != null && !email.labels.isEmpty()) {
                StringBuilder labelsText = new StringBuilder("תוויות: ");

                for (int i = 0; i < email.labels.size(); i++) {
                    if (i > 0) labelsText.append(" • ");
                    labelsText.append(email.labels.get(i));
                }

                tvLabels.setText(labelsText.toString());
                tvLabels.setVisibility(View.VISIBLE);

                // Debug
                System.out.println("Email " + email.id + " labels: " + email.labels);

            } else {
                tvLabels.setVisibility(View.GONE);

                // Debug
                System.out.println("Email " + email.id + " has no labels");
            }
        }
    }
}