package com.example.gmailapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gmailapplication.R;
import com.example.gmailapplication.shared.Email;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmailAdapter extends RecyclerView.Adapter<EmailAdapter.EmailViewHolder> {

    private List<Email> emails;
    private OnEmailClickListener listener;

    public interface OnEmailClickListener {
        void onEmailClick(Email email);
        void onEmailLongClick(Email email);
    }

    public EmailAdapter(List<Email> emails, OnEmailClickListener listener) {
        this.emails = emails;
        this.listener = listener;
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
        Email email = emails.get(position);
        holder.bind(email);
    }

    @Override
    public int getItemCount() {
        return emails.size();
    }

    class EmailViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSender, tvSubject, tvPreview, tvTime;
        private View spamIndicator;

        public EmailViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvPreview = itemView.findViewById(R.id.tvPreview);
            tvTime = itemView.findViewById(R.id.tvTime);
            spamIndicator = itemView.findViewById(R.id.spamIndicator);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onEmailClick(emails.get(position));
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onEmailLongClick(emails.get(position));
                        return true;
                    }
                }
                return false;
            });
        }

        public void bind(Email email) {
            // Display sender or recipient based on direction
            if ("sent".equals(email.direction)) {
                if (email.otherParty != null) {
                    tvSender.setText("אל: " + email.otherParty.getDisplayName());
                } else if (email.recipients != null && !email.recipients.isEmpty()) {
                    tvSender.setText("אל: " + email.recipients.get(0));
                } else {
                    tvSender.setText("אל: " + (email.recipient != null ? email.recipient : "לא ידוע"));
                }
            } else {
                if (email.otherParty != null) {
                    tvSender.setText(email.otherParty.getDisplayName());
                } else {
                    tvSender.setText(email.sender != null ? email.sender : "שולח לא ידוע");
                }
            }

            // Subject
            String subject = email.subject;
            if (subject == null || subject.trim().isEmpty()) {
                subject = "(ללא נושא)";
            }
            tvSubject.setText(subject);

            // Preview
            String preview = email.preview;
            if (preview == null || preview.trim().isEmpty()) {
                preview = email.content;
                if (preview != null && preview.length() > 100) {
                    preview = preview.substring(0, 100) + "...";
                }
            }
            if (preview == null || preview.trim().isEmpty()) {
                preview = "(ללא תוכן)";
            }
            tvPreview.setText(preview);

            // Time
            tvTime.setText(formatTime(email.timestamp));

            // Spam indicator
            if (email.isSpam()) {
                spamIndicator.setVisibility(View.VISIBLE);
            } else {
                spamIndicator.setVisibility(View.GONE);
            }
        }

        private String formatTime(String timestamp) {
            try {
                // Parse ISO timestamp
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                Date date = isoFormat.parse(timestamp);

                // Format for display
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.US);
                return displayFormat.format(date);
            } catch (Exception e) {
                // Fallback: show as-is or just time part
                if (timestamp != null && timestamp.length() > 10) {
                    return timestamp.substring(11, 16); // HH:mm
                }
                return "לא ידוע";
            }
        }
    }
}