package com.example.gmailapplication.shared;

import java.util.List;

public class SendEmailRequest {
    public String sender;
    public String recipient; // Single recipient (for curl compatibility)
    public List<String> recipients; // Multiple recipients (for full functionality)
    public String subject;
    public String content;
    public List<String> labels;

    public SendEmailRequest() {
        this.labels = java.util.Arrays.asList("inbox");
    }
}