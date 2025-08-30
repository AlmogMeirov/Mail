package com.example.gmailapplication.API;

import com.example.gmailapplication.shared.Email;
import com.example.gmailapplication.shared.EmailResponse;
import com.example.gmailapplication.shared.SendEmailRequest;
import com.example.gmailapplication.shared.SendEmailResponse;
import com.example.gmailapplication.shared.UpdateLabelsRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface EmailAPI {

    // Response class
    class EmailListResponse {
        public String message;
        public List<Email> inbox;
        public List<Email> sent;
        public List<Email> drafts;
        public List<Email> recent_mails;
    }

    // כל המיילים (inbox, sent, drafts) - מבנה תגובה חדש מהשרת
    @GET("mails")
    Call<EmailListResponse> getEmails();

    // שליחת מייל
    @POST("mails")
    Call<Void> sendEmail(@Body SendEmailRequest request);
    // מייל ספציפי
    @GET("mails/{id}")
    Call<Email> getEmailById(@Path("id") String emailId);

    // חיפוש מיילים
    @GET("mails/search")
    Call<List<Email>> searchEmails(@Query("q") String query);

    // עדכון תוויות מייל (התאמה לשרת החדש)
    @PATCH("mails/{id}/labels")
    Call<Void> updateEmailLabels(@Path("id") String emailId, @Body UpdateLabelsRequest request);

    // מחיקת מייל
    @DELETE("mails/{id}")
    Call<Void> deleteEmail(@Path("id") String emailId);

    // === API חדש מהשרת ===

    // ארכוב מייל (הסרת תווית inbox)
    @POST("mails/{id}/archive")
    Call<Void> archiveEmail(@Path("id") String emailId);

    // הוספה/הסרה של כוכב
    @POST("mails/{id}/star")
    Call<Void> toggleStarEmail(@Path("id") String emailId);

    // הוספת תווית יחידה
    @POST("mails/{id}/add-label")
    Call<Void> addLabelToEmail(@Path("id") String emailId, @Body AddLabelRequest request);

    // הסרת תווית יחידה
    @POST("mails/{id}/remove-label")
    Call<Void> removeLabelFromEmail(@Path("id") String emailId, @Body RemoveLabelRequest request);

    // מיילים לפי תווית (מהשרת החדש)
    @GET("mails/label/{label}")
    Call<MailsByLabelResponse> getMailsByLabel(@Path("label") String labelName);

    // מיילים מסומנים בכוכב
    @GET("mails/starred")
    Call<MailsByLabelResponse> getStarredMails();

    // מיילי ספאם
    @GET("mails/spam")
    Call<MailsByLabelResponse> getSpamMails();

    // === טיוטות ===

    // יצירת טיוטה
    @POST("mails/drafts")
    Call<DraftResponse> createDraft(@Body CreateDraftRequest request);

    // שליחת טיוטה
    @POST("mails/drafts/{id}/send")
    Call<SendEmailResponse> sendDraft(@Path("id") String draftId);

    // קבלת טיוטות
    @GET("mails/drafts")
    Call<DraftsResponse> getDrafts();

    // === Request/Response classes ===

    class AddLabelRequest {
        public String labelName;

        public AddLabelRequest(String labelName) {
            this.labelName = labelName;
        }
    }

    class RemoveLabelRequest {
        public String labelName;

        public RemoveLabelRequest(String labelName) {
            this.labelName = labelName;
        }
    }

    class CreateDraftRequest {
        public String sender;
        public String recipient;
        public List<String> recipients;
        public String subject;
        public String content;

        public CreateDraftRequest(String sender, List<String> recipients, String subject, String content) {
            this.sender = sender;
            this.recipients = recipients;
            this.recipient = recipients != null && !recipients.isEmpty() ? recipients.get(0) : null;
            this.subject = subject;
            this.content = content;
        }
    }

    class MailsByLabelResponse {
        public String message;
        public List<Email> mails;
    }

    class DraftResponse {
        public String message;
        public Email draft;
    }

    class DraftsResponse {
        public String message;
        public List<Email> drafts;
    }
}