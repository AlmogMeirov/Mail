package com.example.gmailapplication.shared;

import java.util.List;

public class Email {
    public String id;
    public String sender;
    public String recipient;
    public List<String> recipients;
    public String subject;
    public String content;
    public String timestamp;
    public List<String> labels;  // רשימה של strings - תואם למה שהשרת מחזיר
    public String groupId;
    public boolean isDraft = false;

    // אם אתה צריך לבדוק אם יש תווית ספציפית:
    public boolean hasLabel(String labelName) {
        return labels != null && labels.contains(labelName);
    }
}