package com.example.gmailapplication.shared;

import com.google.gson.annotations.SerializedName;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import androidx.annotation.NonNull; // השתמש בזה

@Entity(tableName = "labels")
public class Label {
    @PrimaryKey
    @NonNull
    public String id;          // UUID עבור תוויות מותאמות, או שם עבור תוויות מערכת
    public String name;        // שם התווית
    @SerializedName("isSystem")
    public boolean isSystem;   // האם זו תווית מערכת

    public Label() {}

    public Label(String name) {
        this.name = name;
        this.id = name; // עבור תוויות מערכת, ID = name
        this.isSystem = isSystemLabel(name);
    }

    public Label(String id, String name) {
        this.id = id;
        this.name = name;
        this.isSystem = isSystemLabel(name);
    }

    public Label(String id, String name, boolean isSystem) {
        this.id = id;
        this.name = name;
        this.isSystem = isSystem;
    }

    // בדיקה אם זו תווית מערכת לפי השם
    private boolean isSystemLabel(String labelName) {
        if (labelName == null) return false;
        String lower = labelName.toLowerCase();
        return lower.equals("inbox") || lower.equals("sent") || lower.equals("spam") ||
                lower.equals("drafts") || lower.equals("starred") || lower.equals("trash") ||
                lower.equals("important");
    }

    // שיטה לבדיקה אם ניתן למחוק/לערוך
    public boolean canModify() {
        return !isSystem;
    }

    @Override
    public String toString() {
        return name != null ? name : "";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Label label = (Label) obj;
        return (id != null ? id.equals(label.id) : label.id == null);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}