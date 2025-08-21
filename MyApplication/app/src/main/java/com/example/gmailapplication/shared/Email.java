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
    public List<String> labels;
    public String groupId;
    public String direction; // "sent" or "received"
    public String preview;

    // For recent_mails response
    public UserInfo otherParty;

    public Email() {}

    public boolean isSpam() {
        return labels != null && labels.stream()
                .anyMatch(label -> label.toLowerCase().contains("spam"));
    }

    public boolean isInbox() {
        return labels != null && labels.stream()
                .anyMatch(label -> label.toLowerCase().contains("inbox"));
    }
}