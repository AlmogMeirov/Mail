package com.example.gmailapplication.shared;

import java.util.List;

/**
 * מבנה התגובה החדש מהשרת (התאמה למבנה MongoDB)
 * השרת מחזיר: { message, inbox, sent, drafts, recent_mails }
 */
public class EmailResponse {
    public String message;
    public List<Email> inbox;        // מיילים בדואר נכנס
    public List<Email> sent;         // מיילים שנשלחו
    public List<Email> drafts;       // טיוטות
    public List<Email> recent_mails; // מיילים אחרונים (לתצוגה מהירה)

    public EmailResponse() {}

    /**
     * מחזיר את כל המיילים ממוזגים (לא כולל drafts)
     */
    public List<Email> getAllMails() {
        List<Email> allMails = new java.util.ArrayList<>();

        if (inbox != null) {
            allMails.addAll(inbox);
        }

        if (sent != null) {
            allMails.addAll(sent);
        }

        return allMails;
    }

    /**
     * מחזיר רק מיילי inbox
     */
    public List<Email> getInboxMails() {
        return inbox != null ? inbox : new java.util.ArrayList<>();
    }

    /**
     * מחזיר רק מיילים שנשלחו
     */
    public List<Email> getSentMails() {
        return sent != null ? sent : new java.util.ArrayList<>();
    }

    /**
     * מחזיר רק טיוטות
     */
    public List<Email> getDraftMails() {
        return drafts != null ? drafts : new java.util.ArrayList<>();
    }

    /**
     * מחזיר מיילים אחרונים (לתצוגה מהירה)
     */
    public List<Email> getRecentMails() {
        return recent_mails != null ? recent_mails : new java.util.ArrayList<>();
    }

    @Override
    public String toString() {
        int inboxCount = inbox != null ? inbox.size() : 0;
        int sentCount = sent != null ? sent.size() : 0;
        int draftsCount = drafts != null ? drafts.size() : 0;

        return "EmailResponse{" +
                "message='" + message + '\'' +
                ", inbox=" + inboxCount +
                ", sent=" + sentCount +
                ", drafts=" + draftsCount +
                '}';
    }
}