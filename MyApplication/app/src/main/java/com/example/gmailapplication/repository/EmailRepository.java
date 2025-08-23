// EmailRepository.java
package com.example.gmailapplication.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.EmailAPI;
import com.example.gmailapplication.database.AppDB;
import com.example.gmailapplication.database.entities.EmailEntity;
import com.example.gmailapplication.local.EmailDao;
import com.example.gmailapplication.shared.*;
import com.example.gmailapplication.utils.EmailRefreshManager;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmailRepository {
    private static final String TAG = "EmailRepository";

    private final Context appCtx;
    private final EmailAPI api;
    private final EmailDao dao;
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
    private final EmailRefreshManager refreshManager;

    // LiveData for different email categories
    private final MediatorLiveData<List<EmailEntity>> allEmails = new MediatorLiveData<>();
    private final MediatorLiveData<List<EmailEntity>> inboxEmails = new MediatorLiveData<>();
    private final MediatorLiveData<List<EmailEntity>> sentEmails = new MediatorLiveData<>();
    private final MediatorLiveData<List<EmailEntity>> drafts = new MediatorLiveData<>();

    // Status tracking
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> lastError = new MutableLiveData<>();
    private final MutableLiveData<Long> lastSyncTime = new MutableLiveData<>(0L);

    // Singleton pattern
    private static EmailRepository instance;

    public static EmailRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (EmailRepository.class) {
                if (instance == null) {
                    instance = new EmailRepository(context);
                }
            }
        }
        return instance;
    }

    private EmailRepository(Context context) {
        this.appCtx = context.getApplicationContext();
        this.api = BackendClient.get(appCtx).create(EmailAPI.class);
        this.dao = AppDB.get(appCtx).emailDao();
        this.refreshManager = EmailRefreshManager.getInstance();

        setupLiveDataSources();
        setupAutoSync();
    }

    private void setupLiveDataSources() {
        // Setup mediator live data to combine local data with loading states
        allEmails.addSource(dao.observeAllEmails(), emails -> {
            allEmails.setValue(emails);
        });

        inboxEmails.addSource(dao.observeInboxEmails(), emails -> {
            inboxEmails.setValue(emails);
        });

        sentEmails.addSource(dao.observeSentEmails(), emails -> {
            sentEmails.setValue(emails);
        });

        drafts.addSource(dao.observeDrafts(), emails -> {
            drafts.setValue(emails);
        });
    }

    private void setupAutoSync() {
        refreshManager.addRefreshListener(new EmailRefreshManager.RefreshListener() {
            @Override
            public void onRefreshRequested() {
                fetchEmailsFromServer();
            }

            @Override
            public void onRefreshStarted() {
                isLoading.postValue(true);
            }

            @Override
            public void onRefreshCompleted(boolean success) {
                isLoading.postValue(false);
                if (success) {
                    lastSyncTime.postValue(System.currentTimeMillis());
                }
            }
        });
    }

    // ===== Public API =====

    public LiveData<List<EmailEntity>> getAllEmails() {
        return allEmails;
    }

    public LiveData<List<EmailEntity>> getInboxEmails() {
        return inboxEmails;
    }

    public LiveData<List<EmailEntity>> getSentEmails() {
        return sentEmails;
    }

    public LiveData<List<EmailEntity>> getDrafts() {
        return drafts;
    }

    public LiveData<List<EmailEntity>> getStarredEmails() {
        return dao.observeStarredEmails();
    }

    public LiveData<List<EmailEntity>> getArchivedEmails() {
        return dao.observeArchivedEmails();
    }

    public LiveData<List<EmailEntity>> getUnreadEmails() {
        return dao.observeUnreadEmails();
    }

    public LiveData<Integer> getUnreadCount() {
        return dao.getUnreadCount();
    }

    public LiveData<Integer> getDraftCount() {
        return dao.getDraftCount();
    }

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    public LiveData<String> getLastError() {
        return lastError;
    }

    public LiveData<Long> getLastSyncTime() {
        return lastSyncTime;
    }

    // ===== Network Operations =====

    public void fetchEmailsFromServer() {
        Log.d(TAG, "Fetching emails from server");
        isLoading.postValue(true);

        api.getEmails().enqueue(new Callback<EmailResponse>() {
            @Override
            public void onResponse(Call<EmailResponse> call, Response<EmailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processEmailResponse(response.body());
                    refreshManager.onRefreshCompleted(true);
                    lastError.postValue(null);
                } else {
                    String error = "Failed to fetch emails: " + response.code();
                    Log.e(TAG, error);
                    lastError.postValue(error);
                    refreshManager.onRefreshCompleted(false);
                }
                isLoading.postValue(false);
            }

            @Override
            public void onFailure(Call<EmailResponse> call, Throwable t) {
                String error = "Network error: " + t.getMessage();
                Log.e(TAG, error);
                lastError.postValue(error);
                refreshManager.onRefreshCompleted(false);
                isLoading.postValue(false);
            }
        });
    }

    public void sendEmail(SendEmailRequest request, Callback<SendEmailResponse> callback) {
        Log.d(TAG, "Sending email");

        // Save as draft first
        EmailEntity draft = createDraftEntity(request);
        backgroundExecutor.execute(() -> {
            dao.insertEmail(draft);
        });

        // Send to server
        api.sendEmail(request).enqueue(new Callback<SendEmailResponse>() {
            @Override
            public void onResponse(Call<SendEmailResponse> call, Response<SendEmailResponse> response) {
                if (response.isSuccessful()) {
                    // Convert draft to sent email
                    backgroundExecutor.execute(() -> {
                        dao.convertDraftToSent(draft.id);
                        dao.markAsSynced(draft.id);
                    });

                    // Trigger fast refresh to get updated email list
                    refreshManager.enableFastRefresh();
                } else {
                    // Mark draft as failed
                    backgroundExecutor.execute(() -> {
                        dao.updateSyncStatus(draft.id, "failed");
                    });
                }

                if (callback != null) {
                    callback.onResponse(call, response);
                }
            }

            @Override
            public void onFailure(Call<SendEmailResponse> call, Throwable t) {
                // Mark draft as failed
                backgroundExecutor.execute(() -> {
                    dao.updateSyncStatus(draft.id, "failed");
                });

                if (callback != null) {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    // ===== Local Operations =====

    public void markAsRead(String emailId) {
        backgroundExecutor.execute(() -> {
            dao.markAsRead(emailId, System.currentTimeMillis());
            syncEmailWithServer(emailId);
        });
    }

    public void markAsUnread(String emailId) {
        backgroundExecutor.execute(() -> {
            dao.markAsUnread(emailId, System.currentTimeMillis());
            syncEmailWithServer(emailId);
        });
    }

    public void toggleStar(String emailId) {
        backgroundExecutor.execute(() -> {
            EmailEntity email = dao.getEmailById(emailId);
            if (email != null) {
                dao.setStarred(emailId, !email.isStarred, System.currentTimeMillis());
                syncEmailWithServer(emailId);
            }
        });
    }

    public void archiveEmail(String emailId) {
        backgroundExecutor.execute(() -> {
            dao.setArchived(emailId, true, System.currentTimeMillis());
            syncEmailWithServer(emailId);
        });
    }

    public void unarchiveEmail(String emailId) {
        backgroundExecutor.execute(() -> {
            dao.setArchived(emailId, false, System.currentTimeMillis());
            syncEmailWithServer(emailId);
        });
    }

    public void deleteEmail(String emailId) {
        backgroundExecutor.execute(() -> {
            dao.markAsDeleted(emailId, System.currentTimeMillis());
            syncEmailWithServer(emailId);
        });
    }

    public void restoreEmail(String emailId) {
        backgroundExecutor.execute(() -> {
            dao.restoreFromTrash(emailId, System.currentTimeMillis());
            syncEmailWithServer(emailId);
        });
    }

    // ===== Draft Operations =====

    public void saveDraft(SendEmailRequest request) {
        backgroundExecutor.execute(() -> {
            EmailEntity draft = createDraftEntity(request);
            draft.isDraft = true;
            draft.draftId = "draft_" + System.currentTimeMillis();
            dao.insertEmail(draft);
        });
    }

    public void updateDraft(String draftId, SendEmailRequest request) {
        backgroundExecutor.execute(() -> {
            EmailEntity existingDraft = dao.getDraftById(draftId);
            if (existingDraft != null) {
                updateEntityFromRequest(existingDraft, request);
                existingDraft.lastModified = System.currentTimeMillis();
                dao.updateEmail(existingDraft);
            }
        });
    }

    public void deleteDraft(String draftId) {
        backgroundExecutor.execute(() -> {
            dao.deleteDraft(draftId);
        });
    }

    // ===== Search Operations =====

    public LiveData<List<EmailEntity>> searchEmails(String query) {
        return dao.searchEmails(query);
    }

    public LiveData<List<EmailEntity>> getEmailsFromSender(String senderEmail) {
        return dao.getEmailsFromSender(senderEmail);
    }

    public LiveData<List<EmailEntity>> getEmailsInTimeRange(String startTime, String endTime) {
        return dao.getEmailsInTimeRange(startTime, endTime);
    }

    // ===== Sync Operations =====

    public void syncPendingChanges() {
        backgroundExecutor.execute(() -> {
            List<EmailEntity> pendingEmails = dao.getEmailsNeedingSync();
            for (EmailEntity email : pendingEmails) {
                syncEmailWithServer(email.id);
            }
        });
    }

    private void syncEmailWithServer(String emailId) {
        // TODO: Implement sync with server API
        // This would involve calling appropriate API endpoints for each action
        // For now, we'll just mark as synced after a delay
        backgroundExecutor.execute(() -> {
            try {
                Thread.sleep(1000); // Simulate network delay
                dao.markAsSynced(emailId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    // ===== Maintenance Operations =====

    public void cleanup() {
        backgroundExecutor.execute(() -> {
            // Clean up old synced emails (older than 30 days)
            long cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
            dao.cleanupOldSyncedEmails(cutoffTime);

            // Permanently delete trash emails older than 7 days
            long trashCutoff = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
            dao.permanentlyDeleteOldTrashEmails(trashCutoff);
        });
    }

    public void clearAllLocalData() {
        backgroundExecutor.execute(() -> {
            dao.deleteAllEmails();
        });
    }

    // ===== Helper Methods =====

    private void processEmailResponse(EmailResponse response) {
        backgroundExecutor.execute(() -> {
            // Convert network emails to entities and save
            if (response.inbox != null) {
                List<EmailEntity> inboxEntities = EmailMapper.toEntities(response.inbox);
                // Mark as inbox direction
                for (EmailEntity entity : inboxEntities) {
                    entity.direction = "received";
                }
                dao.insertEmails(inboxEntities);
            }

            if (response.sent != null) {
                List<EmailEntity> sentEntities = EmailMapper.toEntities(response.sent);
                // Mark as sent direction
                for (EmailEntity entity : sentEntities) {
                    entity.direction = "sent";
                }
                dao.insertEmails(sentEntities);
            }

            if (response.recent_mails != null) {
                List<EmailEntity> recentEntities = EmailMapper.toEntities(response.recent_mails);
                dao.insertEmails(recentEntities);
            }
        });
    }

    private EmailEntity createDraftEntity(SendEmailRequest request) {
        return EmailMapper.requestToEntity(request, "draft_" + System.currentTimeMillis());
    }

    private void updateEntityFromRequest(EmailEntity entity, SendEmailRequest request) {
        EmailEntity tempEntity = EmailMapper.requestToEntity(request, entity.id);
        if (tempEntity != null) {
            entity.sender = tempEntity.sender;
            entity.recipient = tempEntity.recipient;
            entity.recipients = tempEntity.recipients;
            entity.subject = tempEntity.subject;
            entity.content = tempEntity.content;
            entity.labels = tempEntity.labels;
            entity.lastModified = System.currentTimeMillis();
        }
    }
}