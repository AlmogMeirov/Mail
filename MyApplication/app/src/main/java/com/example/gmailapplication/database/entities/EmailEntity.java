package com.example.gmailapplication.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.annotation.NonNull;

import com.example.gmailapplication.database.converters.StringListConverter;
import com.example.gmailapplication.database.converters.LabelListConverter;
import com.example.gmailapplication.shared.Label;

import java.util.List;

@Entity(tableName = "emails")
@TypeConverters({StringListConverter.class, LabelListConverter.class})
public class EmailEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String sender;
    public String recipient;

    @TypeConverters(StringListConverter.class)
    public List<String> recipients;

    public String subject;
    public String content;
    public String timestamp;

    @TypeConverters(LabelListConverter.class)
    public List<Label> labels;

    public String groupId;
    public String direction; // "sent" or "received"
    public String preview;

    // מטא-דאטה עבור האפליקציה
    public boolean isRead = false;
    public boolean isStarred = false;
    public boolean isArchived = false;
    public boolean isDeleted = false;
    public long localTimestamp; // זמן הוספה למאגר המקומי
    public long lastModified; // זמן עדכון אחרון

    // שדות עבור sync עם השרת
    public boolean needsSync = false; // האם צריך סנכרון עם השרת
    public String syncStatus = "synced"; // "synced", "pending", "failed"

    // שדות עבור draft emails
    public boolean isDraft = false;
    public String draftId;

    // שדה עבור otherParty (JSON string)
    public String otherPartyJson;

    public EmailEntity() {
        this.localTimestamp = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
    }

    public EmailEntity(String id) {
        this.id = id;
        this.localTimestamp = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
    }

    // Helper methods
    public void markAsRead() {
        this.isRead = true;
        this.lastModified = System.currentTimeMillis();
        this.needsSync = true;
    }

    public void markAsUnread() {
        this.isRead = false;
        this.lastModified = System.currentTimeMillis();
        this.needsSync = true;
    }

    public void toggleStar() {
        this.isStarred = !this.isStarred;
        this.lastModified = System.currentTimeMillis();
        this.needsSync = true;
    }

    public void archive() {
        this.isArchived = true;
        this.lastModified = System.currentTimeMillis();
        this.needsSync = true;
    }

    public void unarchive() {
        this.isArchived = false;
        this.lastModified = System.currentTimeMillis();
        this.needsSync = true;
    }

    public void delete() {
        this.isDeleted = true;
        this.lastModified = System.currentTimeMillis();
        this.needsSync = true;
    }

    public void restore() {
        this.isDeleted = false;
        this.lastModified = System.currentTimeMillis();
        this.needsSync = true;
    }

    // בדיקות מהירות
    public boolean isSpam() {
        return labels != null && labels.stream()
                .anyMatch(label -> label.name != null && label.name.toLowerCase().contains("spam"));
    }

    public boolean isInbox() {
        return labels != null && labels.stream()
                .anyMatch(label -> label.name != null && label.name.toLowerCase().contains("inbox"));
    }

    public boolean isSent() {
        return "sent".equals(direction) ||
                (labels != null && labels.stream()
                        .anyMatch(label -> label.name != null && label.name.toLowerCase().contains("sent")));
    }
}