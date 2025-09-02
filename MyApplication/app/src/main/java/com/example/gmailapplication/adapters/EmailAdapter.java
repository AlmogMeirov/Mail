package com.example.gmailapplication.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private OnEditDraftListener editDraftListener;
    private String currentUserEmail;

    // הממשקים נשארים אותו דבר
    public interface OnEmailClickListener {
        void onEmailClick(Email email);
    }

    public interface OnEmailDeleteListener {
        void onEmailDelete(Email email);
    }

    public interface OnStarClickListener {
        void onStar(Email email);
    }

    public interface OnEditDraftListener {
        void onEditDraft(Email email);
    }

    public EmailAdapter(OnEmailClickListener listener, String currentUserEmail) {
        this.listener = listener;
        this.currentUserEmail = currentUserEmail;
    }

    // Setters נשארים אותו דבר
    public void setDeleteListener(OnEmailDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setStarListener(OnStarClickListener starListener) {
        this.starListener = starListener;
    }

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
        private TextView tvSenderAvatar, tvSender, tvSubject, tvPreview, tvTime;
        private TextView tvLabels;
        private LinearLayout layoutLabels, layoutIndicators;
        private ImageView ivStar, ivMore, ivAttachment, ivImportant, ivSpamIndicator;

        public EmailViewHolder(@NonNull View itemView) {
            super(itemView);
            // Views from new layout
            tvSenderAvatar = itemView.findViewById(R.id.tvSenderAvatar);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvPreview = itemView.findViewById(R.id.tvPreview);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvLabels = itemView.findViewById(R.id.tvLabels);
            layoutLabels = itemView.findViewById(R.id.layoutLabels);
            layoutIndicators = itemView.findViewById(R.id.layoutIndicators);
            ivStar = itemView.findViewById(R.id.ivStar);
            ivMore = itemView.findViewById(R.id.ivMore);
            ivAttachment = itemView.findViewById(R.id.ivAttachment);
            ivImportant = itemView.findViewById(R.id.ivImportant);
            ivSpamIndicator = itemView.findViewById(R.id.ivSpamIndicator);

            setupClickListeners();
        }

        private void setupClickListeners() {
            // לחיצה על המייל עצמו
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onEmailClick(emails.get(position));
                    }
                }
            });

            // לחיצה על הכוכב
            if (ivStar != null) {
                ivStar.setOnClickListener(v -> {
                    if (starListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            starListener.onStar(emails.get(position));
                        }
                    }
                });
            }

            // לחיצה על כפתור More (תפריט אפשרויות)
            if (ivMore != null) {
                ivMore.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            // כרגע נשתמש בזה למחיקה, אבל אפשר להוסיף PopupMenu
                            deleteListener.onEmailDelete(emails.get(position));
                        }
                    }
                });
            }
        }

        public void bind(Email email) {
            boolean isDraft = isDraft(email);
            boolean isSpam = isSpam(email);
            boolean isImportant = isImportant(email);

            // Avatar - אות ראשונה של השולח
            setupSenderAvatar(email);

            // Sender
            setupSender(email);

            // Subject - הוסף אינדיקציה לטיוטה וספאם
            setupSubject(email, isDraft, isSpam);

            // Preview
            setupPreview(email);

            // Time - פורמט יפה יותר
            setupTime(email);

            // Labels
            setupLabels(email);

            // הגדר אינדיקטורים (קבצים מצורפים, חשיבות, ספאם)
            setupIndicators(email, isImportant, isSpam);

            // הגדר כפתור כוכב
            setupStarButton(email);

            // הגדר כפתור More
            setupMoreButton();
        }

        private void setupSenderAvatar(Email email) {
            String senderEmail = email.sender != null ? email.sender : "?";
            String firstLetter = senderEmail.substring(0, 1).toUpperCase();
            tvSenderAvatar.setText(firstLetter);

            // צבע רקע דינמי לפי האות הראשונה
            int[] colors = {
                    Color.parseColor("#1a73e8"), // כחול
                    Color.parseColor("#34a853"), // ירוק
                    Color.parseColor("#fbbc04"), // צהוב
                    Color.parseColor("#ea4335"), // אדום
                    Color.parseColor("#9c27b0"), // סגול
                    Color.parseColor("#ff6f00"), // כתום
            };

            int colorIndex = Math.abs(firstLetter.hashCode()) % colors.length;
            tvSenderAvatar.setBackgroundColor(colors[colorIndex]);
        }

        private void setupSender(Email email) {
            String senderText = email.sender != null ? email.sender : "לא ידוע";

            // אם זה מייל ששלחתי, הצג "אני" או "לעצמי"
            if (currentUserEmail != null && currentUserEmail.equals(email.sender)) {
                if (email.recipient != null && email.recipient.equals(currentUserEmail)) {
                    senderText = "אני (לעצמי)";
                } else {
                    senderText = "אני";
                }
            }

            tvSender.setText(senderText);
        }

        private void setupSubject(Email email, boolean isDraft, boolean isSpam) {
            String subject = email.subject != null && !email.subject.trim().isEmpty()
                    ? email.subject : "(ללא נושא)";

            if (isDraft) {
                subject = "טיוטה: " + subject;
                tvSubject.setTextColor(Color.parseColor("#ff6f00")); // כתום
            } else if (isSpam) {
                tvSubject.setTextColor(Color.parseColor("#ea4335")); // אדום
            } else {
                tvSubject.setTextColor(Color.parseColor("#202124")); // שחור רגיל
            }
            tvSubject.setText(subject);
        }

        private void setupPreview(Email email) {
            String preview = email.content;
            if (preview != null && preview.length() > 100) {
                preview = preview.substring(0, 100) + "...";
            }
            tvPreview.setText(preview != null ? preview : "(ללא תוכן)");
        }

        private void setupTime(Email email) {
            if (email.timestamp != null) {
                try {
                    // פורמט זמן יפה יותר - רק שעה:דקה
                    String timeStr = email.timestamp.length() > 16
                            ? email.timestamp.substring(11, 16)
                            : email.timestamp;
                    tvTime.setText(timeStr);
                } catch (Exception e) {
                    tvTime.setText("--:--");
                }
            } else {
                tvTime.setText("--:--");
            }
        }

        private void setupLabels(Email email) {
            if (email.labels != null && !email.labels.isEmpty()) {
                // הצג רק תוויות מותאמות (לא מערכת)
                List<String> customLabels = new ArrayList<>();
                for (String label : email.labels) {
                    if (!isSystemLabel(label)) {
                        customLabels.add(label);
                    }
                }

                if (!customLabels.isEmpty()) {
                    String labelsText = customLabels.get(0); // הצג רק תווית אחת
                    if (customLabels.size() > 1) {
                        labelsText += " +" + (customLabels.size() - 1);
                    }

                    tvLabels.setText(labelsText);
                    layoutLabels.setVisibility(View.VISIBLE);
                } else {
                    layoutLabels.setVisibility(View.GONE);
                }
            } else {
                layoutLabels.setVisibility(View.GONE);
            }
        }

        private void setupIndicators(Email email, boolean isImportant, boolean isSpam) {
            boolean hasIndicators = false;

            // Spam indicator
            if (ivSpamIndicator != null) {
                if (isSpam) {
                    ivSpamIndicator.setVisibility(View.VISIBLE);
                    hasIndicators = true;
                } else {
                    ivSpamIndicator.setVisibility(View.GONE);
                }
            }

            // Important indicator
            if (ivImportant != null) {
                if (isImportant) {
                    ivImportant.setVisibility(View.VISIBLE);
                    hasIndicators = true;
                } else {
                    ivImportant.setVisibility(View.GONE);
                }
            }

            // Attachment indicator (placeholder logic)
            if (ivAttachment != null) {
                // כרגע נסתיר כי אין לנו מידע על קבצים מצורפים
                ivAttachment.setVisibility(View.GONE);
            }

            // הצג/הסתר את layout האינדיקטורים
            if (layoutIndicators != null) {
                layoutIndicators.setVisibility(hasIndicators ? View.VISIBLE : View.GONE);
            }
        }

        private void setupStarButton(Email email) {
            if (ivStar != null) {
                boolean isStarred = email.labels != null && email.labels.contains("starred");

                if (isStarred) {
                    ivStar.setImageResource(android.R.drawable.btn_star_big_on);
                    ivStar.setColorFilter(Color.parseColor("#fbbc04")); // צהוב זהב
                } else {
                    ivStar.setImageResource(android.R.drawable.btn_star_big_off);
                    ivStar.setColorFilter(Color.parseColor("#9aa0a6")); // אפור בהיר
                }
            }
        }

        private void setupMoreButton() {
            if (ivMore != null) {
                // הצג כפתור More רק אם יש deleteListener
                if (deleteListener != null) {
                    ivMore.setVisibility(View.VISIBLE);
                } else {
                    ivMore.setVisibility(View.GONE);
                }
            }
        }

        private boolean isSystemLabel(String label) {
            return label.equals("inbox") || label.equals("sent") || label.equals("drafts") ||
                    label.equals("spam") || label.equals("starred") || label.equals("trash") ||
                    label.equals("important");
        }

        private boolean isDraft(Email email) {
            return (email.labels != null && email.labels.contains("drafts"));
        }

        private boolean isSpam(Email email) {
            return (email.labels != null && email.labels.contains("spam"));
        }

        private boolean isImportant(Email email) {
            return (email.labels != null && email.labels.contains("important"));
        }
    }
}