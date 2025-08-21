package com.example.gmailapplication.shared;

import java.util.List;

public class EmailResponse {
    public String message;
    public List<Email> inbox;
    public List<Email> sent;
    public List<Email> recent_mails;

    public EmailResponse() {}
}
