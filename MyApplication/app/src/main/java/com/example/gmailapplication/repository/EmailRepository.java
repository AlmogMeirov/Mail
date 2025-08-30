package com.example.gmailapplication.repository;

import android.content.Context;
import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.EmailAPI;
import com.example.gmailapplication.shared.SendEmailRequest;
import com.example.gmailapplication.shared.SendEmailResponse;
import retrofit2.Call;

public class EmailRepository {
    private EmailAPI api;

    public EmailRepository(Context context) {
        this.api = BackendClient.get(context).create(EmailAPI.class);
    }

    public Call<EmailAPI.EmailListResponse> getEmails() {
        return api.getEmails();
    }

    public Call<Void> sendEmail(SendEmailRequest request) {
        return api.sendEmail(request);
    }
}