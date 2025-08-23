// עדכן את EmailAdapter.java עם DiffUtil לביצועים טובים יותר:
package com.example.gmailapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gmailapplication.R;
import com.example.gmailapplication.shared.Email;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmailAdapter extends RecyclerView.Adapter<EmailAdapter.EmailViewHolder> {

    private List<Email> emails = new ArrayList<>();
    private OnEmailClickListener listener;

    public interface OnEmailClickListener {
        void onEmailClick(Email email);
        void onEmailLongClick(Email email);
    }

    public EmailAdapter(List<Email> emails, OnEmailClickListener listener) {
        this.emails = emails != null ? emails : new ArrayList<>();
        this.listener = listener;
    }

    // מתודה מעודכנת לעדכון הרשימה עם DiffUtil
    public void updateEmails(List<Email> newEmails) {
        if (newEmails == null) {
            newEmails = new ArrayList<>();
        }

        EmailDiffCallback diffCallback = new EmailDiffCallback(this.emails, newEmails);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.emails.clear();
        this.emails.addAll(newEmails);
        diffResult.dispatchUpdatesTo(this);
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
        private ImageView ivStar, ivRead;
        private View spamIndicator, unreadIndicator;

        public EmailViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvPreview = itemView.findViewById(R.id.tvPreview);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivStar = itemView.findViewById(R.id.ivStar);
            ivRead = itemView.findViewById(R.id.ivRead);
            spamIndicator = itemView.findViewById(R.id.spamIndicator);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);

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

            // Star indicator
            if (ivStar != null) {
                if (email.isStarred) {
                    ivStar.setVisibility(View.VISIBLE);
                    ivStar.setImageResource(android.R.drawable.btn_star_big_on);
                } else {
                    ivStar.setVisibility(View.GONE);
                }
            }

            // Read/Unread indicators
            if (ivRead != null) {
                if (email.isRead) {
                    ivRead.setImageResource(android.R.drawable.ic_menu_view);
                } else {
                    ivRead.setImageResource(android.R.drawable.ic_menu_edit);
                }
            }

            if (unreadIndicator != null) {
                unreadIndicator.setVisibility(email.isRead ? View.GONE : View.VISIBLE);
            }

            // Visual styling based on read status
            float alpha = email.isRead ? 0.7f : 1.0f;
            tvSender.setAlpha(alpha);
            tvSubject.setAlpha(alpha);
            tvPreview.setAlpha(alpha);

            // Bold text for unread emails
            if (!email.isRead) {
                tvSender.setTypeface(null, android.graphics.Typeface.BOLD);
                tvSubject.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tvSender.setTypeface(null, android.graphics.Typeface.NORMAL);
                tvSubject.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }

        private String formatTime(String timestamp) {
            try {
                // Parse ISO timestamp
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                Date date = isoFormat.parse(timestamp);

                // Check if it's today
                SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
                String today = dayFormat.format(new Date());
                String emailDay = dayFormat.format(date);

                if (today.equals(emailDay)) {
                    // Show time for today
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
                    return timeFormat.format(date);
                } else {
                    // Show date for older emails
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.US);
                    return dateFormat.format(date);
                }
            } catch (Exception e) {
                // Fallback: show as-is or just time part
                if (timestamp != null && timestamp.length() > 10) {
                    return timestamp.substring(11, 16); // HH:mm
                }
                return "לא ידוע";
            }
        }
    }

    // DiffCallback for efficient updates
    private static class EmailDiffCallback extends DiffUtil.Callback {
        private final List<Email> oldList;
        private final List<Email> newList;

        EmailDiffCallback(List<Email> oldList, List<Email> newList) {
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
            Email oldEmail = oldList.get(oldItemPosition);
            Email newEmail = newList.get(newItemPosition);
            return oldEmail.id != null && oldEmail.id.equals(newEmail.id);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Email oldEmail = oldList.get(oldItemPosition);
            Email newEmail = newList.get(newItemPosition);

            return oldEmail.isRead == newEmail.isRead &&
                    oldEmail.isStarred == newEmail.isStarred &&
                    oldEmail.isArchived == newEmail.isArchived &&
                    oldEmail.isDeleted == newEmail.isDeleted &&
                    objectsEqual(oldEmail.subject, newEmail.subject) &&
                    objectsEqual(oldEmail.sender, newEmail.sender) &&
                    objectsEqual(oldEmail.preview, newEmail.preview) &&
                    objectsEqual(oldEmail.timestamp, newEmail.timestamp);
        }

        private boolean objectsEqual(Object obj1, Object obj2) {
            return (obj1 == null && obj2 == null) ||
                    (obj1 != null && obj1.equals(obj2));
        }
    }
}