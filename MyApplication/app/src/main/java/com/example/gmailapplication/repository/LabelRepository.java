package com.example.gmailapplication.repository;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.LabelAPI;
import com.example.gmailapplication.database.AppDB;
import com.example.gmailapplication.database.dao.LabelDao;
import com.example.gmailapplication.database.entities.LabelEntity;
import com.example.gmailapplication.shared.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LabelRepository {

    private static LabelRepository instance;
    private final LabelDao labelDao;
    private final LabelAPI labelAPI;
    private final Executor executor;
    private final Context context;

    // LiveData
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> lastError = new MutableLiveData<>("");

    private LabelRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDB database = AppDB.getInstance(context);
        labelDao = database.labelDao();
        labelAPI = BackendClient.get(context).create(LabelAPI.class);
        executor = Executors.newFixedThreadPool(2);

        // יצירת תוויות מערכת בפעם הראשונה
        initializeSystemLabels();
    }

    public static synchronized LabelRepository getInstance(Context context) {
        if (instance == null) {
            instance = new LabelRepository(context);
        }
        return instance;
    }

    // יצירת תוויות מערכת
    private void initializeSystemLabels() {
        executor.execute(() -> {
            if (labelDao.getLabelsCount() == 0) {
                List<LabelEntity> systemLabels = new ArrayList<>();
                systemLabels.add(new LabelEntity("system_inbox", "Inbox", true));
                systemLabels.add(new LabelEntity("system_sent", "Sent", true));
                systemLabels.add(new LabelEntity("system_spam", "Spam", true));
                systemLabels.add(new LabelEntity("system_trash", "Trash", true));
                systemLabels.add(new LabelEntity("system_drafts", "Drafts", true));

                // סמן אותם כתוויות מערכת
                for (LabelEntity label : systemLabels) {
                    label.isSystemLabel = true;
                    label.isDefault = true;
                }

                labelDao.insertLabels(systemLabels);
            }
        });
    }

    // LiveData getters
    public LiveData<List<LabelEntity>> getAllLabels() {
        return labelDao.getAllLabels();
    }

    public LiveData<List<LabelEntity>> getSystemLabels() {
        return labelDao.getSystemLabels();
    }

    public LiveData<List<LabelEntity>> getCustomLabels() {
        return labelDao.getCustomLabels();
    }

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    public LiveData<String> getLastError() {
        return lastError;
    }

    // טעינת תוויות מהשרת
    public void fetchLabelsFromServer() {
        isLoading.setValue(true);

        String authToken = getAuthToken();
        if (authToken.isEmpty()) {
            isLoading.setValue(false);
            lastError.setValue("No authentication token available");
            return;
        }

        labelAPI.getAllLabels(authToken).enqueue(new Callback<List<Label>>() {
            @Override
            public void onResponse(Call<List<Label>> call, Response<List<Label>> response) {
                isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<Label> serverLabels = response.body();

                    // המר לLabelEntity ושמור במסד הנתונים
                    executor.execute(() -> {
                        List<LabelEntity> labelEntities = new ArrayList<>();
                        String userId = getCurrentUserId();

                        for (Label serverLabel : serverLabels) {
                            LabelEntity entity = new LabelEntity(
                                    serverLabel.id != null ? serverLabel.id : generateTempId(),
                                    serverLabel.name
                            );
                            entity.userId = userId;
                            entity.isSystemLabel = isSystemLabel(serverLabel.name);
                            labelEntities.add(entity);
                        }

                        // שמור רק תוויות מותאמות אישית מהשרת
                        for (LabelEntity entity : labelEntities) {
                            if (!entity.isSystemLabel) {
                                labelDao.insertLabel(entity);
                            }
                        }
                    });
                } else {
                    lastError.setValue("Failed to fetch labels: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Label>> call, Throwable t) {
                isLoading.setValue(false);
                lastError.setValue("Network error: " + t.getMessage());
            }
        });
    }

    // יצירת תווית חדשה
    public void createLabel(String labelName, String color, CreateLabelCallback callback) {
        if (labelName == null || labelName.trim().isEmpty()) {
            callback.onError("Label name cannot be empty");
            return;
        }

        // בדיקה שהתווית לא קיימת כבר
        executor.execute(() -> {
            if (labelDao.isLabelExists(labelName) > 0) {
                callback.onError("Label already exists");
                return;
            }

            // יצירה במסד הנתונים המקומי קודם
            LabelEntity localLabel = new LabelEntity(generateTempId(), labelName);
            localLabel.userId = getCurrentUserId();
            localLabel.color = color; // שמור צבע מקומית
            localLabel.isSystemLabel = false;
            localLabel.needsSync = true;
            localLabel.syncStatus = "pending";

            labelDao.insertLabel(localLabel);

            // שליחה לשרת (בלי צבע - רק שם)
            String authToken = getAuthToken();
            CreateLabelRequest request = new CreateLabelRequest(labelName);
            labelAPI.createLabel(authToken, request).enqueue(new Callback<Label>() {
                @Override
                public void onResponse(Call<Label> call, Response<Label> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Label serverLabel = response.body();

                        // עדכון המסד הנתונים עם ה-ID האמיתי מהשרת
                        executor.execute(() -> {
                            localLabel.id = serverLabel.id;
                            localLabel.needsSync = false;
                            localLabel.syncStatus = "synced";
                            // שמור את הצבע מקומית - השרת לא יודע עליו
                            labelDao.updateLabel(localLabel);
                        });

                        callback.onSuccess(serverLabel);
                    } else {
                        // שגיאה - סמן במסד הנתונים
                        executor.execute(() -> {
                            localLabel.syncStatus = "failed";
                            labelDao.updateLabel(localLabel);
                        });
                        callback.onError("Failed to create label on server: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<Label> call, Throwable t) {
                    executor.execute(() -> {
                        localLabel.syncStatus = "failed";
                        labelDao.updateLabel(localLabel);
                    });
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        });
    }

    // גרסה ישנה עבור תאימות לאחור
    public void createLabel(String labelName, CreateLabelCallback callback) {
        createLabel(labelName, "#2196F3", callback); // צבע ברירת מחדל
    }

    // מחיקת תווית
    public void deleteLabel(String labelId, DeleteLabelCallback callback) {
        executor.execute(() -> {
            LabelEntity label = labelDao.getLabelById(labelId);
            if (label != null && label.isSystemLabel) {
                callback.onError("Cannot delete system labels");
                return;
            }

            // מחק ממסד הנתונים המקומי
            if (label != null) {
                labelDao.deleteLabel(label);
            }

            // מחק מהשרת
            String authToken = getAuthToken();
            labelAPI.deleteLabel(authToken, labelId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Failed to delete label on server");
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        });
    }

    // שיוך תווית למייל
    public void tagEmail(String emailId, String labelId) {
        String authToken = getAuthToken();
        TagMailRequest request = new TagMailRequest();
        request.mailId = emailId;
        request.labelId = labelId;

        labelAPI.tagMail(authToken, request).enqueue(new Callback<TagResponse>() {
            @Override
            public void onResponse(Call<TagResponse> call, Response<TagResponse> response) {
                // עדכן במסד הנתונים המקומי אם צריך
            }

            @Override
            public void onFailure(Call<TagResponse> call, Throwable t) {
                lastError.setValue("Failed to tag email: " + t.getMessage());
            }
        });
    }

    // Helper methods
    private String getAuthToken() {
        SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("auth_token", "");

        // הוסף Bearer prefix אם אין
        if (!token.isEmpty() && !token.startsWith("Bearer ")) {
            token = "Bearer " + token;
        }

        return token;
    }

    private String getCurrentUserId() {
        SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        return prefs.getString("user_id", "");
    }

    private boolean isSystemLabel(String labelName) {
        return "inbox".equalsIgnoreCase(labelName) ||
                "sent".equalsIgnoreCase(labelName) ||
                "spam".equalsIgnoreCase(labelName) ||
                "trash".equalsIgnoreCase(labelName) ||
                "drafts".equalsIgnoreCase(labelName);
    }

    private String generateTempId() {
        return "temp_" + System.currentTimeMillis();
    }

    // Callback interfaces
    public interface CreateLabelCallback {
        void onSuccess(Label label);
        void onError(String error);
    }

    public interface DeleteLabelCallback {
        void onSuccess();
        void onError(String error);
    }
}