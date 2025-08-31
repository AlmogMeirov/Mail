package com.example.gmailapplication.repository;

import android.content.Context;
import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.EmailAPI;
import retrofit2.Call;

public class EmailRepository {
    private EmailAPI emailAPI;

    public EmailRepository(Context context) {
        emailAPI = BackendClient.get(context).create(EmailAPI.class);
    }

    // חשוף את ה-API עבור ViewModel
    public EmailAPI getEmailAPI() {
        return emailAPI;
    }

    // כל המיילים (inbox, sent, drafts)
    public Call<EmailAPI.EmailListResponse> getEmails() {
        return emailAPI.getEmails();
    }

    // מיילים לפי תווית
    public Call<EmailAPI.MailsByLabelResponse> getEmailsByLabel(String labelName) {
        return emailAPI.getMailsByLabel(labelName);
    }

    // מיילים מסומנים בכוכב
    public Call<EmailAPI.MailsByLabelResponse> getStarredEmails() {
        return emailAPI.getStarredMails();
    }

    // מיילי ספאם
    public Call<EmailAPI.MailsByLabelResponse> getSpamEmails() {
        return emailAPI.getSpamMails();
    }

    // חיפוש מיילים
    public Call<java.util.List<com.example.gmailapplication.shared.Email>> searchEmails(String query) {
        return emailAPI.searchEmails(query);
    }

    // שליחת מייל
    public Call<Void> sendEmail(com.example.gmailapplication.shared.SendEmailRequest request) {
        return emailAPI.sendEmail(request);
    }

    // קבלת מייל לפי ID
    public Call<com.example.gmailapplication.shared.Email> getEmailById(String emailId) {
        return emailAPI.getEmailById(emailId);
    }
}