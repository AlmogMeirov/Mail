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
    private OnStarClickListener starListener;
    private OnEditDraftListener editDraftListener; // חדש
    private String currentUserEmail;

    public interface OnEmailClickListener {
        void onEmailClick(Email email);
    }

    public interface OnEmailDeleteListener {
        void onEmailDelete(Email email);
    }

    public interface OnStarClickListener {
        void onStar(Email email);
    }

    // חדש: ממשק לעריכת טיוטות
    public interface OnEditDraftListener {
        void onEditDraft(Email email);
    }

    public EmailAdapter(OnEmailClickListener listener, String currentUserEmail) {
        this.listener = listener;
        this.currentUserEmail = currentUserEmail;
    }

    public void setDeleteListener(OnEmailDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setStarListener(OnStarClickListener starListener) {
        this.starListener = starListener;
    }

    // חדש
    public void setEditDraftListener(OnEditDraftListener editDraftListener) {
        this.editDraftListener = editDraftListener;
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
        private ImageView ivDelete, ivStar, ivEdit; // הוסף ivEdit

        public EmailViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvPreview = itemView.findViewById(R.id.tvPreview);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvLabels = itemView.findViewById(R.id.tvLabels);
            ivDelete = itemView.findViewById(R.id.ivDelete);
            ivStar = itemView.findViewById(R.id.ivStar);
            ivEdit = itemView.findViewById(R.id.ivEdit); // הוסף אם קיים בlayout

            // לחיצה על המייל עצמו
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onEmailClick(emails.get(position));
                    }
                }
            });

            // לחיצה על כפתור המחיקה
            ivDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        deleteListener.onEmailDelete(emails.get(position));
                    }
                }
            });

            // לחיצה על הכוכב
            ivStar.setOnClickListener(v -> {
                if (starListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        starListener.onStar(emails.get(position));
                    }
                }
            });

            // לחיצה על כפתור העריכה (טיוטות)
            if (ivEdit != null) {
                ivEdit.setOnClickListener(v -> {
                    if (editDraftListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            editDraftListener.onEditDraft(emails.get(position));
                        }
                    }
                });
            }
        }

        public void bind(Email email) {
            boolean isDraft = isDraft(email);

            // Sender
            tvSender.setText(email.sender != null ? email.sender : "לא ידוע");

            // Subject - הוסף אינדיקציה לטיוטה
            String subject = email.subject != null ? email.subject : "(ללא נושא)";
            if (isDraft) {
                subject = "[טיוטה] " + subject;
                tvSubject.setTextColor(android.graphics.Color.rgb(255, 152, 0)); // כתום
            } else {
                tvSubject.setTextColor(android.graphics.Color.BLACK); // צבע רגיל
            }
            tvSubject.setText(subject);

            // Preview
            String preview = email.content;
            if (preview != null && preview.length() > 100) {
                preview = preview.substring(0, 100) + "...";
            }
            tvPreview.setText(preview != null ? preview : "(ללא תוכן)");

            // Time
            tvTime.setText(email.timestamp != null ? email.timestamp.substring(11, 16) : "");

            // Labels
            displayLabels(email);

            // הגדר כפתורים לפי סוג המייל
            configureButtonsForEmailType(email, isDraft);
        }

        private void configureButtonsForEmailType(Email email, boolean isDraft) {
            if (isDraft) {
                // טיוטה: הסתר כוכב, הצג עריכה
                ivStar.setVisibility(View.GONE);
                if (ivEdit != null) {
                    ivEdit.setVisibility(View.VISIBLE);
                }
                // מחיקה נשארת זמינה
                ivDelete.setVisibility(View.VISIBLE);
            } else {
                // מייל רגיל: הצג כוכב, הסתר עריכה
                ivStar.setVisibility(View.VISIBLE);
                if (ivEdit != null) {
                    ivEdit.setVisibility(View.GONE);
                }
                ivDelete.setVisibility(View.VISIBLE);
                // עדכן כוכב
                updateStarButton(email);
            }
        }

        private boolean isDraft(Email email) {
            return (email.labels != null && email.labels.contains("drafts"));
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

        private void updateStarButton(Email email) {
            boolean isStarred = email.labels != null && email.labels.contains("starred");

            if (isStarred) {
                ivStar.setImageResource(android.R.drawable.btn_star_big_on);
                ivStar.setColorFilter(android.graphics.Color.rgb(255, 193, 7)); // צהוב זהב
            } else {
                ivStar.setImageResource(android.R.drawable.btn_star_big_off);
                ivStar.setColorFilter(android.graphics.Color.GRAY);
            }
        }
    }
}