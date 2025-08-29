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
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface EmailAPI {

    @GET("mails")
    Call<EmailResponse> getEmails();

    @POST("mails")
    Call<SendEmailResponse> sendEmail(@Body SendEmailRequest request);

    @GET("mails/{id}")
    Call<Email> getEmailById(@Path("id") String emailId);

    @GET("mails/search")
    Call<List<Email>> searchEmails(@Query("q") String query);

    @PATCH("mails/{id}/labels")
    Call<Void> updateEmailLabels(@Path("id") String emailId,
                                 @Body UpdateLabelsRequest request);

    @DELETE("mails/{id}")
    Call<Void> deleteEmail(@Path("id") String emailId);
}