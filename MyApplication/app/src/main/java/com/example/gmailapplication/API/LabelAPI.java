package com.example.gmailapplication.API;

import com.example.gmailapplication.shared.*;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface LabelAPI {

    // קבלת כל התוויות (מערכת + מותאמות)
    @GET("labels")
    Call<List<Label>> getAllLabels();

    // קבלת תווית לפי ID
    @GET("labels/{id}")
    Call<Label> getLabelById(@Path("id") String labelId);

    // יצירת תווית חדשה (רק מותאמות)
    @POST("labels")
    Call<Label> createLabel(@Body CreateLabelRequest request);

    // עדכון תווית (רק מותאמות)
    @PATCH("labels/{id}")
    Call<Label> updateLabel(@Path("id") String labelId, @Body UpdateLabelRequest request);

    // מחיקת תווית (רק מותאמות)
    @DELETE("labels/{id}")
    Call<Void> deleteLabel(@Path("id") String labelId);

    // חיפוש תוויות
    @GET("labels/search/{query}")
    Call<List<Label>> searchLabels(@Path("query") String query);

    // הוספת תווית למייל
    @POST("labels/tag")
    Call<TagResponse> tagMail(@Body TagMailRequest request);

    // הסרת תווית ממייל
    @POST("labels/untag")
    Call<UntagResponse> untagMail(@Body UntagMailRequest request);

    // קבלת תוויות של מייל
    @GET("labels/mail/{mailId}")
    Call<List<String>> getLabelsForMail(@Path("mailId") String mailId);

    // קבלת מיילים לפי תווית (משתמש ב-labelId)
    @GET("labels/by-label/{labelId}")
    Call<List<String>> getMailsByLabel(@Path("labelId") String labelId);
}