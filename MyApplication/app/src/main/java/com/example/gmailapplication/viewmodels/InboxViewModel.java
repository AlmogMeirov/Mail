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

    // תמיכה בסינון
    private MutableLiveData<String> currentFilter = new MutableLiveData<>("inbox");
    private MutableLiveData<String> currentFilterDisplayName = new MutableLiveData<>("דואר נכנס");

    public InboxViewModel(Application app) {
        super(app);
        repository = new EmailRepository(app);
    }

    // Getters קיימים
    public MutableLiveData<List<Email>> getEmails() { return emails; }
    public MutableLiveData<Boolean> getIsLoading() { return isLoading; }
    public MutableLiveData<String> getError() { return error; }

    // Getters חדשים לסינון
    public MutableLiveData<String> getCurrentFilter() { return currentFilter; }
    public MutableLiveData<String> getCurrentFilterDisplayName() { return currentFilterDisplayName; }

    // הפונקציה המקורית - נשארת ללא שינוי
    public void loadEmails() {
        isLoading.setValue(true);
        error.setValue(null);
        currentFilter.setValue("inbox");
        currentFilterDisplayName.setValue("דואר נכנס");

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

    // פונקציה חדשה לטעינת מיילים לפי תווית
    public void loadEmailsByLabel(String labelName, String displayName) {
        isLoading.setValue(true);
        error.setValue(null);
        currentFilter.setValue(labelName);
        currentFilterDisplayName.setValue(displayName);

        System.out.println("=== LOADING EMAILS BY LABEL ===");
        System.out.println("Label: " + labelName);
        System.out.println("Display Name: " + displayName);

        repository.getEmailsByLabel(labelName).enqueue(new Callback<EmailAPI.MailsByLabelResponse>() {
            @Override
            public void onResponse(Call<EmailAPI.MailsByLabelResponse> call, Response<EmailAPI.MailsByLabelResponse> response) {
                isLoading.setValue(false);
                System.out.println("=== LABEL RESPONSE DEBUG ===");
                System.out.println("Response successful: " + response.isSuccessful());
                System.out.println("Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    List<Email> labelMails = response.body().mails;
                    System.out.println("Label emails count: " + (labelMails != null ? labelMails.size() : 0));
                    emails.setValue(labelMails);
                    System.out.println("emails.setValue() called for label: " + labelName);
                } else {
                    System.out.println("Response error or null body for label: " + labelName);
                    error.setValue("שגיאה בטעינת מיילים עבור " + displayName);
                }
                System.out.println("===========================");
            }

            @Override
            public void onFailure(Call<EmailAPI.MailsByLabelResponse> call, Throwable t) {
                isLoading.setValue(false);
                System.err.println("Network error loading emails by label: " + t.getMessage());
                error.setValue("שגיאת רשת בטעינת " + displayName);
            }
        });
    }

    // פונקציה נוחות לסינונים מיוחדים
    public void loadStarredEmails() {
        isLoading.setValue(true);
        error.setValue(null);
        currentFilter.setValue("starred");
        currentFilterDisplayName.setValue("מסומן בכוכב");

        repository.getStarredEmails().enqueue(new Callback<EmailAPI.MailsByLabelResponse>() {
            @Override
            public void onResponse(Call<EmailAPI.MailsByLabelResponse> call, Response<EmailAPI.MailsByLabelResponse> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    emails.setValue(response.body().mails);
                } else {
                    error.setValue("שגיאה בטעינת מיילים מסומנים בכוכב");
                }
            }

            @Override
            public void onFailure(Call<EmailAPI.MailsByLabelResponse> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue("שגיאת רשת");
            }
        });
    }

    public void loadSpamEmails() {
        isLoading.setValue(true);
        error.setValue(null);
        currentFilter.setValue("spam");
        currentFilterDisplayName.setValue("ספאם");

        repository.getSpamEmails().enqueue(new Callback<EmailAPI.MailsByLabelResponse>() {
            @Override
            public void onResponse(Call<EmailAPI.MailsByLabelResponse> call, Response<EmailAPI.MailsByLabelResponse> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    emails.setValue(response.body().mails);
                } else {
                    error.setValue("שגיאה בטעינת מיילי ספאם");
                }
            }

            @Override
            public void onFailure(Call<EmailAPI.MailsByLabelResponse> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue("שגיאת רשת");
            }
        });
    }

    // רענון הסינון הנוכחי
    public void refreshCurrentFilter() {
        String filter = currentFilter.getValue();
        if (filter == null || filter.equals("inbox")) {
            loadEmails();
        } else if (filter.equals("starred")) {
            loadStarredEmails();
        } else if (filter.equals("spam")) {
            loadSpamEmails();
        } else {
            loadEmailsByLabel(filter, currentFilterDisplayName.getValue());
        }
    }
}