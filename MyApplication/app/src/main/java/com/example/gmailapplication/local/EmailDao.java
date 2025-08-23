package com.example.gmailapplication.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.gmailapplication.database.entities.EmailEntity;

import java.util.List;

@Dao
public interface EmailDao {

    // ===== צפייה בנתונים (LiveData) =====

    @Query("SELECT * FROM emails WHERE isDeleted = 0 ORDER BY timestamp DESC")
    LiveData<List<EmailEntity>> observeAllEmails();

    @Query("SELECT * FROM emails WHERE isDeleted = 0 AND direction = 'received' ORDER BY timestamp DESC")
    LiveData<List<EmailEntity>> observeInboxEmails();

    @Query("SELECT * FROM emails WHERE isDeleted = 0 AND direction = 'sent' ORDER BY timestamp DESC")
    LiveData<List<EmailEntity>> observeSentEmails();

    @Query("SELECT * FROM emails WHERE isDeleted = 0 AND isDraft = 1 ORDER BY lastModified DESC")
    LiveData<List<EmailEntity>> observeDrafts();

    @Query("SELECT * FROM emails WHERE isDeleted = 0 AND isStarred = 1 ORDER BY timestamp DESC")
    LiveData<List<EmailEntity>> observeStarredEmails();

    @Query("SELECT * FROM emails WHERE isDeleted = 0 AND isArchived = 1 ORDER BY timestamp DESC")
    LiveData<List<EmailEntity>> observeArchivedEmails();

    @Query("SELECT * FROM emails WHERE isDeleted = 0 AND isRead = 0 ORDER BY timestamp DESC")
    LiveData<List<EmailEntity>> observeUnreadEmails();

    @Query("SELECT * FROM emails WHERE isDeleted = 1 ORDER BY lastModified DESC")
    LiveData<List<EmailEntity>> observeDeletedEmails();

    // ===== קבלת נתונים סנכרונית =====

    @Query("SELECT * FROM emails WHERE id = :emailId")
    EmailEntity getEmailById(String emailId);

    @Query("SELECT * FROM emails WHERE isDeleted = 0 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    List<EmailEntity> getEmailsPaginated(int limit, int offset);

    @Query("SELECT * FROM emails WHERE needsSync = 1")
    List<EmailEntity> getEmailsNeedingSync();

    @Query("SELECT * FROM emails WHERE syncStatus = :status")
    List<EmailEntity> getEmailsBySyncStatus(String status);

    @Query("SELECT COUNT(*) FROM emails WHERE isDeleted = 0 AND isRead = 0")
    LiveData<Integer> getUnreadCount();

    @Query("SELECT COUNT(*) FROM emails WHERE isDraft = 1")
    LiveData<Integer> getDraftCount();

    // ===== חיפוש =====

    @Query("SELECT * FROM emails WHERE isDeleted = 0 AND " +
            "(subject LIKE '%' || :query || '%' OR " +
            "content LIKE '%' || :query || '%' OR " +
            "sender LIKE '%' || :query || '%' OR " +
            "recipient LIKE '%' || :query || '%') " +
            "ORDER BY timestamp DESC")
    LiveData<List<EmailEntity>> searchEmails(String query);

    @Query("SELECT * FROM emails WHERE isDeleted = 0 AND sender = :senderEmail ORDER BY timestamp DESC")
    LiveData<List<EmailEntity>> getEmailsFromSender(String senderEmail);

    @Query("SELECT * FROM emails WHERE isDeleted = 0 AND " +
            "timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    LiveData<List<EmailEntity>> getEmailsInTimeRange(String startTime, String endTime);

    // ===== הוספה ועדכון =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEmail(EmailEntity email);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEmails(List<EmailEntity> emails);

    @Update
    void updateEmail(EmailEntity email);

    @Update
    void updateEmails(List<EmailEntity> emails);

    // ===== פעולות ספציפיות =====

    @Query("UPDATE emails SET isRead = 1, lastModified = :timestamp, needsSync = 1 WHERE id = :emailId")
    void markAsRead(String emailId, long timestamp);

    @Query("UPDATE emails SET isRead = 0, lastModified = :timestamp, needsSync = 1 WHERE id = :emailId")
    void markAsUnread(String emailId, long timestamp);

    @Query("UPDATE emails SET isStarred = :starred, lastModified = :timestamp, needsSync = 1 WHERE id = :emailId")
    void setStarred(String emailId, boolean starred, long timestamp);

    @Query("UPDATE emails SET isArchived = :archived, lastModified = :timestamp, needsSync = 1 WHERE id = :emailId")
    void setArchived(String emailId, boolean archived, long timestamp);

    @Query("UPDATE emails SET isDeleted = 1, lastModified = :timestamp, needsSync = 1 WHERE id = :emailId")
    void markAsDeleted(String emailId, long timestamp);

    @Query("UPDATE emails SET isDeleted = 0, lastModified = :timestamp, needsSync = 1 WHERE id = :emailId")
    void restoreFromTrash(String emailId, long timestamp);

    @Query("UPDATE emails SET syncStatus = :status WHERE id = :emailId")
    void updateSyncStatus(String emailId, String status);

    @Query("UPDATE emails SET needsSync = 0, syncStatus = 'synced' WHERE id = :emailId")
    void markAsSynced(String emailId);

    // ===== מחיקה =====

    @Delete
    void deleteEmail(EmailEntity email);

    @Query("DELETE FROM emails WHERE id = :emailId")
    void deleteEmailById(String emailId);

    @Query("DELETE FROM emails WHERE isDeleted = 1 AND lastModified < :olderThan")
    void permanentlyDeleteOldTrashEmails(long olderThan);

    @Query("DELETE FROM emails")
    void deleteAllEmails();

    @Query("DELETE FROM emails WHERE isDraft = 1 AND id = :draftId")
    void deleteDraft(String draftId);

    // ===== פעולות תחזוקה =====

    @Query("SELECT COUNT(*) FROM emails")
    int getTotalEmailCount();

    @Query("SELECT COUNT(*) FROM emails WHERE isDeleted = 0")
    int getActiveEmailCount();

    @Query("DELETE FROM emails WHERE localTimestamp < :cutoffTime AND syncStatus = 'synced'")
    void cleanupOldSyncedEmails(long cutoffTime);

    // ===== טיוטות =====

    @Query("SELECT * FROM emails WHERE isDraft = 1 AND draftId = :draftId")
    EmailEntity getDraftById(String draftId);

    @Query("UPDATE emails SET isDraft = 0, draftId = NULL WHERE id = :emailId")
    void convertDraftToSent(String emailId);
}