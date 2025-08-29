package com.example.gmailapplication.repository;

import android.content.Context;
import androidx.lifecycle.MutableLiveData;

import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.LabelAPI;
import com.example.gmailapplication.shared.*;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LabelRepository {

    private final LabelAPI api;
    private final MutableLiveData<List<Label>> labelsLiveData = new MutableLiveData<>();

    public LabelRepository(Context context) {
        this.api = BackendClient.get(context).create(LabelAPI.class);
    }

    public MutableLiveData<List<Label>> getLabels() {
        return labelsLiveData;
    }

    public void fetchAllLabels() {
        api.getAllLabels().enqueue(new Callback<List<Label>>() {
            @Override
            public void onResponse(Call<List<Label>> call, Response<List<Label>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    labelsLiveData.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Label>> call, Throwable t) {
                System.out.println("Failed to fetch labels: " + t.getMessage());
            }
        });
    }

    public void createLabel(String name, Callback<Label> callback) {
        CreateLabelRequest request = new CreateLabelRequest(name);
        api.createLabel(request).enqueue(callback);
    }

    public void deleteLabel(String labelId, Callback<Void> callback) {
        api.deleteLabel(labelId).enqueue(callback);
    }

    public void tagMail(String mailId, String labelId, Callback<TagResponse> callback) {
        TagMailRequest request = new TagMailRequest(mailId, labelId);
        api.tagMail(request).enqueue(callback);
    }

    public void untagMail(String mailId, String labelId, Callback<UntagResponse> callback) {
        UntagMailRequest request = new UntagMailRequest(mailId, labelId);
        api.untagMail(request).enqueue(callback);
    }
}
