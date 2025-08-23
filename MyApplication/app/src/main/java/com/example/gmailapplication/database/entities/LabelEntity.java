package com.example.gmailapplication.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "labels")
public class LabelEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String name;
    public String userId;  // למי שייכת התווית
    public String color;   // צבע התווית (optional)
    public boolean isSystemLabel; // תווית מערכת (inbox, spam, etc.) או מותאמת אישית
    public boolean isDefault; // תוויות ברירת מחדל
    public long createdAt;
    public long lastModified;

    // מטא-דאטא לsync
    public boolean needsSync = false;
    public String syncStatus = "synced"; // "synced", "pending", "failed"

    public LabelEntity() {
        this.createdAt = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
    }

    public LabelEntity(String id, String name) {
        this();
        this.id = id;
        this.name = name;
    }

    public LabelEntity(String id, String name, boolean isSystemLabel) {
        this(id, name);
        this.isSystemLabel = isSystemLabel;
    }

    // Helper methods
    public void updateName(String newName) {
        this.name = newName;
        this.lastModified = System.currentTimeMillis();
        this.needsSync = true;
    }

    public void updateColor(String newColor) {
        this.color = newColor;
        this.lastModified = System.currentTimeMillis();
        this.needsSync = true;
    }

    // בדיקות מהירות
    public boolean isInbox() {
        return "inbox".equalsIgnoreCase(name);
    }

    public boolean isSpam() {
        return "spam".equalsIgnoreCase(name);
    }

    public boolean isSent() {
        return "sent".equalsIgnoreCase(name);
    }

    public boolean isTrash() {
        return "trash".equalsIgnoreCase(name) || "deleted".equalsIgnoreCase(name);
    }
}