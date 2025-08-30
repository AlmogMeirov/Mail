package com.example.gmailapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class EmailDetailActivity extends AppCompatActivity {

    private TextView tvSender, tvSubject, tvContent, tvTimestamp;
    private Toolbar toolbar;

    private String emailId;
    private String sender;
    private String subject;
    private String content;
    private String timestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_detail);

        System.out.println("=== EMAIL DETAIL ACTIVITY STARTED ===");

        initViews();
        setupToolbar();
        loadEmailData();
        displayEmailData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvSender = findViewById(R.id.tvSender);
        tvSubject = findViewById(R.id.tvSubject);
        tvContent = findViewById(R.id.tvContent);
        tvTimestamp = findViewById(R.id.tvTimestamp);

        Button btnReply = findViewById(R.id.btnReply);
        btnReply.setOnClickListener(v -> openReply());
    }

    private void openReply() {
        Intent intent = new Intent(this, ComposeActivity.class);
        intent.putExtra("reply_to", sender);
        intent.putExtra("reply_subject", "Re: " + subject);
        intent.putExtra("reply_content", "\n\n--- תגובה למייל מקורי ---\n" + content);
        startActivity(intent);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("פרטי המייל");
        }
    }

    private void loadEmailData() {
        // קבל את הנתונים מה-Intent
        emailId = getIntent().getStringExtra("email_id");
        sender = getIntent().getStringExtra("sender");
        subject = getIntent().getStringExtra("subject");
        content = getIntent().getStringExtra("content");
        timestamp = getIntent().getStringExtra("timestamp");

        System.out.println("=== LOADED EMAIL DATA ===");
        System.out.println("ID: " + emailId);
        System.out.println("Sender: " + sender);
        System.out.println("Subject: " + subject);
        System.out.println("Content length: " + (content != null ? content.length() : 0));
        System.out.println("========================");
    }

    private void displayEmailData() {
        // הצג את הנתונים בUI
        tvSender.setText("מאת: " + (sender != null ? sender : "לא ידוע"));
        tvSubject.setText(subject != null ? subject : "(ללא נושא)");
        tvContent.setText(content != null ? content : "(ללא תוכן)");

        // פורמט זמן פשוט (לעכשיו)
        if (timestamp != null && timestamp.length() > 10) {
            String timeOnly = timestamp.substring(11, 16); // HH:MM
            String dateOnly = timestamp.substring(0, 10);  // YYYY-MM-DD
            tvTimestamp.setText(dateOnly + " " + timeOnly);
        } else {
            tvTimestamp.setText("זמן לא ידוע");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        // חזרה למסך הקודם כשלוחצים על החץ
        onBackPressed();
        return true;
    }
}