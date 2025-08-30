package com.example.gmailapplication.viewmodels;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.example.gmailapplication.API.EmailAPI;
import com.example.gmailapplication.repository.EmailRepository;
import com.example.gmailapplication.shared.Email;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InboxViewModel extends AndroidViewModel {
    private EmailRepository repository;
    private MutableLiveData<List<Email>> emails = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> error = new MutableLiveData<>();

    public InboxViewModel(Application app) {
        super(app);
        repository = new EmailRepository(app);
    }

    public MutableLiveData<List<Email>> getEmails() { return emails; }
    public MutableLiveData<Boolean> getIsLoading() { return isLoading; }
    public MutableLiveData<String> getError() { return error; }

    public void loadEmails() {
        isLoading.setValue(true);
        error.setValue(null);

        repository.getEmails().enqueue(new Callback<EmailAPI.EmailListResponse>() {
            @Override
            public void onResponse(Call<EmailAPI.EmailListResponse> call, Response<EmailAPI.EmailListResponse> response) {
                isLoading.setValue(false);
                System.out.println("=== INBOX RESPONSE DEBUG ===");
                System.out.println("Response successful: " + response.isSuccessful());
                if (response.isSuccessful() && response.body() != null) {
                    System.out.println("Inbox emails count: " + response.body().inbox.size());
                    emails.setValue(response.body().inbox);
                    System.out.println("emails.setValue() called");
                } else {
                    System.out.println("Response error or null body");
                    error.setValue("שגיאה בטעינת מיילים");
                }
                System.out.println("========================");
            }

            @Override
            public void onFailure(Call<EmailAPI.EmailListResponse> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue("שגיאת רשת");
            }
        });
    }
}