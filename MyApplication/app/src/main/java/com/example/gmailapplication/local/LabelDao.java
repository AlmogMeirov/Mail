package com.example.gmailapplication.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.gmailapplication.database.entities.LabelEntity;

import java.util.List;

@Dao
public interface LabelDao {

    // קבלת כל התוויות
    @Query("SELECT * FROM labels ORDER BY isSystemLabel DESC, name ASC")
    LiveData<List<LabelEntity>> getAllLabels();

    // קבלת תוויות המערכת בלבד
    @Query("SELECT * FROM labels WHERE isSystemLabel = 1 ORDER BY name ASC")
    LiveData<List<LabelEntity>> getSystemLabels();

    // קבלת תוויות מותאמות אישית בלבד
    @Query("SELECT * FROM labels WHERE isSystemLabel = 0 ORDER BY name ASC")
    LiveData<List<LabelEntity>> getCustomLabels();

    // קבלת תווית לפי ID
    @Query("SELECT * FROM labels WHERE id = :labelId")
    LabelEntity getLabelById(String labelId);

    @Query("SELECT * FROM labels WHERE id = :labelId")
    LiveData<LabelEntity> getLabelByIdLive(String labelId);

    // קבלת תווית לפי שם
    @Query("SELECT * FROM labels WHERE name = :labelName LIMIT 1")
    LabelEntity getLabelByName(String labelName);

    // חיפוש תוויות
    @Query("SELECT * FROM labels WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    LiveData<List<LabelEntity>> searchLabels(String query);

    // קבלת תוויות שצריכות sync
    @Query("SELECT * FROM labels WHERE needsSync = 1")
    List<LabelEntity> getLabelsNeedingSync();

    // הוספת תווית
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLabel(LabelEntity label);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLabels(List<LabelEntity> labels);

    // עדכון תווית
    @Update
    void updateLabel(LabelEntity label);

    // מחיקת תווית
    @Delete
    void deleteLabel(LabelEntity label);

    @Query("DELETE FROM labels WHERE id = :labelId")
    void deleteLabelById(String labelId);

    // מחיקת תוויות מותאמות אישית (לא מערכת)
    @Query("DELETE FROM labels WHERE isSystemLabel = 0")
    void deleteAllCustomLabels();

    // מחיקת כל התוויות
    @Query("DELETE FROM labels")
    void deleteAllLabels();

    // עדכון סטטוס sync
    @Query("UPDATE labels SET needsSync = :needsSync, syncStatus = :syncStatus WHERE id = :labelId")
    void updateSyncStatus(String labelId, boolean needsSync, String syncStatus);

    // ספירת תוויות
    @Query("SELECT COUNT(*) FROM labels")
    int getLabelsCount();

    @Query("SELECT COUNT(*) FROM labels WHERE isSystemLabel = 0")
    int getCustomLabelsCount();

    // בדיקה אם תווית קיימת
    @Query("SELECT COUNT(*) FROM labels WHERE name = :labelName")
    int isLabelExists(String labelName);

    // איפוס סטטוס sync
    @Query("UPDATE labels SET needsSync = 0, syncStatus = 'synced'")
    void clearAllSyncStatus();
}