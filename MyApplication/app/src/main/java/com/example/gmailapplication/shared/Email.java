package com.example.gmailapplication.shared;

import android.text.TextUtils;
import java.util.List;

public class Email {
    public String id;
    public String sender;
    public String recipient;
    public List<String> recipients;
    public String subject;
    public String content;
    public String timestamp;
    public List<Label> labels; // רשימת תוויות כאובייקטים
    public String groupId;
    public String direction; // "sent" or "received"
    public String preview;

    // *** השדות החסרים שהאדפטר צריך ***
    public boolean isRead = false;        // האם המייל נקרא
    public boolean isStarred = false;     // האם המייל מסומן בכוכב
    public boolean isArchived = false;    // האם המייל בארכיון
    public boolean isDeleted = false;     // האם המייל נמחק

    // For recent_mails response
    public UserInfo otherParty;

    public Email() {}

    public boolean isSpam() {
        return labels != null && labels.stream()
                .anyMatch(label -> label.name != null && label.name.toLowerCase().contains("spam"));
    }

    public boolean isInbox() {
        if (labels == null || labels.isEmpty()) {
            return false;
        }

        return labels.stream()
                .anyMatch(label -> {
                    if (label == null || label.name == null) {
                        return false;
                    }
                    String labelName = label.name.toLowerCase().trim();
                    // בדיקה מדויקת יותר לתווית INBOX
                    return labelName.equals("inbox") ||
                            labelName.equals("דואר נכנס") ||
                            labelName.equals("received");
                });
    }

    public boolean isSent() {
        return "sent".equals(direction) ||
                (labels != null && labels.stream()
                        .anyMatch(label -> label.name != null && label.name.toLowerCase().contains("sent")));
    }

    public boolean isDraft() {
        return labels != null && labels.stream()
                .anyMatch(label -> label.name != null && label.name.toLowerCase().contains("draft"));
    }

    /**
     * מחזיר מחרוזת של נמענים מופרדת בפסיקים
     */
    public String getRecipientsString() {
        if (recipients != null && !recipients.isEmpty()) {
            return TextUtils.join(", ", recipients);
        }
        return recipient != null ? recipient : "";
    }

    /**
     * מחזיר מחרוזת של תוויות מופרדת בפסיקים
     */
    public String getLabelsString() {
        if (labels != null && !labels.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < labels.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(labels.get(i).name != null ? labels.get(i).name : "");
            }
            return sb.toString();
        }
        return "";
    }

    /**
     * יוצר preview קצר מהתוכן
     */
    public String getPreview() {
        if (preview != null && !preview.trim().isEmpty()) {
            return preview;
        }

        if (content != null && !content.trim().isEmpty()) {
            String cleanContent = content.replaceAll("<[^>]+>", "").trim(); // Remove HTML tags
            if (cleanContent.length() > 100) {
                return cleanContent.substring(0, 100) + "...";
            }
            return cleanContent;
        }

        return "(ללא תוכן)";
    }

    /**
     * מחזיר שם השולח או המקבל בהתאם לכיוון
     */
    public String getDisplaySender() {
        if ("sent".equals(direction)) {
            if (otherParty != null) {
                return "אל: " + otherParty.getDisplayName();
            } else if (recipients != null && !recipients.isEmpty()) {
                return "אל: " + recipients.get(0);
            } else {
                return "אל: " + (recipient != null ? recipient : "לא ידוע");
            }
        } else {
            if (otherParty != null) {
                return otherParty.getDisplayName();
            } else {
                return sender != null ? sender : "שולח לא ידוע";
            }
        }
    }

    /**
     * מחזיר נושא המייל או ברירת מחדל
     */
    public String getDisplaySubject() {
        if (subject != null && !subject.trim().isEmpty()) {
            return subject;
        }
        return "(ללא נושא)";
    }

    /**
     * בדיקה אם המייל זה טיוטה
     */
    public boolean isDraftEmail() {
        return isDraft() || "draft".equals(direction);
    }

    /**
     * בדיקה אם המייל חשוב (יש לו כוכב או סומן כחשוב)
     */
    public boolean isImportant() {
        return isStarred ||
                (labels != null && labels.stream()
                        .anyMatch(label -> label.name != null && label.name.toLowerCase().contains("important")));
    }

    /**
     * מעדכן סטטוס קריאה
     */
    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * מעדכן סטטוס כלא נקרא
     */
    public void markAsUnread() {
        this.isRead = false;
    }

    /**
     * החלפת מצב כוכב
     */
    public void toggleStar() {
        this.isStarred = !this.isStarred;
    }

    /**
     * מעבר לארכיון
     */
    public void archive() {
        this.isArchived = true;
    }

    /**
     * מחיקת המייל (העברה לפח)
     */
    public void delete() {
        this.isDeleted = true;
    }

    /**
     * שחזור מהפח
     */
    public void restore() {
        this.isDeleted = false;
        this.isArchived = false;
    }

    @Override
    public String toString() {
        return "Email{" +
                "id='" + id + '\'' +
                ", sender='" + sender + '\'' +
                ", subject='" + subject + '\'' +
                ", isRead=" + isRead +
                ", isStarred=" + isStarred +
                ", direction='" + direction + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Email email = (Email) obj;
        return id != null && id.equals(email.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}